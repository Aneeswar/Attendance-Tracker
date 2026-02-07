package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.CourseAttendanceCalendarDTO;
import com.deepak.Attendance.dto.DateBasedAttendanceDTO;
import com.deepak.Attendance.entity.*;
import com.deepak.Attendance.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class DateBasedAttendanceService {

    private static final Logger logger = LoggerFactory.getLogger(DateBasedAttendanceService.class);

    @Autowired
    private DateBasedAttendanceRepository dateBasedAttendanceRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private AttendanceInputRepository attendanceInputRepository;

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private StudentService studentService;

    /**
     * Get attendance calendar for a specific course
     * Shows dates from last attendance update to today based on course schedule
     */
    public CourseAttendanceCalendarDTO getCourseAttendanceCalendar(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        // Get course timetable to know which days have classes
        List<TimetableEntry> timetableEntries = timetableEntryRepository.findByCourseId(courseId);
        List<String> classDays = timetableEntries.stream()
                .map(TimetableEntry::getDayOfWeek)
                .distinct()
                .collect(Collectors.toList());

        if (classDays.isEmpty()) {
            throw new RuntimeException("Course has no scheduled class days. Please update timetable first.");
        }

        // Get last attendance update date or use 30 days ago as default
        Optional<AttendanceInput> lastAttendance = attendanceInputRepository.findFirstByCourseIdOrderByLastUpdatedDesc(courseId);
        LocalDate startDate;
        if (lastAttendance.isPresent()) {
            startDate = lastAttendance.get().getLastUpdated().toLocalDate();
        } else {
            startDate = LocalDate.now().minusDays(30);
        }

        LocalDate endDate = LocalDate.now();

        // Get all dates between startDate and endDate that match class schedule
        List<LocalDate> classDatesList = getClassDatesInRange(startDate, endDate, classDays);

        // Get attendance records for these dates
        List<DateBasedAttendance> attendanceRecords = dateBasedAttendanceRepository
                .findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(courseId, startDate, endDate);

        // Create DTOs for all class dates
        Map<LocalDate, DateBasedAttendance> attendanceMap = attendanceRecords.stream()
                .collect(Collectors.toMap(DateBasedAttendance::getAttendanceDate, a -> a));

        List<DateBasedAttendanceDTO> attendanceDtos = classDatesList.stream()
                .map(date -> {
                    DateBasedAttendanceDTO dto = new DateBasedAttendanceDTO();
                    dto.setDate(date);
                    dto.setDayOfWeek(date.getDayOfWeek().toString());
                    
                    DateBasedAttendance record = attendanceMap.get(date);
                    dto.setAttended(record != null ? record.getAttended() : null);
                    
                    return dto;
                })
                .collect(Collectors.toList());

        // Build response DTO
        CourseAttendanceCalendarDTO response = new CourseAttendanceCalendarDTO();
        response.setCourseId(courseId);
        response.setCourseCode(course.getCourseCode());
        response.setCourseName(course.getCourseName());
        response.setStartDate(startDate);
        response.setEndDate(endDate);
        response.setClassScheduleDays(classDays);
        response.setAttendanceDates(attendanceDtos);
        response.setTotalDaysAvailable(classDatesList.size());
        response.setTotalDaysAttended((int) attendanceDtos.stream()
                .filter(d -> d.getAttended() != null && d.getAttended())
                .count());

        logger.info("Retrieved attendance calendar for course {} with {} class dates", courseId, classDatesList.size());
        return response;
    }

    /**
     * Update attendance for specific dates
     */
    public void updateDateBasedAttendance(Long courseId, List<DateBasedAttendanceDTO> attendanceDates) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        for (DateBasedAttendanceDTO dto : attendanceDates) {
            if (dto.getAttended() != null) {
                Optional<DateBasedAttendance> existing = dateBasedAttendanceRepository
                        .findByCourseIdAndAttendanceDate(courseId, dto.getDate());

                if (existing.isPresent()) {
                    existing.get().setAttended(dto.getAttended());
                    dateBasedAttendanceRepository.save(existing.get());
                } else {
                    DateBasedAttendance newRecord = new DateBasedAttendance();
                    newRecord.setCourse(course);
                    newRecord.setAttendanceDate(dto.getDate());
                    newRecord.setAttended(dto.getAttended());
                    dateBasedAttendanceRepository.save(newRecord);
                }
            }
        }

        // Update the AttendanceInput with aggregated values
        updateAggregatedAttendance(courseId);
        
        // Recalculate attendance report for this course since attendance was updated
        logger.info("Date-based attendance updated. Recalculating report for course {}", courseId);
        studentService.recalculateAttendanceReportForCourse(courseId);
    }

    /**
     * Update aggregated attendance numbers based on date records
     */
    private void updateAggregatedAttendance(Long courseId) {
        Optional<AttendanceInput> attendanceInput = attendanceInputRepository.findFirstByCourseIdOrderByLastUpdatedDesc(courseId);
        
        if (attendanceInput.isPresent()) {
            AttendanceInput input = attendanceInput.get();
            
            LocalDate endDate = LocalDate.now();
            LocalDate startDate = input.getLastUpdated().toLocalDate();
            
            Integer attended = dateBasedAttendanceRepository.countAttendedDays(courseId, startDate, endDate);
            Integer total = dateBasedAttendanceRepository.countTotalDays(courseId, startDate, endDate);
            
            input.setClassesAttended(input.getClassesAttended() + (attended != null ? attended : 0));
            input.setTotalClassesConducted(input.getTotalClassesConducted() + (total != null ? total : 0));
            attendanceInputRepository.save(input);
            
            logger.info("Updated aggregated attendance for course {}: {} / {}", 
                    courseId, input.getClassesAttended(), input.getTotalClassesConducted());
        }
    }

    /**
     * Generate list of dates between start and end date that match given days of week
     */
    private List<LocalDate> getClassDatesInRange(LocalDate startDate, LocalDate endDate, List<String> classDays) {
        List<LocalDate> dates = new ArrayList<>();
        Set<DayOfWeek> classedaySet = classDays.stream()
                .map(day -> DayOfWeek.valueOf(day.toUpperCase()))
                .collect(Collectors.toSet());

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (classedaySet.contains(current.getDayOfWeek())) {
                dates.add(current);
            }
            current = current.plusDays(1);
        }

        return dates;
    }
}
