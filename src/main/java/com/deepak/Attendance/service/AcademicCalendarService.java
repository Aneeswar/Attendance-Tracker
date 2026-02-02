package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.AcademicCalendarDTO;
import com.deepak.Attendance.entity.AcademicCalendar;
import com.deepak.Attendance.repository.AcademicCalendarRepository;
import com.deepak.Attendance.repository.AttendanceReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class AcademicCalendarService {

    @Autowired
    private AcademicCalendarRepository academicCalendarRepository;
    
    @Autowired
    private AttendanceReportRepository attendanceReportRepository;

    public AcademicCalendarDTO saveOrUpdateAcademicCalendar(AcademicCalendarDTO dto) {
        log.info("Saving Academic Calendar: academicYear={}, semesterStart={}, examStart={}", 
                 dto.getAcademicYear(), dto.getSemesterStartDate(), dto.getExamStartDate());
        log.info("Exam Dates - CAT1: {}-{}, CAT2: {}-{}, FAT: {}-{}", 
                 dto.getCat1StartDate(), dto.getCat1EndDate(),
                 dto.getCat2StartDate(), dto.getCat2EndDate(),
                 dto.getFatStartDate(), dto.getFatEndDate());
        
        if (dto.getSemesterStartDate() != null && dto.getExamStartDate() != null 
            && dto.getSemesterStartDate().isAfter(dto.getExamStartDate())) {
            throw new IllegalArgumentException("Semester start date must be before exam start date");
        }

        // Delete all existing records and create a new one (only one active at a time)
        academicCalendarRepository.deleteAll();
        
        // Clear all cached attendance reports when calendar changes
        // This ensures reports will be recalculated with new exam dates
        log.info("Clearing all cached attendance reports due to calendar update");
        attendanceReportRepository.deleteAll();

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
        log.info("Academic Calendar saved with ID: {}", saved.getId());
        return convertToDTO(saved);
    }

    public Optional<AcademicCalendarDTO> getCurrentAcademicCalendar() {
        List<AcademicCalendar> calendars = academicCalendarRepository.findAll();
        if (!calendars.isEmpty()) {
            AcademicCalendarDTO dto = convertToDTO(calendars.get(0));
            log.info("Retrieved Academic Calendar: academicYear={}, cat1: {}-{}, cat2: {}-{}, fat: {}-{}",
                     dto.getAcademicYear(),
                     dto.getCat1StartDate(), dto.getCat1EndDate(),
                     dto.getCat2StartDate(), dto.getCat2EndDate(),
                     dto.getFatStartDate(), dto.getFatEndDate());
            return Optional.of(dto);
        }
        log.info("No academic calendar found");
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
