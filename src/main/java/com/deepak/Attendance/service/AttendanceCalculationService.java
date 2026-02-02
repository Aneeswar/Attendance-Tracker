package com.deepak.Attendance.service;

import com.deepak.Attendance.entity.*;
import com.deepak.Attendance.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class AttendanceCalculationService {

    @Autowired
    private AcademicCalendarRepository academicCalendarRepository;

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private AttendanceResultRepository attendanceResultRepository;

    public static final double TARGET_PERCENTAGE = 0.75;

    /**
     * Calculate future valid working days between today and exam start date
     * Only considers: Tuesday to Saturday, excludes holidays, excludes last working day before exam
     */
    public List<LocalDate> getValidWorkingDays() {
        LocalDate today = LocalDate.now();

        // Get current academic calendar
        Optional<AcademicCalendar> currentCalendar = academicCalendarRepository
                .findBySemesterStartDateLessThanEqualAndExamStartDateGreaterThanEqual(today, today);

        if (currentCalendar.isEmpty()) {
            return new ArrayList<>();
        }

        AcademicCalendar calendar = currentCalendar.get();
        LocalDate examStart = calendar.getExamStartDate();

        List<LocalDate> validDays = new ArrayList<>();
        LocalDate current = today;

        // Get all holidays
        Set<LocalDate> holidays = new HashSet<>(
                holidayRepository.findByAcademicCalendarId(calendar.getId()).stream()
                        .map(Holiday::getDate)
                        .toList()
        );

        // Last working day before exam
        LocalDate lastWorkingDay = getLastWorkingDayBeforeExam(examStart, holidays);

        while (!current.isAfter(examStart.minusDays(1)) && !current.isEqual(lastWorkingDay)) {
            // Only Tuesday (2) to Saturday (6)
            if (current.getDayOfWeek().getValue() >= 2 && current.getDayOfWeek().getValue() <= 6) {
                // Not a holiday
                if (!holidays.contains(current)) {
                    validDays.add(current);
                }
            }
            current = current.plusDays(1);
        }

        return validDays;
    }

    private LocalDate getLastWorkingDayBeforeExam(LocalDate examStart, Set<LocalDate> holidays) {
        LocalDate current = examStart.minusDays(1);
        while (current.isAfter(LocalDate.now())) {
            // Tuesday to Saturday
            if (current.getDayOfWeek().getValue() >= 2 && current.getDayOfWeek().getValue() <= 6) {
                if (!holidays.contains(current)) {
                    return current;
                }
            }
            current = current.minusDays(1);
        }
        return examStart.minusDays(1);
    }

    /**
     * Calculate future classes available for a course until a specific date
     * Only considers: Tuesday to Saturday, excludes holidays
     * The untilDate should be the exam start date - we calculate up to the day BEFORE
     */
    public int calculateFutureClassesAvailableUntilDate(Course course, LocalDate untilDate, String examType) {
        LocalDate today = LocalDate.now();
        int totalFutureClasses = 0;

        log.info("Calculating future classes for course {} until {} (exam: {})", course.getId(), untilDate, examType);

        // Get academic calendar to get holidays and exam dates
        Optional<AcademicCalendar> currentCalendar = academicCalendarRepository
                .findBySemesterStartDateLessThanEqualAndExamStartDateGreaterThanEqual(today, today);

        Set<LocalDate> excludedDates = new HashSet<>();
        if (currentCalendar.isPresent()) {
            AcademicCalendar calendar = currentCalendar.get();
            
            // Add holidays to excluded dates
            excludedDates.addAll(
                    holidayRepository.findByAcademicCalendarId(calendar.getId()).stream()
                            .map(Holiday::getDate)
                            .toList()
            );
            log.info("Academic Calendar found: {} holidays", excludedDates.size());
            for (LocalDate h : excludedDates) {
                log.debug("Holiday: {}", h);
            }
            
            // Add exam period dates to excluded dates (CAT-1, CAT-2, FAT)
            if (calendar.getCat1StartDate() != null && calendar.getCat1EndDate() != null) {
                LocalDate examDate = calendar.getCat1StartDate();
                while (!examDate.isAfter(calendar.getCat1EndDate())) {
                    excludedDates.add(examDate);
                    log.debug("CAT-1 exam date: {}", examDate);
                    examDate = examDate.plusDays(1);
                }
            }
            if (calendar.getCat2StartDate() != null && calendar.getCat2EndDate() != null) {
                LocalDate examDate = calendar.getCat2StartDate();
                while (!examDate.isAfter(calendar.getCat2EndDate())) {
                    excludedDates.add(examDate);
                    log.debug("CAT-2 exam date: {}", examDate);
                    examDate = examDate.plusDays(1);
                }
            }
            if (calendar.getFatStartDate() != null && calendar.getFatEndDate() != null) {
                LocalDate examDate = calendar.getFatStartDate();
                while (!examDate.isAfter(calendar.getFatEndDate())) {
                    excludedDates.add(examDate);
                    log.debug("FAT exam date: {}", examDate);
                    examDate = examDate.plusDays(1);
                }
            }
        } else {
            log.warn("No academic calendar found for today");
        }

        // Determine last class day based on exam type
        LocalDate lastClassDay;
        if ("FAT".equals(examType)) {
            // For FAT, include the last working day of semester (day before exam starts)
            lastClassDay = untilDate.minusDays(1);
            log.info("FAT exam: including last working day before exam start");
        } else {
            // For CAT-1 and CAT-2, exclude the last working day before exam
            lastClassDay = untilDate.minusDays(2);
            log.info("CAT exam: excluding last working day before exam start");
        }
        log.info("Calculating classes from {} to {} (exam type: {})", today, lastClassDay, examType);

        // Iterate from today to last class day
        LocalDate current = today;
        int classesFound = 0;
        int daysChecked = 0;
        
        while (!current.isAfter(lastClassDay)) {
            daysChecked++;
            int dayOfWeek = current.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
            
            // Only Tuesday (2) to Saturday (6)
            if (dayOfWeek >= 2 && dayOfWeek <= 6) {
                // Not a holiday or exam date
                if (!excludedDates.contains(current)) {
                    // Convert day number to day name
                    String dayName = getDayNameFromJavaTime(dayOfWeek);
                    List<TimetableEntry> entries = timetableEntryRepository
                            .findByCourseIdAndDayOfWeek(course.getId(), dayName);

                    if (!entries.isEmpty()) {
                        for (TimetableEntry entry : entries) {
                            totalFutureClasses += entry.getClassesCount();
                            classesFound++;
                        }
                        log.debug("Date {}: {} - Found {} classes", current, dayName, entries.size());
                    } else {
                        log.debug("Date {}: {} - No classes scheduled", current, dayName);
                    }
                } else {
                    log.debug("Date {}: EXCLUDED (holiday or exam period)", current);
                }
            } else {
                log.debug("Date {}: Weekend (day={}) (excluded)", current, dayOfWeek);
            }
            current = current.plusDays(1);
        }
        
        log.info("Total future classes: {} found on {} out of {} days checked", 
                totalFutureClasses, classesFound, daysChecked);
        return totalFutureClasses;
    }

    /**
     * Convert Java time day of week (1-7) to day name
     * 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday
     */
    private String getDayNameFromJavaTime(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "";
        };
    }

    /**
     * Calculate future classes available for a course
     * F = number of times the course appears in remaining valid working days
     */
    public int calculateFutureClassesAvailable(Course course) {
        List<LocalDate> validDays = getValidWorkingDays();
        int totalFutureClasses = 0;

        for (LocalDate day : validDays) {
            String dayName = day.getDayOfWeek().toString();
            List<TimetableEntry> entries = timetableEntryRepository
                    .findByCourseIdAndDayOfWeek(course.getId(), dayName);

            for (TimetableEntry entry : entries) {
                totalFutureClasses += entry.getClassesCount();
            }
        }

        return totalFutureClasses;
    }

    /**
     * Calculate required classes to reach 75% attendance
     * Formula: (A + x) / (T + F) >= 0.75
     * x >= 0.75 * (T + F) - A
     */
    public AttendanceCalculationResult calculateRequiredClasses(
            int totalConducted, int attended, int futureClasses) {

        int totalFuture = totalConducted + futureClasses;
        double requiredAttendance = TARGET_PERCENTAGE * totalFuture;
        double x = requiredAttendance - attended;

        String status;
        int minClassesRequired;

        if (x <= 0) {
            status = "SAFE";
            minClassesRequired = 0;
        } else if (x > futureClasses) {
            status = "IMPOSSIBLE";
            minClassesRequired = futureClasses + 1; // More than available
        } else {
            status = "AT_RISK";
            minClassesRequired = (int) Math.ceil(x);
        }

        double currentPercentage = (double) attended / totalConducted * 100;

        return new AttendanceCalculationResult(
                currentPercentage, futureClasses, minClassesRequired, status
        );
    }

    /**
     * Record attendance calculation result in database
     */
    public AttendanceResult recordAttendanceResult(Course course,
                                                   AttendanceCalculationResult result) {
        AttendanceResult attendanceResult = new AttendanceResult();
        attendanceResult.setCourse(course);
        attendanceResult.setCurrentPercentage(result.currentPercentage());
        attendanceResult.setFutureClassesAvailable(result.futureClasses());
        attendanceResult.setMinClassesRequired(result.minClassesRequired());
        attendanceResult.setEligibilityStatus(result.status());
        attendanceResult.setCalculatedAt(LocalDateTime.now());

        return attendanceResultRepository.save(attendanceResult);
    }

    public record AttendanceCalculationResult(
            double currentPercentage,
            int futureClasses,
            int minClassesRequired,
            String status
    ) {
    }
}
