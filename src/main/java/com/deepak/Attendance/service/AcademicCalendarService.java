package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.AcademicCalendarDTO;
import com.deepak.Attendance.entity.AcademicCalendar;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AcademicCalendarService {

    @Autowired
    private AcademicCalendarRepository academicCalendarRepository;

    public AcademicCalendarDTO saveOrUpdateAcademicCalendar(AcademicCalendarDTO dto) {
        if (dto.getSemesterStartDate() != null && dto.getExamStartDate() != null 
            && dto.getSemesterStartDate().isAfter(dto.getExamStartDate())) {
            throw new IllegalArgumentException("Semester start date must be before end date");
        }

        // Delete all existing records and create a new one (only one active at a time)
        academicCalendarRepository.deleteAll();

        AcademicCalendar calendar = new AcademicCalendar();
        calendar.setAcademicYear(dto.getAcademicYear());
        calendar.setSemesterStartDate(dto.getSemesterStartDate());
        calendar.setExamStartDate(dto.getExamStartDate());
        
        // Set exam date ranges
        calendar.setCat1StartDate(dto.getCat1StartDate());
        calendar.setCat1EndDate(dto.getCat1EndDate());
        calendar.setCat2StartDate(dto.getCat2StartDate());
        calendar.setCat2EndDate(dto.getCat2EndDate());
        calendar.setFatStartDate(dto.getFatStartDate());
        calendar.setFatEndDate(dto.getFatEndDate());

        AcademicCalendar saved = academicCalendarRepository.save(calendar);
        return convertToDTO(saved);
    }

    public Optional<AcademicCalendarDTO> getCurrentAcademicCalendar() {
        List<AcademicCalendar> calendars = academicCalendarRepository.findAll();
        if (!calendars.isEmpty()) {
            return Optional.of(convertToDTO(calendars.get(0)));
        }
        return Optional.empty();
    }

    private AcademicCalendarDTO convertToDTO(AcademicCalendar calendar) {
        AcademicCalendarDTO dto = new AcademicCalendarDTO();
        dto.setId(calendar.getId());
        dto.setAcademicYear(calendar.getAcademicYear());
        dto.setSemesterStartDate(calendar.getSemesterStartDate());
        dto.setExamStartDate(calendar.getExamStartDate());
        
        // Set exam date ranges
        dto.setCat1StartDate(calendar.getCat1StartDate());
        dto.setCat1EndDate(calendar.getCat1EndDate());
        dto.setCat2StartDate(calendar.getCat2StartDate());
        dto.setCat2EndDate(calendar.getCat2EndDate());
        dto.setFatStartDate(calendar.getFatStartDate());
        dto.setFatEndDate(calendar.getFatEndDate());
        
        dto.setCreatedAt(calendar.getCreatedAt());
        dto.setUpdatedAt(calendar.getUpdatedAt());
        return dto;
    }
}
