package com.deepak.Attendance.service;

import com.deepak.Attendance.entity.*;
import com.deepak.Attendance.entity.enums.CourseType;
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
    private SemesterRepository semesterRepository;

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
        // Get current active semester
        List<Semester> calendarList = semesterRepository
            .findBySemesterStartDateLessThanEqualAndSemesterEndDateGreaterThanEqual(startDate, startDate);

        if (calendarList.isEmpty()) {
            return new ArrayList<>();
        }

        Semester calendar = calendarList.get(0);
        LocalDate examStart = calendar.getSemesterEndDate();

        List<LocalDate> validDays = new ArrayList<>();
        LocalDate current = startDate;

        // Use all configured holidays to avoid mismatches with report cutoff logic.
        Set<LocalDate> holidays = new HashSet<>(
            holidayRepository.findBySemesterIdOrderByDateAsc(calendar.getId()).stream()
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

        // Loop until semester end date (exam start date)
        // Only Monday (1) to Saturday (6)
        while (!current.isAfter(examStart)) {
            if (current.getDayOfWeek().getValue() >= 1 && current.getDayOfWeek().getValue() <= 6) {
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

    /**
     * Find the last working day (Tuesday-Saturday) before a given date
     * Only excludes holidays, not exam dates
     * Used for determining the cutoff day for exam class counts
     */
    private LocalDate getLastWorkingDayBeforeDate(LocalDate date, Set<LocalDate> holidays) {
        LocalDate current = date.minusDays(1);
        while (current.isAfter(date.minusDays(15))) { // Sufficient lookback
            int dayOfWeek = current.getDayOfWeek().getValue();
            // Monday (1) to Saturday (6)
            if (dayOfWeek >= 1 && dayOfWeek <= 6 && !holidays.contains(current)) {
                log.debug("Last working day before {} is {}", date, current);
                return current;
            }
            current = current.minusDays(1);
        }
        return null;
    }

    private LocalDate resolveAttendanceWindowEndBeforeExam(LocalDate examDate, Set<LocalDate> fullHolidays) {
        if (examDate == null) {
            return null;
        }

        // Last working day (LWD) is the working day before the exam date.
        LocalDate lwd = getLastWorkingDayBeforeDate(examDate, fullHolidays);
        if (lwd == null) {
            lwd = examDate.minusDays(1);
        }

        // Attendance cutoff is the working day before LWD.
        LocalDate cutoff = getLastWorkingDayBeforeDate(lwd, fullHolidays);
        if (cutoff == null) {
            cutoff = lwd.minusDays(1);
        }
        return cutoff;
    }

    private LocalDate resolveFatAttendanceWindowEnd(LocalDate requestedUntilDate, Set<LocalDate> fullHolidays) {
        if (requestedUntilDate == null) {
            return null;
        }

        LocalDate cutoff = getLastWorkingDayBeforeDate(requestedUntilDate, fullHolidays);
        if (cutoff == null) {
            cutoff = requestedUntilDate.minusDays(1);
        }
        return cutoff;
    }

    private LocalDate resolveLabFatAttendanceWindowEnd(LocalDate labFatStartDate) {
        if (labFatStartDate == null) {
            return null;
        }

        // LAB FAT cutoff is the same weekday from the previous week.
        return labFatStartDate.minusWeeks(1);
    }

    private boolean isLabCourse(Course course) {
        return course != null && course.getCourseType() == CourseType.LAB;
    }

    private LocalDate resolveLabFatAnchorDate(Semester semester) {
        if (semester == null) {
            return null;
        }

        LocalDate labFatStart = semester.getLabFatStartDate();
        LocalDate semesterEnd = semester.getSemesterEndDate();

        if (labFatStart != null && semesterEnd != null) {
            return labFatStart.isBefore(semesterEnd) ? labFatStart : semesterEnd;
        }
        return labFatStart != null ? labFatStart : semesterEnd;
    }

    private LocalDate resolveFatAnchorDateForCourse(Semester semester, Course course) {
        if (isLabCourse(course)) {
            LocalDate labAnchor = resolveLabFatAnchorDate(semester);
            if (labAnchor != null) {
                return labAnchor;
            }
        }

        return resolveFatAnchorDate(semester);
    }

    private LocalDate resolveFatAnchorDate(Semester semester) {
        if (semester == null) {
            return null;
        }

        LocalDate fatStart = semester.getFatStartDate();
        LocalDate semesterEnd = semester.getSemesterEndDate();

        if (fatStart != null && semesterEnd != null) {
            return fatStart.isBefore(semesterEnd) ? fatStart : semesterEnd;
        }
        return fatStart != null ? fatStart : semesterEnd;
    }

    /**
     * Calculate future classes available for a course until a specific date
     * Only considers: Tuesday to Saturday, excludes holidays
     * The untilDate should be the exam start date - we calculate up to the day BEFORE
     */
    public int calculateFutureClassesAvailableUntilDate(Course course, LocalDate untilDate, String examType) {
        // Create a final copy of today's date to work with consistently
        final LocalDate today = LocalDate.now();
        int totalFutureClasses = 0;
        final boolean isLabFatContext = "FAT".equals(examType) && isLabCourse(course);

        log.info("Calculating future classes for course {} until {} (exam: {})", course.getId(), untilDate, examType);

        // Get academic calendar
        Optional<Semester> currentCalendarForCalc = Optional.ofNullable(course.getSemester())
            .or(() -> semesterRepository.findFirstByActiveTrueOrderBySemesterStartDateDesc());

        Set<LocalDate> excludedDates = new HashSet<>();
        List<Holiday> calendarHolidays = new ArrayList<>();
        
        if (currentCalendarForCalc.isPresent()) {
            Semester calendar = currentCalendarForCalc.get();
            
                // Use all holidays for consistent behavior with cutoff date calculation.
                calendarHolidays = holidayRepository.findBySemesterIdOrderByDateAsc(calendar.getId());
            excludedDates.addAll(calendarHolidays.stream()
                    .filter(h -> h.getScope() == Holiday.HolidayScope.FULL)
                    .map(Holiday::getDate)
                    .toList());
            
            Set<LocalDate> fullHolidaysSet = new HashSet<>(excludedDates);

            // For FAT, exclude exam periods but NOT their study holidays (study holidays are working days)
            if ("FAT".equals(examType)) {
                if (calendar.getCat1StartDate() != null && calendar.getCat1EndDate() != null) {
                    // Exclude CAT-1 Exam
                    LocalDate examDate = calendar.getCat1StartDate();
                    while (!examDate.isAfter(calendar.getCat1EndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }
                if (calendar.getCat2StartDate() != null && calendar.getCat2EndDate() != null) {
                    // Exclude CAT-2 Exam
                    LocalDate examDate = calendar.getCat2StartDate();
                    while (!examDate.isAfter(calendar.getCat2EndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }
                
                if (isLabFatContext) {
                    if (calendar.getLabFatStartDate() != null && calendar.getLabFatEndDate() != null) {
                        LocalDate examDate = calendar.getLabFatStartDate();
                        while (!examDate.isAfter(calendar.getLabFatEndDate())) {
                            excludedDates.add(examDate);
                            examDate = examDate.plusDays(1);
                        }
                    }
                } else if (calendar.getFatStartDate() != null && calendar.getFatEndDate() != null) {
                    LocalDate examDate = calendar.getFatStartDate().plusDays(1);
                    while (!examDate.isAfter(calendar.getFatEndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }
            } else {
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

                // If calculating for CAT-2, only the current CAT-2 study holiday is excluded
                // Previous study holidays (like CAT-1) are treated as working days
                if (calendar.getCat2StartDate() != null && "CAT-2".equals(examType)) {
                    LocalDate studyHoliday2 = getLastWorkingDayBeforeDate(calendar.getCat2StartDate(), fullHolidaysSet);
                    if (studyHoliday2 != null) excludedDates.add(studyHoliday2);
                }
            }
        }

        // Get already marked dates (both present and absent)
        // We look back to the course start date to be accurate
        LocalDate searchStart = course.getCourseStartDate() != null ? course.getCourseStartDate() : today.minusMonths(6);
        Set<LocalDate> markedDates = new HashSet<>(
            dateBasedAttendanceRepository.findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                course.getId(), searchStart, today
            ).stream().map(DateBasedAttendance::getAttendanceDate).toList()
        );

        // Determine last class day
        LocalDate lastClassDay = null;
        final Set<LocalDate> finalFullHolidays = calendarHolidays.stream()
                .filter(h -> h.getScope() == Holiday.HolidayScope.FULL)
                .map(Holiday::getDate)
                .collect(java.util.stream.Collectors.toSet());

        if ("FAT".equals(examType)) {
            lastClassDay = isLabFatContext
                    ? resolveLabFatAttendanceWindowEnd(untilDate)
                    : resolveFatAttendanceWindowEnd(untilDate, finalFullHolidays);
        } else {
            lastClassDay = resolveAttendanceWindowEndBeforeExam(untilDate, finalFullHolidays);
        }

        // Start from today if not yet marked, otherwise start from tomorrow
        // We always use the loop to check !markedDates.contains(current), but this initial step
        // optimizes the loop by skipping 'today' if we already have it in markedDates.
        LocalDate current = markedDates.contains(today) ? today.plusDays(1) : today;
        
        while (!current.isAfter(lastClassDay)) {
            // Only count as future if not marked and not a holiday/exam day
            if (current.getDayOfWeek().getValue() >= 1 && current.getDayOfWeek().getValue() <= 6
                    && !markedDates.contains(current) && !excludedDates.contains(current)) {
                
                String dayName = getDayNameFromJavaTime(current.getDayOfWeek().getValue());
                List<TimetableEntry> allEntries = timetableEntryRepository.findByCourseId(course.getId());
                List<TimetableEntry> entries = allEntries.stream()
                        .filter(e -> e.getDayOfWeek().replaceAll("\\s+", "").equalsIgnoreCase(dayName))
                        .toList();

                // Filter out entries that fall on a partial holiday
                LocalDate finalCurrent = current;
                List<Holiday> dayHolidays = calendarHolidays.stream()
                        .filter(h -> h.getDate().equals(finalCurrent))
                        .toList();

                long validEntriesCount = entries.stream()
                        .filter(entry -> {
                            for (Holiday h : dayHolidays) {
                                if (h.getScope() == Holiday.HolidayScope.MORNING && entry.getSession() == TimetableEntry.Session.MORNING) return false;
                                if (h.getScope() == Holiday.HolidayScope.AFTERNOON && entry.getSession() == TimetableEntry.Session.AFTERNOON) return false;
                            }
                            return true;
                        })
                        .count();

                if (validEntriesCount > 0) {
                    totalFutureClasses += (int) validEntriesCount;
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
            case 1 -> "MONDAY";
            case 2 -> "TUESDAY";
            case 3 -> "WEDNESDAY";
            case 4 -> "THURSDAY";
            case 5 -> "FRIDAY";
            case 6 -> "SATURDAY";
            case 7 -> "SUNDAY";
            default -> "";
        };
    }

    /**
     * Calculate future classes available for a course
     * F = number of times the course appears in remaining valid working days
     */
    public int calculateFutureClassesAvailable(Course course) {
        LocalDate today = LocalDate.now();
        // Use consistent exam-based logic for the total count as well
        Optional<Semester> currentCalendar = Optional.ofNullable(course.getSemester())
            .or(() -> semesterRepository.findFirstByActiveTrueOrderBySemesterStartDateDesc());
        String examType = "FAT";
        LocalDate untilDate = today.plusMonths(4);

        if (currentCalendar.isPresent()) {
            Semester calendar = currentCalendar.get();
            if (calendar.getCat1StartDate() != null && !today.isAfter(calendar.getCat1EndDate())) {
                examType = "CAT-1";
                untilDate = calendar.getCat1StartDate();
            } else if (calendar.getCat2StartDate() != null && !today.isAfter(calendar.getCat2EndDate())) {
                examType = "CAT-2";
                untilDate = calendar.getCat2StartDate();
            } else if (calendar.getFatStartDate() != null || calendar.getSemesterEndDate() != null) {
                examType = "FAT";
                untilDate = resolveFatAnchorDateForCourse(calendar, course);
            }
        }

        return calculateFutureClassesAvailableUntilDate(course, untilDate, examType);
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

        double currentPercentage = totalConducted > 0 ? (double) attended / totalConducted * 100 : 0;
        currentPercentage = Math.ceil(currentPercentage);

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

    /**
     * Get the list of dates for future classes for a course
     */
    public List<LocalDate> getFutureClassDates(Course course) {
        LocalDate today = LocalDate.now();
        // Determine current active exam type to use consistent exclusion logic
        Optional<Semester> currentCalendar = Optional.ofNullable(course.getSemester())
            .or(() -> semesterRepository.findFirstByActiveTrueOrderBySemesterStartDateDesc());
        String examType = "FAT"; // Default
        LocalDate untilDate = today.plusMonths(4); // Default far future

        if (currentCalendar.isPresent()) {
            Semester calendar = currentCalendar.get();
            if (calendar.getCat1StartDate() != null && !today.isAfter(calendar.getCat1EndDate())) {
                examType = "CAT-1";
                untilDate = calendar.getCat1StartDate();
            } else if (calendar.getCat2StartDate() != null && !today.isAfter(calendar.getCat2EndDate())) {
                examType = "CAT-2";
                untilDate = calendar.getCat2StartDate();
            } else if (calendar.getFatStartDate() != null || calendar.getSemesterEndDate() != null) {
                examType = "FAT";
                untilDate = resolveFatAnchorDateForCourse(calendar, course);
            }
        }

        return getFutureClassDatesUntilDate(course, untilDate, examType);
    }

    /**
     * Get the list of future class dates until a specific date
     */
    public List<LocalDate> getFutureClassDatesUntilDate(Course course, LocalDate untilDate, String examType) {
        // Create a final copy of today's date to work with consistently
        final LocalDate today = LocalDate.now();
        List<LocalDate> classDates = new ArrayList<>();
        final boolean isLabFatContext = "FAT".equals(examType) && isLabCourse(course);

        log.info("Generating future class dates for course {} until {} (exam: {})", course.getId(), untilDate, examType);

        // Get academic calendar
        Optional<Semester> currentCalendarForDates = Optional.ofNullable(course.getSemester())
            .or(() -> semesterRepository.findFirstByActiveTrueOrderBySemesterStartDateDesc());

        Set<LocalDate> excludedDates = new HashSet<>();
        List<Holiday> calendarHolidays = new ArrayList<>();
        
        if (currentCalendarForDates.isPresent()) {
            Semester calendar = currentCalendarForDates.get();
            
                // Use all holidays for consistent behavior with cutoff date calculation.
                calendarHolidays = holidayRepository.findBySemesterIdOrderByDateAsc(calendar.getId());
            excludedDates.addAll(calendarHolidays.stream()
                    .filter(h -> h.getScope() == Holiday.HolidayScope.FULL)
                    .map(Holiday::getDate)
                    .toList());
            
            Set<LocalDate> fullHolidaysSet = new HashSet<>(excludedDates);

            // For FAT, exclude exam periods but NOT their study holidays or LWDs (those are working days)
            if ("FAT".equals(examType)) {
                if (calendar.getCat1StartDate() != null && calendar.getCat1EndDate() != null) {
                    // Exclude CAT-1 Exam dates
                    LocalDate examDate = calendar.getCat1StartDate();
                    while (!examDate.isAfter(calendar.getCat1EndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }
                if (calendar.getCat2StartDate() != null && calendar.getCat2EndDate() != null) {
                    // Exclude CAT-2 Exam dates
                    LocalDate examDate = calendar.getCat2StartDate();
                    while (!examDate.isAfter(calendar.getCat2EndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }
                
                if (isLabFatContext) {
                    if (calendar.getLabFatStartDate() != null && calendar.getLabFatEndDate() != null) {
                        LocalDate examDate = calendar.getLabFatStartDate();
                        while (!examDate.isAfter(calendar.getLabFatEndDate())) {
                            excludedDates.add(examDate);
                            examDate = examDate.plusDays(1);
                        }
                    }
                } else if (calendar.getFatStartDate() != null && calendar.getFatEndDate() != null) {
                    LocalDate examDate = calendar.getFatStartDate().plusDays(1);
                    while (!examDate.isAfter(calendar.getFatEndDate())) {
                        excludedDates.add(examDate);
                        examDate = examDate.plusDays(1);
                    }
                }
            } else {
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

                // If calculating for CAT-2, only the current CAT-2 study holiday is excluded
                // Previous study holidays (like CAT-1) are treated as working days
                if (calendar.getCat2StartDate() != null && "CAT-2".equals(examType)) {
                    LocalDate studyHoliday2 = getLastWorkingDayBeforeDate(calendar.getCat2StartDate(), fullHolidaysSet);
                    if (studyHoliday2 != null) excludedDates.add(studyHoliday2);
                }
            }
        }

        // Get already marked dates
        Set<LocalDate> markedDates = new HashSet<>(
            dateBasedAttendanceRepository.findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                course.getId(), today.minusMonths(6), today
            ).stream().map(DateBasedAttendance::getAttendanceDate).toList()
        );

        // Determine last class day
        LocalDate lastClassDay = null;
        final Set<LocalDate> finalFullHolidays = calendarHolidays.stream()
                .filter(h -> h.getScope() == Holiday.HolidayScope.FULL)
                .map(Holiday::getDate)
                .collect(java.util.stream.Collectors.toSet());

        if ("FAT".equals(examType)) {
            lastClassDay = isLabFatContext
                    ? resolveLabFatAttendanceWindowEnd(untilDate)
                    : resolveFatAttendanceWindowEnd(untilDate, finalFullHolidays);
        } else {
            lastClassDay = resolveAttendanceWindowEndBeforeExam(untilDate, finalFullHolidays);
        }

        // Start from today if not yet marked, otherwise start from tomorrow
        LocalDate current = markedDates.contains(today) ? today.plusDays(1) : today;
        while (!current.isAfter(lastClassDay)) {
            if (current.getDayOfWeek().getValue() >= 1 && current.getDayOfWeek().getValue() <= 6
                    && !markedDates.contains(current) && !excludedDates.contains(current)) {
                
                String dayName = getDayNameFromJavaTime(current.getDayOfWeek().getValue());
                List<TimetableEntry> entries = timetableEntryRepository
                        .findByCourseIdAndDayOfWeek(course.getId(), dayName);

                // Filter out entries that fall on a partial holiday
                LocalDate finalCurrent = current;
                List<Holiday> dayHolidays = calendarHolidays.stream()
                        .filter(h -> h.getDate().equals(finalCurrent))
                        .toList();

                boolean hasValidEntry = entries.stream()
                        .anyMatch(entry -> {
                            for (Holiday h : dayHolidays) {
                                if (h.getScope() == Holiday.HolidayScope.MORNING && entry.getSession() == TimetableEntry.Session.MORNING) return false;
                                if (h.getScope() == Holiday.HolidayScope.AFTERNOON && entry.getSession() == TimetableEntry.Session.AFTERNOON) return false;
                            }
                            return true;
                        });

                if (hasValidEntry) {
                    classDates.add(current);
                }
            }
            current = current.plusDays(1);
        }
        
        return classDates;
    }

    /**
     * Get the attendance cutoff date for a specific exam type
     */
    public LocalDate getAttendanceCutoffDate(Course course, LocalDate untilDate, String examType, Set<LocalDate> holidays) {
        if (untilDate == null) return null;
        
        if ("FAT".equals(examType)) {
            if (isLabCourse(course)) {
                return resolveLabFatAttendanceWindowEnd(untilDate);
            }
            return resolveFatAttendanceWindowEnd(untilDate, holidays);
        } else {
            return resolveAttendanceWindowEndBeforeExam(untilDate, holidays);
        }
    }

    public LocalDate getAttendanceCutoffDate(LocalDate untilDate, String examType, Set<LocalDate> holidays) {
        return getAttendanceCutoffDate(null, untilDate, examType, holidays);
    }
}
