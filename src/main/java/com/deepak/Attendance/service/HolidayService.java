package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.HolidayDTO;
import com.deepak.Attendance.dto.BulkHolidayRequest;
import com.deepak.Attendance.dto.DateRangeHolidayRequest;
import com.deepak.Attendance.entity.Holiday;
import com.deepak.Attendance.entity.Semester;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import com.deepak.Attendance.repository.HolidayRepository;
import com.deepak.Attendance.repository.SemesterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HolidayService {

    @Autowired
    private HolidayRepository holidayRepository;
    
    @Autowired
    private AcademicCalendarRepository academicCalendarRepository;

    @Autowired
    private SemesterRepository semesterRepository;
    
    @Autowired
    private ObjectProvider<StudentService> studentServiceProvider;

    public HolidayDTO addHoliday(HolidayDTO dto) {
        List<Semester> targetSemesters = resolveTargetSemesters(dto.getSemesterId(), dto.getTargetSemesterIds());
        Holiday saved = null;

        for (Semester semester : targetSemesters) {
            if (holidayRepository.existsByDateAndSemesterId(dto.getDate(), semester.getId())) {
                continue;
            }

            Holiday holiday = new Holiday();
            holiday.setDate(dto.getDate());
            holiday.setReason(dto.getReason());
            holiday.setType(Holiday.HolidayType.valueOf(dto.getType() != null ? dto.getType() : "PUBLIC"));
            holiday.setScope(dto.getScope() != null
                    ? Holiday.HolidayScope.valueOf(dto.getScope())
                    : Holiday.HolidayScope.FULL);
            holiday.setAcademicCalendarId(semester.getId());
            holiday.setSemester(semester);
            log.debug("Setting academicCalendarId {} for holiday on {}", semester.getId(), dto.getDate());

            Holiday inserted = saveHolidaySafely(holiday);
            if (saved == null) {
                saved = inserted;
            }
        }

        if (saved == null) {
            throw new IllegalArgumentException("Holiday already added");
        }
        
        // Mark all attendance reports as stale when holiday is added
        log.info("Holiday added on {}. Marking all attendance reports as stale.", dto.getDate());
        studentServiceProvider.ifAvailable(studentService -> {
            try {
                studentService.markAllAttendanceReportsAsStale();
            } catch (Exception e) {
                log.error("Error marking reports as stale after holiday addition", e);
            }
        });
        
        return convertToDTO(saved);
    }

    public List<HolidayDTO> addHolidayRange(DateRangeHolidayRequest request) {
        List<Semester> targetSemesters = resolveTargetSemesters(request.getSemesterId(), request.getTargetSemesterIds());
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = LocalDate.parse(request.getEndDate());
        
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        List<HolidayDTO> addedHolidays = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            for (Semester semester : targetSemesters) {
                boolean missingForSemester = holidayRepository.findByDateAndSemesterId(currentDate, semester.getId()).isEmpty();

                if (missingForSemester) {
                    Holiday holiday = new Holiday();
                    holiday.setDate(currentDate);
                    holiday.setReason(request.getName());
                    holiday.setType(Holiday.HolidayType.valueOf(request.getType() != null ? request.getType() : "PUBLIC"));
                    holiday.setScope(request.getScope() != null
                            ? Holiday.HolidayScope.valueOf(request.getScope())
                            : Holiday.HolidayScope.FULL);

                    holiday.setAcademicCalendarId(semester.getId());
                    holiday.setSemester(semester);
                    log.debug("Setting academicCalendarId {} for holiday on {}", semester.getId(), currentDate);

                    addedHolidays.add(convertToDTO(saveHolidaySafely(holiday)));
                }
            }
            currentDate = currentDate.plusDays(1);
        }

        if (addedHolidays.isEmpty()) {
            throw new IllegalArgumentException("Holiday already added");
        }
        
        // Mark all attendance reports as stale when holidays are added in bulk
        if (!addedHolidays.isEmpty()) {
            log.info("Holidays added (date range: {} to {}). Marking all attendance reports as stale.", startDate, endDate);
            studentServiceProvider.ifAvailable(studentService -> {
                try {
                    studentService.markAllAttendanceReportsAsStale();
                } catch (Exception e) {
                    log.error("Error marking reports as stale after bulk holiday addition", e);
                }
            });
        }
        
        return addedHolidays;
    }

    public List<HolidayDTO> bulkAddHolidays(BulkHolidayRequest request) {
        List<Semester> targetSemesters = resolveTargetSemesters(request.getSemesterId(), request.getTargetSemesterIds());
        
        List<HolidayDTO> savedHolidays = new ArrayList<>();
        for (BulkHolidayRequest.Holiday h : request.getHolidays()) {
            LocalDate date = LocalDate.parse(h.getDate());
            for (Semester semester : targetSemesters) {
                boolean missingForSemester = holidayRepository.findByDateAndSemesterId(date, semester.getId()).isEmpty();

                if (missingForSemester) {
                    Holiday holiday = new Holiday();
                    holiday.setDate(date);
                    holiday.setReason(h.getReason());

                    try {
                        if (h.getType() != null && !h.getType().isEmpty()) {
                            holiday.setType(Holiday.HolidayType.valueOf(h.getType().toUpperCase()));
                        } else {
                            holiday.setType(Holiday.HolidayType.CALENDAR);
                        }
                    } catch (Exception e) {
                        holiday.setType(Holiday.HolidayType.CALENDAR);
                    }

                    try {
                        if (h.getScope() != null && !h.getScope().isEmpty()) {
                            holiday.setScope(Holiday.HolidayScope.valueOf(h.getScope().toUpperCase()));
                        } else {
                            holiday.setScope(Holiday.HolidayScope.FULL);
                        }
                    } catch (Exception e) {
                        holiday.setScope(Holiday.HolidayScope.FULL);
                    }

                    holiday.setAcademicCalendarId(semester.getId());
                    holiday.setSemester(semester);
                    log.debug("Setting academicCalendarId {} for holiday on {}", semester.getId(), h.getDate());
                    savedHolidays.add(convertToDTO(saveHolidaySafely(holiday)));
                }
            }
        }

        if (savedHolidays.isEmpty()) {
            throw new IllegalArgumentException("Holiday already added");
        }
        
        // Mark all attendance reports as stale when holidays are added in bulk
        if (!savedHolidays.isEmpty()) {
            log.info("Bulk added {} holidays. Marking all attendance reports as stale.", savedHolidays.size());
            studentServiceProvider.ifAvailable(studentService -> {
                try {
                    studentService.markAllAttendanceReportsAsStale();
                } catch (Exception e) {
                    log.error("Error marking reports as stale after bulk holiday addition", e);
                }
            });
        }
        
        return savedHolidays;
    }

    private Long getCurrentCalendarId() {
        if (semesterRepository == null) {
            return academicCalendarRepository.findFirstByOrderByCreatedAtDesc()
                    .map(c -> c.getId())
                    .orElseThrow(() -> new IllegalArgumentException("No academic calendar found. Please create an academic calendar first."));
        }

        return semesterRepository.findFirstByActiveTrueOrderBySemesterStartDateDesc()
                .map(Semester::getId)
                .orElseThrow(() -> new IllegalArgumentException("No active semester found. Please create a semester first."));
    }

    private Semester resolveSemester(Long semesterId) {
        if (semesterId != null) {
            return semesterRepository.findById(semesterId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid semester selected"));
        }

        Long currentId = getCurrentCalendarId();
        return semesterRepository.findById(currentId)
                .orElseThrow(() -> new IllegalArgumentException("No active semester found. Please create a semester first."));
    }

    private List<Semester> resolveTargetSemesters(Long semesterId, List<Long> targetSemesterIds) {
        List<Long> ids = new ArrayList<>();

        if (targetSemesterIds != null && !targetSemesterIds.isEmpty()) {
            ids = targetSemesterIds.stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
        } else {
            ids.add(resolveSemester(semesterId).getId());
        }

        List<Semester> semesters = semesterRepository.findAllById(ids);
        if (semesters.size() != ids.size()) {
            throw new IllegalArgumentException("Invalid semester selection");
        }
        return semesters;
    }

    public List<HolidayDTO> getAllHolidays() {
        return holidayRepository.findAllByOrderByDateAsc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<HolidayDTO> getHolidaysBySemester(Long semesterId) {
        if (semesterId == null) {
            return getAllHolidays();
        }
        return holidayRepository.findBySemesterIdOrderByDateAsc(semesterId)
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
        
        // Mark all attendance reports as stale when holiday is deleted
        log.info("Holiday deleted (date: {}). Triggering immediate recalculation of reports.", holiday.getDate());
        studentServiceProvider.ifAvailable(studentService -> {
            try {
                studentService.recalculateAttendanceReportsForAllStudents();
            } catch (Exception e) {
                log.error("Error recalculating reports after holiday deletion", e);
            }
        });
    }

    public void deleteHolidays(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        
        log.info("Deleting {} holidays. Marking all attendance reports as stale.", ids.size());
        holidayRepository.deleteAllById(ids);
        
        studentServiceProvider.ifAvailable(studentService -> {
            try {
                studentService.markAllAttendanceReportsAsStale();
            } catch (Exception e) {
                log.error("Error marking reports as stale after bulk holiday deletion", e);
            }
        });
    }

    private HolidayDTO convertToDTO(Holiday holiday) {
        HolidayDTO dto = new HolidayDTO();
        dto.setId(holiday.getId());
        dto.setSemesterId(holiday.getSemester() != null ? holiday.getSemester().getId() : holiday.getAcademicCalendarId());
        dto.setDate(holiday.getDate());
        dto.setReason(holiday.getReason());
        dto.setType(holiday.getType().toString());
        dto.setScope(holiday.getScope() != null ? holiday.getScope().toString() : "FULL");
        dto.setCreatedAt(holiday.getCreatedAt());
        return dto;
    }

    private Holiday saveHolidaySafely(Holiday holiday) {
        try {
            return holidayRepository.save(holiday);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Holiday already added");
        }
    }
}

