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
    private DateBasedAttendanceRepository dateBasedAttendanceRepository;

    @Autowired
    private AttendanceResultRepository attendanceResultRepository;

    public static final double TARGET_PERCENTAGE = 0.75;

    /**
     * Calculate future valid working days from a starting date
     * Only considers: Tuesday to Saturday, excludes holidays, excludes exam periods
     */
    public List<LocalDate> getValidWorkingDays(LocalDate startDate) {
        // Get current academic calendar
        Optional<AcademicCalendar> currentCalendar = academicCalendarRepository
                .findBySemesterStartDateLessThanEqualAndExamStartDateGreaterThanEqual(startDate, startDate);

        if (currentCalendar.isEmpty()) {
            return new ArrayList<>();
        }

        AcademicCalendar calendar = currentCalendar.get();
        LocalDate examStart = calendar.getExamStartDate();

        List<LocalDate> validDays = new ArrayList<>();
        LocalDate current = startDate;

        // Get all holidays
        Set<LocalDate> holidays = new HashSet<>(
                holidayRepository.findByAcademicCalendarId(calendar.getId()).stream()
                        .map(Holiday::getDate)
                        .toList()
        );

        // Exclude all exam periods (CAT-1, CAT-2, FAT)
        Set<LocalDate> examDates = new HashSet<>();
        if (calendar.getCat1StartDate() != null && calendar.getCat1EndDate() != null) {
            LocalDate d = calendar.getCat1StartDate();
            while (!d.isAfter(calendar.getCat1EndDate())) {
                examDates.add(d);
                d = d.plusDays(1);
            }
        }
        if (calendar.getCat2StartDate() != null && calendar.getCat2EndDate() != null) {
            LocalDate d = calendar.getCat2StartDate();
            while (!d.isAfter(calendar.getCat2EndDate())) {
                examDates.add(d);
                d = d.plusDays(1);
            }
        }
        if (calendar.getFatStartDate() != null && calendar.getFatEndDate() != null) {
            LocalDate d = calendar.getFatStartDate();
            while (!d.isAfter(calendar.getFatEndDate())) {
                examDates.add(d);
                d = d.plusDays(1);
            }
        }

        // Exclude study holidays (one working day before CAT-1 and CAT-2)
        if (calendar.getCat1StartDate() != null) {
            LocalDate studyHoliday = getLastWorkingDayBeforeDate(calendar.getCat1StartDate(), holidays);
            if (studyHoliday != null) {
                examDates.add(studyHoliday);
            }
        }
        if (calendar.getCat2StartDate() != null) {
            LocalDate studyHoliday = getLastWorkingDayBeforeDate(calendar.getCat2StartDate(), holidays);
            if (studyHoliday != null) {
                examDates.add(studyHoliday);
            }
        }

        // Loop until exam start
        while (!current.isAfter(examStart.minusDays(1))) {
            // Only Tuesday (2) to Saturday (6)
            if (current.getDayOfWeek().getValue() >= 2 && current.getDayOfWeek().getValue() <= 6) {
                // Not a holiday and not an exam date
                if (!holidays.contains(current) && !examDates.contains(current)) {
                    validDays.add(current);
                }
            }
            current = current.plusDays(1);
        }

        return validDays;
    }

    /**
     * Legacy method for backward compatibility
     */
    public List<LocalDate> getValidWorkingDays() {
        return getValidWorkingDays(LocalDate.now());
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
     * Find the last working day (Tuesday-Saturday) before a given date
     * Only excludes holidays, not exam dates
     * Used for determining the cutoff day for exam class counts
     */
    private LocalDate getLastWorkingDayBeforeDate(LocalDate date, Set<LocalDate> holidays) {
        LocalDate current = date.minusDays(1);
        while (current.isAfter(LocalDate.now().minusDays(1))) {
            int dayOfWeek = current.getDayOfWeek().getValue();
            // Tuesday (2) to Saturday (6)
            if (dayOfWeek >= 2 && dayOfWeek <= 6 && !holidays.contains(current)) {
                return current;
            }
            current = current.minusDays(1);
        }
        return null;
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
        Set<LocalDate> holidays = new HashSet<>();
        
        if (currentCalendar.isPresent()) {
            AcademicCalendar calendar = currentCalendar.get();
            
            // Add holidays to both sets
            holidays.addAll(
                    holidayRepository.findByAcademicCalendarId(calendar.getId()).stream()
                            .map(Holiday::getDate)
                            .toList()
            );
            excludedDates.addAll(holidays);
            
            log.info("Academic Calendar found: {} holidays", holidays.size());
            
            // For FAT, exclude all holidays and all exam period dates (CAT-1, CAT-2, FAT)
            if ("FAT".equals(examType)) {
                // Exclude all exam periods for FAT
                if (calendar.getCat1StartDate() != null && calendar.getCat1EndDate() != null) {
                    LocalDate examDate = calendar.getCat1StartDate();
                    while (!examDate.isAfter(calendar.getCat1EndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }
                if (calendar.getCat2StartDate() != null && calendar.getCat2EndDate() != null) {
                    LocalDate examDate = calendar.getCat2StartDate();
                    while (!examDate.isAfter(calendar.getCat2EndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }
                if (calendar.getFatStartDate() != null && calendar.getFatEndDate() != null) {
                    LocalDate examDate = calendar.getFatStartDate();
                    while (!examDate.isAfter(calendar.getFatEndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }

                // Also exclude study holidays (one working day before CAT-1/CAT-2)
                if (calendar.getCat1StartDate() != null) {
                    LocalDate studyHoliday = getLastWorkingDayBeforeDate(calendar.getCat1StartDate(), holidays);
                    if (studyHoliday != null) {
                        excludedDates.add(studyHoliday);
                    }
                }
                if (calendar.getCat2StartDate() != null) {
                    LocalDate studyHoliday = getLastWorkingDayBeforeDate(calendar.getCat2StartDate(), holidays);
                    if (studyHoliday != null) {
                        excludedDates.add(studyHoliday);
                    }
                }
            } else {
                // For CAT-1 and CAT-2, exclude all exam periods
                if (calendar.getCat1StartDate() != null && calendar.getCat1EndDate() != null) {
                    LocalDate examDate = calendar.getCat1StartDate();
                    while (!examDate.isAfter(calendar.getCat1EndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }
                if (calendar.getCat2StartDate() != null && calendar.getCat2EndDate() != null) {
                    LocalDate examDate = calendar.getCat2StartDate();
                    while (!examDate.isAfter(calendar.getCat2EndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }
                if (calendar.getFatStartDate() != null && calendar.getFatEndDate() != null) {
                    LocalDate examDate = calendar.getFatStartDate();
                    while (!examDate.isAfter(calendar.getFatEndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }

                // Also exclude study holidays
                if (calendar.getCat1StartDate() != null) {
                    LocalDate studyHoliday = getLastWorkingDayBeforeDate(calendar.getCat1StartDate(), holidays);
                    if (studyHoliday != null) {
                        excludedDates.add(studyHoliday);
                    }
                }
                if (calendar.getCat2StartDate() != null) {
                    LocalDate studyHoliday = getLastWorkingDayBeforeDate(calendar.getCat2StartDate(), holidays);
                    if (studyHoliday != null) {
                        excludedDates.add(studyHoliday);
                    }
                }
            }
        }

        // Get already marked dates for this course to avoid double-counting
        Set<LocalDate> markedDates = new HashSet<>(
            dateBasedAttendanceRepository.findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                course.getId(), today.minusMonths(6), today
            ).stream().map(DateBasedAttendance::getAttendanceDate).toList()
        );

        // Determine last class day based on exam type
        LocalDate lastWorkingDayBeforeExam = getLastWorkingDayBeforeDate(untilDate, holidays);
        
        LocalDate lastClassDay;
        if ("FAT".equals(examType)) {
            lastClassDay = untilDate.minusDays(1);
        } else {
            if (lastWorkingDayBeforeExam != null) {
                lastClassDay = lastWorkingDayBeforeExam.minusDays(1);
            } else {
                lastClassDay = untilDate.minusDays(2);
            }
        }

        // Iterate from today to last class day
        LocalDate current = today;
        
        while (!current.isAfter(lastClassDay)) {
            // Only Tue-Sat and not already marked and not excluded
            if (current.getDayOfWeek().getValue() >= 2 && current.getDayOfWeek().getValue() <= 6
                    && !markedDates.contains(current) && !excludedDates.contains(current)) {
                
                String dayName = getDayNameFromJavaTime(current.getDayOfWeek().getValue());
                List<TimetableEntry> entries = timetableEntryRepository
                        .findByCourseIdAndDayOfWeek(course.getId(), dayName);

                for (TimetableEntry entry : entries) {
                    totalFutureClasses += entry.getClassesCount();
                }
            }
            current = current.plusDays(1);
        }
        
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
        LocalDate today = LocalDate.now();
        List<LocalDate> validDays = getValidWorkingDays(today);
        
        // Get already marked dates for this course to avoid double-counting
        Set<LocalDate> markedDates = new HashSet<>(
            dateBasedAttendanceRepository.findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                course.getId(), today.minusMonths(6), today
            ).stream().map(DateBasedAttendance::getAttendanceDate).toList()
        );

        int totalFutureClasses = 0;

        for (LocalDate day : validDays) {
            // Skip if already marked to avoid double-counting today or past days in validDays list
            if (markedDates.contains(day)) {
                continue;
            }

            String dayName = getDayNameFromJavaTime(day.getDayOfWeek().getValue());
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
