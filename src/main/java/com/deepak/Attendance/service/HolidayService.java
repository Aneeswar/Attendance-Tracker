package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.HolidayDTO;
import com.deepak.Attendance.dto.BulkHolidayRequest;
import com.deepak.Attendance.dto.DateRangeHolidayRequest;
import com.deepak.Attendance.entity.Holiday;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import com.deepak.Attendance.repository.HolidayRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;
    
    @Autowired
    private AcademicCalendarRepository academicCalendarRepository;
    
    @Autowired
    private ObjectProvider<StudentService> studentServiceProvider;

    public HolidayDTO addHoliday(HolidayDTO dto) {
        if (holidayRepository.findByDate(dto.getDate()).isPresent()) {
            throw new IllegalArgumentException("Holiday already exists for this date");
        }

        Holiday holiday = new Holiday();
        holiday.setDate(dto.getDate());
        holiday.setReason(dto.getReason());
        holiday.setType(Holiday.HolidayType.valueOf(dto.getType()));
        
        // Set the academic calendar ID
        Long calendarId = academicCalendarRepository.findAll().stream()
                .map(c -> c.getId())
                .findFirst()
                .orElse(null);
        holiday.setAcademicCalendarId(calendarId);
        log.debug("Setting academicCalendarId {} for holiday on {}", calendarId, dto.getDate());

        Holiday saved = holidayRepository.save(holiday);
        
        // Recalculate attendance reports for all students when holiday is added
        log.info("Holiday added on {}. Recalculating attendance reports for all students.", dto.getDate());
        studentServiceProvider.ifAvailable(studentService -> {
            try {
                studentService.recalculateAttendanceReportsForAllStudents();
            } catch (Exception e) {
                log.error("Error recalculating reports after holiday addition", e);
            }
        });
        
        return convertToDTO(saved);
    }

    public List<HolidayDTO> addHolidayRange(DateRangeHolidayRequest request) {
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());
        
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        List<HolidayDTO> addedHolidays = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            if (holidayRepository.findByDate(currentDate).isEmpty()) {
                Holiday holiday = new Holiday();
                holiday.setDate(currentDate);
                holiday.setReason(request.getName());
                holiday.setType(Holiday.HolidayType.valueOf(request.getType() != null ? request.getType() : "PUBLIC"));
                
                // Set the academic calendar ID
                Long calendarId = academicCalendarRepository.findAll().stream()
                        .map(c -> c.getId())
                        .findFirst()
                        .orElse(null);
                holiday.setAcademicCalendarId(calendarId);
                log.debug("Setting academicCalendarId {} for holiday on {}", calendarId, currentDate);
                
                addedHolidays.add(convertToDTO(holidayRepository.save(holiday)));
            }
            currentDate = currentDate.plusDays(1);
        }
        
        // Recalculate attendance reports for all students when holidays are added in bulk
        if (!addedHolidays.isEmpty()) {
            log.info("Holidays added (date range: {} to {}). Recalculating attendance reports for all students.", startDate, endDate);
            studentServiceProvider.ifAvailable(studentService -> {
                try {
                    studentService.recalculateAttendanceReportsForAllStudents();
                } catch (Exception e) {
                    log.error("Error recalculating reports after bulk holiday addition", e);
                }
            });
        }
        
        return addedHolidays;
    }

    public List<HolidayDTO> bulkAddHolidays(BulkHolidayRequest request) {
        // Get the current academic calendar ID
        Long calendarId = academicCalendarRepository.findAll().stream()
                .map(c -> c.getId())
                .findFirst()
                .orElse(null);
        
        List<HolidayDTO> savedHolidays = request.getHolidays().stream()
                .map(h -> {
                    if (holidayRepository.findByDate(h.getDate()).isEmpty()) {
                        Holiday holiday = new Holiday();
                        holiday.setDate(h.getDate());
                        holiday.setReason(h.getReason());
                        holiday.setType(Holiday.HolidayType.CALENDAR);
                        holiday.setAcademicCalendarId(calendarId);
                        log.debug("Setting academicCalendarId {} for holiday on {}", calendarId, h.getDate());
                        return convertToDTO(holidayRepository.save(holiday));
                    }
                    return null;
                })
                .filter(h -> h != null)
                .collect(Collectors.toList());
        
        // Recalculate attendance reports for all students when holidays are added in bulk
        if (!savedHolidays.isEmpty()) {
            log.info("Bulk added {} holidays. Recalculating attendance reports for all students.", savedHolidays.size());
            studentServiceProvider.ifAvailable(studentService -> {
                try {
                    studentService.recalculateAttendanceReportsForAllStudents();
                } catch (Exception e) {
                    log.error("Error recalculating reports after bulk holiday addition", e);
                }
            });
        }
        
        return savedHolidays;
    }

    public List<HolidayDTO> getAllHolidays() {
        return holidayRepository.findAllByOrderByDateAsc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public void deleteHoliday(Long id) {
        if (!holidayRepository.existsById(id)) {
            throw new IllegalArgumentException("Holiday not found");
        }
        
        Holiday holiday = holidayRepository.findById(id).get();
        holidayRepository.deleteById(id);
        
        // Recalculate attendance reports for all students when holiday is deleted
        log.info("Holiday deleted (date: {}). Recalculating attendance reports for all students.", holiday.getDate());
        studentServiceProvider.ifAvailable(studentService -> {
            try {
                studentService.recalculateAttendanceReportsForAllStudents();
            } catch (Exception e) {
                log.error("Error recalculating reports after holiday deletion", e);
            }
        });
    }

    private HolidayDTO convertToDTO(Holiday holiday) {
        HolidayDTO dto = new HolidayDTO();
        dto.setId(holiday.getId());
        dto.setDate(holiday.getDate());
        dto.setReason(holiday.getReason());
        dto.setType(holiday.getType().toString());
        dto.setCreatedAt(holiday.getCreatedAt());
        return dto;
    }
}

