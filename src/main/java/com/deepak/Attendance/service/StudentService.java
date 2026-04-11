package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.AcademicCalendarDTO;
import com.deepak.Attendance.dto.AttendanceReportDTO;
import com.deepak.Attendance.dto.TimetableConfirmRequest;
import com.deepak.Attendance.dto.TimetableEntryDTO;
import com.deepak.Attendance.dto.WeeklyScheduleItemDTO;
import com.deepak.Attendance.entity.*;
import com.deepak.Attendance.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
public class StudentService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private TimetableEntryRepository timetableEntryRepository;

    @Autowired
    private AttendanceInputRepository attendanceInputRepository;

    @Autowired
    private DateBasedAttendanceRepository dateBasedAttendanceRepository;

    @Autowired
    private AttendanceCalculationService attendanceCalculationService;

    @Autowired
    private HolidayService holidayService;
    
    @Autowired
    private AcademicCalendarService academicCalendarService;
    
    @Autowired
    private AcademicCalendarRepository academicCalendarRepository;

    @Autowired
    private AttendanceReportRepository attendanceReportRepository;

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private CourseCatalogRepository courseCatalogRepository;

    @Value("${app.upload.dir:uploads/timetables}")
    private String uploadDir;

    private CourseCatalog resolveOrCreateCourseCatalog(String courseCode, String courseName) {
        String normalizedCode = courseCode == null ? "" : courseCode.trim().toUpperCase();
        String normalizedName = courseName == null ? "" : courseName.trim();

        return courseCatalogRepository.findByCourseCodeAndCourseName(normalizedCode, normalizedName)
                .orElseGet(() -> courseCatalogRepository.save(new CourseCatalog(null, normalizedCode, normalizedName)));
    }

    /**
     * Returns true when the course has at least one valid class session on the given date
     * after applying holiday scope rules.
     */
    private boolean isClassConductedOnDate(Course course, LocalDate date) {
        if (course == null || date == null) {
            return false;
        }

        List<TimetableEntry> entriesForDay = timetableEntryRepository.findByCourseId(course.getId()).stream()
                .filter(entry -> {
                    String cleanEntryDay = entry.getDayOfWeek().replaceAll("\\s+", "").toUpperCase();
                    String cleanDateDay = date.getDayOfWeek().name().toUpperCase();
                    return cleanEntryDay.equals(cleanDateDay);
                })
                .toList();

        if (entriesForDay.isEmpty()) {
            return false;
        }

        // Exam days are treated as non-instructional days for attendance counting.
        if (isExamDate(date)) {
            return false;
        }

        Optional<Holiday> holidayOpt = holidayRepository.findByDate(date);
        if (holidayOpt.isEmpty()) {
            return true;
        }

        Holiday.HolidayScope scope = holidayOpt.get().getScope() != null
                ? holidayOpt.get().getScope()
                : Holiday.HolidayScope.FULL;

        if (scope == Holiday.HolidayScope.FULL) {
            return false;
        }

        if (scope == Holiday.HolidayScope.MORNING) {
            return entriesForDay.stream().anyMatch(entry -> entry.getSession() != TimetableEntry.Session.MORNING);
        }

        return entriesForDay.stream().anyMatch(entry -> entry.getSession() != TimetableEntry.Session.AFTERNOON);
    }

    private boolean isExamDate(LocalDate date) {
        Optional<AcademicCalendar> calendarOpt = academicCalendarRepository.findFirstByOrderByCreatedAtDesc();
        if (calendarOpt.isEmpty()) {
            return false;
        }

        AcademicCalendar calendar = calendarOpt.get();
        return isDateWithin(date, calendar.getCat1StartDate(), calendar.getCat1EndDate())
                || isDateWithin(date, calendar.getCat2StartDate(), calendar.getCat2EndDate())
                || isDateWithin(date, calendar.getFatStartDate(), calendar.getFatEndDate());
    }

    private boolean isDateWithin(LocalDate date, LocalDate start, LocalDate end) {
        return date != null && start != null && end != null && !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * Confirm and save timetable (add or update courses)
     */
    @CacheEvict(value = "attendanceReports", key = "#userId")
    public void confirmTimetable(Long userId, List<TimetableEntryDTO> confirmedData) {
        // Save new courses and timetable entries (add/update, don't delete)
        for (TimetableEntryDTO dto : confirmedData) {
            CourseCatalog catalog = resolveOrCreateCourseCatalog(dto.getCourseCode(), dto.getCourseName());
            // Check if course already exists
            Optional<Course> existing = courseRepository
                    .findByUserIdAndCourseCatalog_CourseCodeIgnoreCase(userId, dto.getCourseCode());

            Course course;
            if (existing.isPresent()) {
                course = existing.get();
                course.setCourseName(dto.getCourseName());
                course.setCourseCode(dto.getCourseCode());
                course.setCourseCatalog(catalog);
                course.setSlot(dto.getSlot()); // Set course level slot
            } else {
                course = new Course();
                course.setUserId(userId);
                course.setCourseCode(dto.getCourseCode());
                course.setCourseName(dto.getCourseName());
                course.setCourseCatalog(catalog);
                course.setSlot(dto.getSlot()); // Set course level slot
                course.setTimetableEntries(new ArrayList<>());
            }

            course = courseRepository.save(course);

            // Delete old timetable entries for this course and recreate them
            List<TimetableEntry> oldEntries = timetableEntryRepository.findByCourseId(course.getId());
            if (!oldEntries.isEmpty()) {
                timetableEntryRepository.deleteAll(oldEntries);
            }

            // Save timetable entries
            for (WeeklyScheduleItemDTO item : dto.getWeeklySchedule()) {
                TimetableEntry timetableEntry = new TimetableEntry();
                timetableEntry.setCourse(course);
                timetableEntry.setDayOfWeek(item.getDayOfWeek());
                timetableEntry.setSession(item.getSession());
                timetableEntry.setClassesCount(item.getClassesCount());
                timetableEntryRepository.save(timetableEntry);
            }

            // Course schedule updates change projected attendance, so recalculate cache.
            calculateAndCacheAttendanceReport(course);
        }

        log.info("Timetable confirmed and saved for user: {}", userId);
    }

    /**
     * Confirm and save timetable with course start dates
     */
    @CacheEvict(value = "attendanceReports", key = "#userId")
    public void confirmTimetableWithDates(Long userId, List<TimetableConfirmRequest> confirmedData) {
        for (TimetableConfirmRequest req : confirmedData) {
            CourseCatalog catalog = resolveOrCreateCourseCatalog(req.getCourseCode(), req.getCourseName());
            // Check if course already exists
            Optional<Course> existing = courseRepository
                    .findByUserIdAndCourseCatalog_CourseCodeIgnoreCase(userId, req.getCourseCode());

            Course course;
            if (existing.isPresent()) {
                course = existing.get();
                course.setCourseName(req.getCourseName());
                course.setCourseCode(req.getCourseCode());
                course.setCourseCatalog(catalog);
                course.setSlot(req.getSlot()); // Set course level slot
                if (req.getCourseStartDate() != null) {
                    course.setCourseStartDate(req.getCourseStartDate());
                }
            } else {
                course = new Course();
                course.setUserId(userId);
                course.setCourseCode(req.getCourseCode());
                course.setCourseName(req.getCourseName());
                course.setCourseCatalog(catalog);
                course.setSlot(req.getSlot()); // Set course level slot
                course.setCourseStartDate(req.getCourseStartDate());
                course.setTimetableEntries(new ArrayList<>());
            }

            course = courseRepository.save(course);

            // Delete old timetable entries for this course and recreate them
            List<TimetableEntry> oldEntries = timetableEntryRepository.findByCourseId(course.getId());
            if (!oldEntries.isEmpty()) {
                timetableEntryRepository.deleteAll(oldEntries);
            }

            // Save timetable entries
            for (WeeklyScheduleItemDTO item : req.getWeeklySchedule()) {
                TimetableEntry timetableEntry = new TimetableEntry();
                timetableEntry.setCourse(course);
                timetableEntry.setDayOfWeek(item.getDayOfWeek());
                timetableEntry.setSession(item.getSession() != null ? item.getSession() : TimetableEntry.Session.MORNING);
                timetableEntry.setClassesCount(item.getClassesCount() != null ? item.getClassesCount() : 1);
                timetableEntryRepository.save(timetableEntry);
            }

            // Start date/timetable edits should immediately reflect in report values.
            calculateAndCacheAttendanceReport(course);
        }

        log.info("Timetable confirmed and saved for user: {}", userId);
    }

    /**
     * Save attendance input for a course
     */
    @CacheEvict(value = "attendanceReports", key = "#userId")
    public void saveAttendanceInput(Long userId, String courseCode, 
                                   Integer totalConducted, Integer attended) {
        Optional<Course> course = courseRepository.findByUserIdAndCourseCatalog_CourseCodeIgnoreCase(userId, courseCode);
        if (course.isEmpty()) {
            throw new IllegalArgumentException("Course not found for user: " + courseCode);
        }

        Course c = course.get();
        
        // Check if attendance input already exists
        Optional<AttendanceInput> existing = attendanceInputRepository.findByCourseId(c.getId());

        AttendanceInput input;
        if (existing.isPresent()) {
            input = existing.get();
        } else {
            input = new AttendanceInput();
            input.setCourse(c);
        }

        input.setTotalClassesConducted(totalConducted);
        input.setClassesAttended(attended);
        attendanceInputRepository.save(input);

        // Also generate date-based attendance records from total/attended
        // This allows mark-by-date to show the records and allows overriding later
        generateDateBasedAttendanceFromTotal(c.getId(), attended, totalConducted);

        // Calculate and cache the attendance report for this course
        calculateAndCacheAttendanceReport(c);

        log.info("Attendance input saved for course: {} with attended: {}/{}", 
                courseCode, attended, totalConducted);
    }

    /**
     * Generate date-based attendance records from total/attended counts
     * Marks the LAST (total - attended) dates as absent
     */
    private void generateDateBasedAttendanceFromTotal(Long courseId, Integer attended, Integer total) {
        try {
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null || course.getCourseStartDate() == null) {
                return; // Cannot generate without start date
            }

            // Get all class dates
            List<Map<String, Object>> classDates = getClassDatesForCourse(course.getUserId(), courseId);
            
            if (classDates.isEmpty()) {
                return;
            }

            // Delete existing date-based attendance for this course
            dateBasedAttendanceRepository.deleteByCourseId(courseId);

            // Mark first 'attended' dates as present, remaining as absent
            // Logic change: Use Math.min to prevent index out of bounds
            int limit = Math.min(classDates.size(), total);
            for (int i = 0; i < limit; i++) {
                Map<String, Object> dateInfo = classDates.get(i);
                LocalDate date = LocalDate.parse((String) dateInfo.get("date"));
                
                DateBasedAttendance dba = new DateBasedAttendance();
                dba.setCourse(course);
                dba.setAttendanceDate(date);
                dba.setAttended(i < attended); // First 'attended' count = true, rest = false
                
                dateBasedAttendanceRepository.save(dba);
            }
            
            // If total was greater than classes found, we should update the total to classDates.size()
            // but for now we just log it.
            if (total > classDates.size()) {
                log.warn("Total classes conducted ({}) is greater than calendar classes found ({})", total, classDates.size());
            }

            log.info("Generated date-based attendance for course {} from total/attended", courseId);
        } catch (Exception e) {
            log.warn("Could not generate date-based attendance records", e);
            // Don't fail the save - just log and continue
        }
    }

    /**
     * Get attendance report for all courses of a student (from cached data)
     */
    @Cacheable(value = "attendanceReports", key = "#userId")
    public List<AttendanceReportDTO> getAttendanceReport(Long userId) {
        List<AttendanceReport> cachedReports = attendanceReportRepository.findByCourse_UserId(userId);
        List<AttendanceReportDTO> reports = new ArrayList<>();

        for (AttendanceReport cached : cachedReports) {
            AttendanceReportDTO report = new AttendanceReportDTO();
            report.setCourseCode(cached.getCourse().getCourseCode());
            report.setCourseName(cached.getCourse().getCourseName());
            report.setCourseId(cached.getCourse().getId());
            
            report.setCurrentPercentage(cached.getCurrentPercentage());
            report.setFutureClassesAvailable(cached.getFutureClassesAvailable());
            report.setMinimumClassesToAttend(cached.getMinimumClassesToAttend75());
            report.setStatus(cached.getStatus());
            report.setTotalClassesConducted(cached.getTotalClassesConducted());
            report.setClassesAttended(cached.getClassesAttended());
            
            report.setClassesCanSkip75(cached.getClassesCanSkip75());
            report.setClassesCanSkip65(cached.getClassesCanSkip65());
            report.setStale(cached.getIsStale());
            
            // Populate future class dates for the UI
            report.setFutureClassDates(attendanceCalculationService.getFutureClassDates(cached.getCourse()));
            
            if (cached.getUpcomingExamName() != null) {
                report.setUpcomingExamName(cached.getUpcomingExamName());
                report.setUpcomingExamStartDate(cached.getUpcomingExamStartDate());
                report.setUpcomingExamEndDate(cached.getUpcomingExamEndDate());
                report.setUpcomingExamEligible(cached.getUpcomingExamEligible75());
                report.setUpcomingExamEligibleRelaxed(cached.getUpcomingExamEligible65());
                report.setFutureClassesAvailable(cached.getFutureClassesUntilExam() != null ? cached.getFutureClassesUntilExam() : cached.getFutureClassesAvailable());
                report.setMinimumClassesToAttend(cached.getMinimumClassesToAttendForExam75() != null ? cached.getMinimumClassesToAttendForExam75() : cached.getMinimumClassesToAttend75());
                report.setClassesCanSkip75(cached.getClassesCanSkipUntilExam75() != null ? cached.getClassesCanSkipUntilExam75() : cached.getClassesCanSkip75());
                report.setClassesCanSkip65(cached.getClassesCanSkipUntilExam65() != null ? cached.getClassesCanSkipUntilExam65() : cached.getClassesCanSkip65());
                report.setProjectedAttendancePercentage(cached.getProjectedAttendancePercentage());
                
                // Populate future class dates until exam
                report.setFutureClassDatesUntilExam(attendanceCalculationService.getFutureClassDatesUntilDate(
                    cached.getCourse(), cached.getUpcomingExamStartDate(), cached.getUpcomingExamName()));
            }

            // Populate multi-exam reports
            AcademicCalendar calendar = academicCalendarRepository.findFirstByOrderByCreatedAtDesc().orElse(null);
            if (calendar != null) {
                Set<LocalDate> holidays = new HashSet<>(holidayService.getAllHolidays().stream().map(h -> h.getDate()).toList());
                
                report.setCat1Report(calculateExamEligibility(cached.getCourse(), "CAT-1", calendar.getCat1StartDate(), calendar.getCat1EndDate(), cached.getClassesAttended(), cached.getTotalClassesConducted(), holidays));
                report.setCat2Report(calculateExamEligibility(cached.getCourse(), "CAT-2", calendar.getCat2StartDate(), calendar.getCat2EndDate(), cached.getClassesAttended(), cached.getTotalClassesConducted(), holidays));
                // Use calendar.getExamStartDate() as the Instructional End Date for FAT analysis
                report.setFatReport(calculateExamEligibility(cached.getCourse(), "FAT", calendar.getExamStartDate(), calendar.getFatEndDate(), cached.getClassesAttended(), cached.getTotalClassesConducted(), holidays));
                
                // Determine main report exam
                LocalDate today = LocalDate.now();
                if (calendar.getCat1StartDate() != null && !today.isAfter(calendar.getCat1EndDate())) {
                    report.setMainReportExam("CAT-1");
                } else if (calendar.getCat2StartDate() != null && !today.isAfter(calendar.getCat2EndDate())) {
                    report.setMainReportExam("CAT-2");
                } else {
                    report.setMainReportExam("FAT");
                }
            }
            
            reports.add(report);
        }

        return reports;
    }

    private com.deepak.Attendance.dto.ExamEligibilityDTO calculateExamEligibility(Course course, String examType, LocalDate start, LocalDate end, int attended, int totalConducted, Set<LocalDate> holidays) {
        if (start == null) return null;
        
        com.deepak.Attendance.dto.ExamEligibilityDTO dto = new com.deepak.Attendance.dto.ExamEligibilityDTO();
        dto.setExamName(examType);
        dto.setExamStartDate(start);
        dto.setExamEndDate(end);
        dto.setAvailable(true);
        
        LocalDate today = LocalDate.now();
        dto.setUpcoming(today.isBefore(start));
        dto.setOngoing(end != null && !today.isBefore(start) && !today.isAfter(end));
        dto.setCompleted(end != null && today.isAfter(end));
        
        LocalDate cutoff = attendanceCalculationService.getAttendanceCutoffDate(start, examType, holidays);
        dto.setAttendanceCutoffDate(cutoff);
        
        // Filter attendance records to only include those up to the cutoff date for this exam.
        // Do not fall back to global totals here, because those may include dates beyond cutoff (e.g., LWD).
        int relevantConducted = 0;
        int relevantAttended = 0;
        if (cutoff != null) {
            LocalDate startDate = course.getCourseStartDate() != null ? course.getCourseStartDate() : today.minusMonths(4);
            List<DateBasedAttendance> relevantRecords = dateBasedAttendanceRepository
                    .findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(course.getId(), startDate, cutoff);

            List<DateBasedAttendance> validRelevantRecords = relevantRecords.stream()
                .filter(record -> isClassConductedOnDate(course, record.getAttendanceDate()))
                .toList();

            relevantConducted = validRelevantRecords.size();
            relevantAttended = (int) validRelevantRecords.stream().filter(DateBasedAttendance::getAttended).count();
        }

        dto.setTotalClassesConductedUntilCutoff(relevantConducted);
        dto.setClassesAttendedUntilCutoff(relevantAttended);
        
        int futureUntilExam = attendanceCalculationService.calculateFutureClassesAvailableUntilDate(course, start, examType);
        int totalUntilExam = relevantConducted + futureUntilExam;
        dto.setFutureClassesUntilExam(futureUntilExam);
        
        // Use existing logic pattern for min classes
        int min75 = relevantAttended;
        for (int a = relevantAttended; a <= relevantAttended + futureUntilExam; a++) {
            double pct = totalUntilExam > 0 ? (double) a / totalUntilExam * 100 : 0;
            if (Math.ceil(pct) >= 75.0) { min75 = a; break; }
        }
        int add75 = Math.max(0, min75 - relevantAttended);
        dto.setMinimumClassesToAttend75(add75);
        dto.setClassesCanSkip75(Math.max(0, futureUntilExam - add75));
        dto.setEligible75(add75 <= futureUntilExam);
        
        // Calculate target percentage if all skip classes are used (75%)
        int totalAttended75 = relevantAttended + add75;
        double targetVal75 = totalUntilExam > 0 ? (double) totalAttended75 / totalUntilExam * 100 : 0;
        dto.setTargetPercentage75(Math.ceil(targetVal75));
        
        int min65 = relevantAttended;
        for (int a = relevantAttended; a <= relevantAttended + futureUntilExam; a++) {
            double pct = totalUntilExam > 0 ? (double) a / totalUntilExam * 100 : 0;
            if (Math.ceil(pct) >= 65.0) { min65 = a; break; }
        }
        int add65 = Math.max(0, min65 - relevantAttended);
        dto.setMinimumClassesToAttend65(add65);
        dto.setClassesCanSkip65(Math.max(0, futureUntilExam - add65));
        dto.setEligible65(add65 <= futureUntilExam);

        // Calculate target percentage if all skip classes are used (65%)
        int totalAttended65 = relevantAttended + add65;
        double targetVal65 = totalUntilExam > 0 ? (double) totalAttended65 / totalUntilExam * 100 : 0;
        dto.setTargetPercentage65(Math.ceil(targetVal65));
        
        double projected = totalUntilExam > 0 ? (double) (relevantAttended + futureUntilExam) / totalUntilExam * 100 : 0;
        dto.setProjectedPercentage(Math.ceil(projected));
        dto.setFutureClassDates(attendanceCalculationService.getFutureClassDatesUntilDate(course, start, examType));
        
        return dto;
    }

    /**
     * Calculate and cache attendance report for a specific course
     * This is called when attendance is saved to pre-calculate the report
     */
    private void calculateAndCacheAttendanceReport(Course course) {
        try {
            Optional<AcademicCalendarDTO> calendarOpt = academicCalendarService.getCurrentAcademicCalendar();
            LocalDate today = LocalDate.now();

            // Get attendance data
            LocalDate startDate = course.getCourseStartDate() != null ? course.getCourseStartDate() : today.minusMonths(4);
            List<DateBasedAttendance> dateBasedRecords = dateBasedAttendanceRepository
                    .findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(course.getId(), startDate, today);
            
            int totalClasses;
            int attendedClasses;
            
            if (!dateBasedRecords.isEmpty()) {
                // Use date-wise attendance records as the source of truth for overall counters.
                totalClasses = dateBasedRecords.size();
                attendedClasses = (int) dateBasedRecords.stream().filter(DateBasedAttendance::getAttended).count();
            } else {
                Optional<AttendanceInput> input = attendanceInputRepository.findByCourseId(course.getId());
                if (input.isPresent()) {
                    totalClasses = input.get().getTotalClassesConducted();
                    attendedClasses = input.get().getClassesAttended();
                } else {
                    // Keep report materialized with zero baseline so stale banners can clear
                    // after manual refresh even before first attendance entry exists.
                    totalClasses = 0;
                    attendedClasses = 0;
                }
            }
            
            int futureClasses = attendanceCalculationService.calculateFutureClassesAvailable(course);

            // Calculate required classes
            AttendanceCalculationService.AttendanceCalculationResult result =
                    attendanceCalculationService.calculateRequiredClasses(
                            totalClasses,
                            attendedClasses,
                            futureClasses
                    );

            // Create or update report cache
            AttendanceReport cachedReport = attendanceReportRepository.findByCourseId(course.getId())
                    .orElse(new AttendanceReport());
            
            cachedReport.setCourse(course);
            cachedReport.setTotalClassesConducted(totalClasses);
            cachedReport.setClassesAttended(attendedClasses);
            cachedReport.setCurrentPercentage(result.currentPercentage());
            cachedReport.setFutureClassesAvailable(result.futureClasses());
            cachedReport.setMinimumClassesToAttend75(result.minClassesRequired());
            cachedReport.setStatus(result.status());
            
            // Calculate classes that can be skipped
            int totalWithFuture = totalClasses + futureClasses;
            int attended = attendedClasses;
            
            int maxSkip75 = (int) Math.floor(attended + futureClasses - (0.75 * totalWithFuture));
            cachedReport.setClassesCanSkip75(Math.max(0, maxSkip75));
            
            int maxSkip65 = (int) Math.floor(attended + futureClasses - (0.65 * totalWithFuture));
            cachedReport.setClassesCanSkip65(Math.max(0, maxSkip65));
            
            // Calculate minimum classes needed for 65%
            int futureForCalc = futureClasses;
            int minFor65 = attended;
            for (int att = attended; att <= attended + futureForCalc; att++) {
                double pct = totalWithFuture > 0 ? (double) att / totalWithFuture * 100 : 0;
                if (pct >= 65.0) {
                    minFor65 = att;
                    break;
                }
            }
            cachedReport.setMinimumClassesToAttend65(Math.max(0, minFor65 - attended));
            
            // Handle upcoming exam info if available
            if (calendarOpt.isPresent()) {
                AcademicCalendarDTO calendar = calendarOpt.get();
                
                String upcomingExam = null;
                LocalDate upcomingStart = null;
                LocalDate upcomingEnd = null;
                
                if (calendar.getCat1StartDate() != null && today.isBefore(calendar.getCat1StartDate())) {
                    upcomingExam = "CAT-1";
                    upcomingStart = calendar.getCat1StartDate();
                    upcomingEnd = calendar.getCat1EndDate();
                } else if (calendar.getCat2StartDate() != null && today.isBefore(calendar.getCat2StartDate())) {
                    upcomingExam = "CAT-2";
                    upcomingStart = calendar.getCat2StartDate();
                    upcomingEnd = calendar.getCat2EndDate();
                } else if (calendar.getFatStartDate() != null && today.isBefore(calendar.getFatStartDate())) {
                    upcomingExam = "FAT";
                    upcomingStart = calendar.getFatStartDate();
                    upcomingEnd = calendar.getFatEndDate();
                }
                
                if (upcomingExam != null && upcomingStart != null) {
                    cachedReport.setUpcomingExamName(upcomingExam);
                    cachedReport.setUpcomingExamStartDate(upcomingStart);
                    cachedReport.setUpcomingExamEndDate(upcomingEnd);
                    
                    int futureClassesUntilExam = attendanceCalculationService
                            .calculateFutureClassesAvailableUntilDate(course, upcomingStart, upcomingExam);
                    int totalClassesUntilExam = totalClasses + futureClassesUntilExam;
                    
                    cachedReport.setFutureClassesUntilExam(futureClassesUntilExam);
                    
                    // Calculate for 75%
                    int minClassesNeededFor75 = attendedClasses;
                    for (int att75 = attendedClasses; att75 <= attendedClasses + futureClassesUntilExam; att75++) {
                        double pct75 = totalClassesUntilExam > 0 
                            ? (double) att75 / totalClassesUntilExam * 100 
                            : 0;
                        if (Math.ceil(pct75) >= 75.0) {
                            minClassesNeededFor75 = att75;
                            break;
                        }
                    }
                    int additionalFor75 = Math.max(0, minClassesNeededFor75 - attendedClasses);
                    cachedReport.setMinimumClassesToAttendForExam75(additionalFor75);
                    cachedReport.setClassesCanSkipUntilExam75(Math.max(0, futureClassesUntilExam - additionalFor75));
                    cachedReport.setUpcomingExamEligible75(additionalFor75 <= futureClassesUntilExam);
                    
                    double projectedPct = totalClassesUntilExam > 0 
                        ? (double) (attendedClasses + additionalFor75) / totalClassesUntilExam * 100 
                        : 0;
                    cachedReport.setProjectedAttendancePercentage(Math.ceil(projectedPct));
                    
                    // Calculate for 65%
                    int minClassesNeededFor65 = attendedClasses;
                    for (int att65 = attendedClasses; att65 <= attendedClasses + futureClassesUntilExam; att65++) {
                        double pct65 = totalClassesUntilExam > 0 
                            ? (double) att65 / totalClassesUntilExam * 100 
                            : 0;
                        if (Math.ceil(pct65) >= 65.0) {
                            minClassesNeededFor65 = att65;
                            break;
                        }
                    }
                    int additionalFor65 = Math.max(0, minClassesNeededFor65 - attendedClasses);
                    cachedReport.setMinimumClassesToAttendForExam65(additionalFor65);
                    cachedReport.setClassesCanSkipUntilExam65(Math.max(0, futureClassesUntilExam - additionalFor65));
                    cachedReport.setUpcomingExamEligible65(additionalFor65 <= futureClassesUntilExam);
                }
            } else {
                cachedReport.setUpcomingExamEligible75(false);
                cachedReport.setUpcomingExamEligible65(false);
            }
            
            // Mark as not stale since we just recalculated
            cachedReport.setIsStale(false);
            
            attendanceReportRepository.save(cachedReport);
            log.info("Cached attendance report for course: {}", course.getCourseCode());
            
        } catch (Exception e) {
            log.warn("Failed to cache attendance report for course: {}", course.getCourseCode(), e);
            // Don't fail the save - just log the error
        }
    }

    @Transactional
    public void autoMarkTodayAttendance(Long userId) {
        LocalDate today = LocalDate.now();
        String dayOfWeek = today.getDayOfWeek().toString();
        log.info("Auto-marking attendance for user: {} for today: {} ({})", userId, today, dayOfWeek);

        List<Course> courses = courseRepository.findByUserId(userId);
        for (Course course : courses) {
            // Check if course has a valid class today after holiday scope rules.
            boolean hasClassToday = isClassConductedOnDate(course, today);

            if (hasClassToday) {
                // Check if attendance already exists for today
                Optional<DateBasedAttendance> existing = dateBasedAttendanceRepository
                        .findByCourseIdAndAttendanceDate(course.getId(), today);

                if (existing.isEmpty()) {
                    log.info("Auto-marking today's attendance as PRESENT for course: {}", course.getCourseCode());
                    DateBasedAttendance attendance = new DateBasedAttendance();
                    attendance.setCourse(course);
                    attendance.setAttendanceDate(today);
                    attendance.setAttended(true); // Default to Present
                    dateBasedAttendanceRepository.save(attendance);
                    
                    // Force refresh because we added new data
                    attendanceReportRepository.findByCourseId(course.getId())
                            .ifPresent(report -> {
                                report.setIsStale(true);
                                attendanceReportRepository.save(report);
                            });
                }
            } else {
                // If a holiday made today's class invalid, remove stale today's attendance record if present.
                dateBasedAttendanceRepository.findByCourseIdAndAttendanceDate(course.getId(), today)
                        .ifPresent(existing -> {
                            dateBasedAttendanceRepository.delete(existing);
                            attendanceReportRepository.findByCourseId(course.getId())
                                    .ifPresent(report -> {
                                        report.setIsStale(true);
                                        attendanceReportRepository.save(report);
                                    });
                        });
            }
        }
    }

    /**
     * Refresh attendance reports for a specific student's courses
     * Returns true if reports were refreshed
     */
    @CacheEvict(value = "attendanceReports", key = "#userId")
    @Transactional
    public boolean refreshAttendanceReports(Long userId) {
        log.info("Force refreshing attendance reports for user: {}", userId);
        
        // Auto-mark today's attendance before refreshing
        autoMarkTodayAttendance(userId);
        
        List<Course> courses = courseRepository.findByUserId(userId);
        boolean refreshed = !courses.isEmpty();
        
        for (Course course : courses) {
            removeInvalidDateBasedRecords(course);
            // User-triggered refresh should always recompute so UI reflects latest attendance immediately.
            calculateAndCacheAttendanceReport(course);
        }
        return refreshed;
    }

    private void removeInvalidDateBasedRecords(Course course) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = course.getCourseStartDate() != null ? course.getCourseStartDate() : today.minusMonths(6);

        List<DateBasedAttendance> records = dateBasedAttendanceRepository
                .findByCourseIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(course.getId(), startDate, today);

        for (DateBasedAttendance record : records) {
            if (!isClassConductedOnDate(course, record.getAttendanceDate())) {
                dateBasedAttendanceRepository.delete(record);
                log.info("Removed invalid attendance record for course {} on {}", course.getCourseCode(), record.getAttendanceDate());
            }
        }
    }

    /**
     * Mark all attendance reports as stale.
     * Called when holidays or academic calendar changes to avoid heavy immediate recalculation
     */
    @Transactional
    public void markAllAttendanceReportsAsStale() {
        log.info("Marking all attendance reports as stale due to configuration change");
        attendanceReportRepository.markAllReportsAsStale();
    }

    public boolean isReportStale(Long courseId) {
        return attendanceReportRepository.findByCourseId(courseId)
                .map(AttendanceReport::getIsStale)
                .orElse(true);
    }

    public List<Course> getStudentCourses(Long userId) {
        return courseRepository.findByUserId(userId);
    }

    /**
     * Recalculate attendance reports for all students (all their courses)
     * Called when holidays or academic calendar changes
     */
    @Transactional
    public void recalculateAttendanceReportsForAllStudents() {
        try {
            log.info("Starting recalculation of attendance reports for all students");
            
            // Get all courses
            List<Course> allCourses = courseRepository.findAll();
            
            for (Course course : allCourses) {
                try {
                    calculateAndCacheAttendanceReport(course);
                } catch (Exception e) {
                    log.warn("Failed to recalculate report for course: {}", course.getCourseCode(), e);
                    // Continue processing other courses even if one fails
                }
            }
            
            log.info("Completed recalculation of attendance reports for all students. Processed {} courses", allCourses.size());
        } catch (Exception e) {
            log.error("Error recalculating all attendance reports", e);
            throw e;
        }
    }

    /**
     * Recalculate attendance reports for a specific student (all their courses)
     * Called when holidays or academic calendar changes
     */
    @Transactional
    public void recalculateAttendanceReportsForStudent(Long userId) {
        try {
            log.info("Starting recalculation of attendance reports for student: {}", userId);
            
            // Get all courses for this student
            List<Course> studentCourses = courseRepository.findByUserId(userId);
            
            for (Course course : studentCourses) {
                try {
                    calculateAndCacheAttendanceReport(course);
                } catch (Exception e) {
                    log.warn("Failed to recalculate report for course: {} for student: {}", course.getCourseCode(), userId, e);
                    // Continue processing other courses even if one fails
                }
            }
            
            log.info("Completed recalculation of attendance reports for student: {}. Processed {} courses", userId, studentCourses.size());
        } catch (Exception e) {
            log.error("Error recalculating attendance reports for student: {}", userId, e);
            throw e;
        }
    }

    /**
     * Recalculate attendance report for a specific course
     * Called when student attendance is updated
     */
    public void recalculateAttendanceReportForCourse(Long courseId) {
        try {
            log.info("Recalculating attendance report for course ID: {}", courseId);
            
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                log.warn("Course not found for ID: {}", courseId);
                return;
            }
            
            calculateAndCacheAttendanceReport(course);
            log.info("Successfully recalculated attendance report for course: {}", course.getCourseCode());
        } catch (Exception e) {
            log.error("Error recalculating attendance report for course ID: {}", courseId, e);
            throw e;
        }
    }

    /**
     * Get student's timetable
     */
    public List<TimetableEntryDTO> getStudentTimetable(Long userId) {
        List<Course> courses = courseRepository.findByUserId(userId);
        List<TimetableEntryDTO> timetables = new ArrayList<>();

        for (Course course : courses) {
            List<WeeklyScheduleItemDTO> weeklySchedule = new ArrayList<>();
            List<TimetableEntry> entries = timetableEntryRepository.findByCourseId(course.getId());

            for (TimetableEntry entry : entries) {
                weeklySchedule.add(new WeeklyScheduleItemDTO(
                        entry.getDayOfWeek(),
                        entry.getSession(),
                        entry.getClassesCount()
                ));
            }

            // Even if weeklySchedule is empty, we must show the course
            TimetableEntryDTO dto = new TimetableEntryDTO(
                    course.getId(),
                    course.getCourseCode(),
                    course.getCourseName(),
                    course.getSlot(),
                    weeklySchedule,
                    course.getCourseStartDate()
            );
            timetables.add(dto);
        }

        return timetables;
    }

    /**
     * Get all courses for a student (alias for getStudentTimetable)
     */
    public List<TimetableEntryDTO> getStudentTimetableDTOs(Long userId) {
        return getStudentTimetable(userId);
    }

    public List<Course> getStudentCoursesEntity(Long userId) {
        return courseRepository.findByUserId(userId);
    }

    /**
     * Get all holidays for student viewing
     */
    public List<com.deepak.Attendance.dto.HolidayDTO> getAllHolidays() {
        return holidayService.getAllHolidays();
    }

    /**
     * Delete a course by course code for a specific user
     */
    public void deleteCourse(Long userId, String courseCode) {
        Optional<Course> course = courseRepository.findByUserIdAndCourseCatalog_CourseCodeIgnoreCase(userId, courseCode);
        if (course.isEmpty()) {
            throw new IllegalArgumentException("Course not found: " + courseCode);
        }
        
        Course c = course.get();
        
        // Delete related attendance inputs (may have multiple records)
        List<AttendanceInput> attendanceInputs = attendanceInputRepository.findAllByCourseId(c.getId());
        attendanceInputRepository.deleteAll(attendanceInputs);
        
        // Delete the course (this will cascade delete timetable entries)
        courseRepository.delete(c);
        
        log.info("Deleted course {} for user {}", courseCode, userId);
    }
    
    /**
     * Delete a course by course ID
     */
    @Transactional
    public void deleteCourse(Long userId, Long courseId) {
        Optional<Course> course = courseRepository.findById(courseId);
        if (course.isEmpty() || !course.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Course not found or unauthorized");
        }
        
        Course c = course.get();
        
        // Delete attendance reports first (may have multiple records)
        List<AttendanceReport> attendanceReports = attendanceReportRepository.findAllByCourseId(c.getId());
        attendanceReportRepository.deleteAll(attendanceReports);
        
        // Delete date-based attendance records
        dateBasedAttendanceRepository.deleteByCourseId(c.getId());
        
        // Delete related attendance inputs (may have multiple records)
        List<AttendanceInput> attendanceInputs = attendanceInputRepository.findAllByCourseId(c.getId());
        attendanceInputRepository.deleteAll(attendanceInputs);
        
        // Delete timetable entries
        List<TimetableEntry> entries = timetableEntryRepository.findByCourseId(c.getId());
        timetableEntryRepository.deleteAll(entries);
        
        // Delete the course
        courseRepository.delete(c);
        
        log.info("Deleted course ID {} for user {}", courseId, userId);
    }

    /**
     * Auto-generate "Present" attendance from course start date until today
     */
    @Transactional
    public int autoGeneratePresentAttendance(Long userId, Long courseId) {
        Optional<Course> courseOpt = courseRepository.findById(courseId);
        if (courseOpt.isEmpty() || !courseOpt.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Course not found or unauthorized");
        }

        Course course = courseOpt.get();
        if (course.getCourseStartDate() == null) {
            throw new IllegalArgumentException("Course start date is not set");
        }

        LocalDate start = course.getCourseStartDate();
        LocalDate today = LocalDate.now();
        if (start.isAfter(today)) {
            return 0; // Future course
        }

        // Get all holidays to exclude them
        Set<LocalDate> holidays = new HashSet<>(holidayService.getAllHolidays().stream().map(h -> h.getDate()).toList());
        
        // Get valid working days for this course's timetable
        Map<String, Integer> schedule = new HashMap<>();
        for (TimetableEntry entry : course.getTimetableEntries()) {
            String cleanDay = entry.getDayOfWeek().replaceAll("\\s+", "").toUpperCase();
            schedule.put(cleanDay, entry.getClassesCount());
        }

        int count = 0;
        LocalDate current = start;
        while (!current.isAfter(today)) {
            String dayName = current.getDayOfWeek().name().toUpperCase();
            if (schedule.containsKey(dayName) && !holidays.contains(current) && !isExamDate(current)) {
                // Only create if it doesn't exist
                Optional<DateBasedAttendance> existing = dateBasedAttendanceRepository.findByCourseIdAndAttendanceDate(courseId, current);
                if (existing.isEmpty()) {
                    DateBasedAttendance dba = new DateBasedAttendance();
                    dba.setCourse(course);
                    dba.setAttendanceDate(current);
                    dba.setAttended(true); // Default to Present
                    dateBasedAttendanceRepository.save(dba);
                    count++;
                }
            }
            current = current.plusDays(1);
        }

        if (count > 0) {
            // Delete the manual AttendanceInput record if any exists (to switch to date-based)
            attendanceInputRepository.findByCourseId(courseId).ifPresent(attendanceInputRepository::delete);
            calculateAndCacheAttendanceReport(course);
        }

        return count;
    }

    /**
     * Get all class dates for a course (from start date until today, excluding holidays)
     * Returns dates where classes should be conducted based on weekly schedule
     */
    public List<Map<String, Object>> getClassDatesForCourse(Long userId, Long courseId) {
        Optional<Course> course = courseRepository.findById(courseId);
        if (course.isEmpty() || !course.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Course not found or unauthorized");
        }

        Course c = course.get();
        
        // Check if course has a start date
        if (c.getCourseStartDate() == null) {
            throw new IllegalArgumentException("Course start date not set. Please set the course start date first.");
        }

        LocalDate startDate = c.getCourseStartDate();
        LocalDate today = LocalDate.now();

        // Get all timetable entries for this course (days of week and class count)
        List<TimetableEntry> timetableEntries = timetableEntryRepository.findByCourseId(courseId);
        
        if (timetableEntries.isEmpty()) {
            throw new IllegalArgumentException("No weekly schedule set for this course");
        }

        // Get all holidays and exam dates to exclude
        Map<LocalDate, Holiday.HolidayScope> holidayMap = new HashMap<>();
        
        // Add holidays
        List<Holiday> holidays = holidayRepository.findAll();
        holidays.forEach(h -> holidayMap.put(h.getDate(), h.getScope() != null ? h.getScope() : Holiday.HolidayScope.FULL));
        
        // Generate list of class dates
        List<Map<String, Object>> classDates = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(today)) {
            Holiday.HolidayScope holidayScope = holidayMap.get(currentDate);
            boolean isExamDay = isExamDate(currentDate);

            if (!isExamDay && (holidayScope == null || holidayScope != Holiday.HolidayScope.FULL)) {
                for (TimetableEntry entry : timetableEntries) {
                    String cleanEntryDay = entry.getDayOfWeek().replaceAll("\\s+", "").toUpperCase();
                    String cleanCurrentDay = currentDate.getDayOfWeek().name().toUpperCase();
                    if (cleanEntryDay.equals(cleanCurrentDay)) {
                        // Check partial holidays
                        boolean isHoliday = false;
                        if (holidayScope == Holiday.HolidayScope.MORNING && entry.getSession() == TimetableEntry.Session.MORNING) {
                            isHoliday = true;
                        } else if (holidayScope == Holiday.HolidayScope.AFTERNOON && entry.getSession() == TimetableEntry.Session.AFTERNOON) {
                            isHoliday = true;
                        }

                        if (!isHoliday) {
                            Map<String, Object> dateInfo = new HashMap<>();
                            dateInfo.put("date", currentDate.toString());
                            dateInfo.put("dayOfWeek", currentDate.getDayOfWeek().name());
                            dateInfo.put("session", entry.getSession().name());
                            dateInfo.put("slot", c.getSlot()); // Use the course variable 'c' initialized above
                            
                            // Check if attendance already saved for this date
                            Optional<DateBasedAttendance> savedAttendance = dateBasedAttendanceRepository
                                    .findByCourseIdAndAttendanceDate(courseId, currentDate);
                            
                            boolean attended = true;
                            if (savedAttendance.isPresent()) {
                                attended = savedAttendance.get().getAttended();
                            }
                            dateInfo.put("attended", attended);
                            classDates.add(dateInfo);
                        }
                    }
                }
            }
            
            currentDate = currentDate.plusDays(1);
        }

        return classDates;
    }

    /**
     * Save date-based attendance for a course
     */
    public void saveDateBasedAttendance(Long userId, Long courseId, List<Map<String, Object>> attendanceData) {
        Optional<Course> course = courseRepository.findById(courseId);
        if (course.isEmpty() || !course.get().getUserId().equals(userId)) {
            throw new IllegalArgumentException("Course not found or unauthorized");
        }

        Course c = course.get();
        
        // Delete the AttendanceInput record for this course (mark-by-date overrides total/attended)
        Optional<AttendanceInput> existingInput = attendanceInputRepository.findByCourseId(courseId);
        if (existingInput.isPresent()) {
            attendanceInputRepository.delete(existingInput.get());
            log.info("Deleted AttendanceInput for course {} - using date-based attendance now", courseId);
        }

        // Process each attendance entry
        for (Map<String, Object> entry : attendanceData) {
            try {
                String dateStr = (String) entry.get("date");
                Boolean attended = (Boolean) entry.get("attended");
                
                if (dateStr == null || attended == null) {
                    continue;
                }

                LocalDate attendanceDate = LocalDate.parse(dateStr);

                // Check if record already exists
                Optional<DateBasedAttendance> existing = dateBasedAttendanceRepository.findByCourseIdAndAttendanceDate(courseId, attendanceDate);

                DateBasedAttendance attendance;
                if (existing.isPresent()) {
                    attendance = existing.get();
                    attendance.setAttended(attended);
                } else {
                    attendance = new DateBasedAttendance();
                    attendance.setCourse(c);
                    attendance.setAttendanceDate(attendanceDate);
                    attendance.setAttended(attended);
                }

                dateBasedAttendanceRepository.save(attendance);
                log.info("Saved date-based attendance for course {} on {}: {}", courseId, attendanceDate, attended);
            } catch (Exception e) {
                log.error("Error processing attendance entry: {}", e.getMessage());
            }
        }
        
        // Calculate and cache the attendance report for this course
        calculateAndCacheAttendanceReport(c);
        
        log.info("Date-based attendance saved for course {} - overrides total/attended", courseId);
    }

    /**
     * Get all available courses (excluding ones already added by the student)
     */
    public List<Map<String, Object>> getAllAvailableCourses(Long userId) {
        List<Course> allCourses = courseRepository.findAll();
        List<Course> studentCourses = courseRepository.findByUserId(userId);
        List<Long> studentCourseIds = studentCourses.stream().map(Course::getId).toList();

        List<Map<String, Object>> availableCourses = new ArrayList<>();
        for (Course course : allCourses) {
            if (!studentCourseIds.contains(course.getId())) {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", course.getId());
                courseMap.put("courseCode", course.getCourseCode());
                courseMap.put("courseName", course.getCourseName());
                courseMap.put("courseStartDate", course.getCourseStartDate());
                
                // Get timetable info
                List<TimetableEntry> entries = timetableEntryRepository.findByCourseId(course.getId());
                Map<String, Integer> weeklySchedule = new HashMap<>();
                for (TimetableEntry entry : entries) {
                    weeklySchedule.put(entry.getDayOfWeek(), entry.getClassesCount());
                }
                courseMap.put("weeklySchedule", weeklySchedule);
                
                availableCourses.add(courseMap);
            }
        }
        return availableCourses;
    }

    /**
     * Search courses by code or name (excluding ones already added by the student)
     */
    public List<Map<String, Object>> searchCourses(String query, Long userId) {
        String lowerQuery = query.toLowerCase();
        return getAllAvailableCourses(userId).stream()
                .filter(course -> 
                    course.get("courseCode").toString().toLowerCase().contains(lowerQuery) ||
                    course.get("courseName").toString().toLowerCase().contains(lowerQuery)
                )
                .toList();
    }

    /**
     * Add an existing course to student's profile
     */
    public void addExistingCourse(Long userId, Long courseId, List<Map<String, Object>> weeklySchedule, String courseStartDate) {
        Optional<Course> existingCourse = courseRepository.findById(courseId);
        if (existingCourse.isEmpty()) {
            throw new IllegalArgumentException("Course not found");
        }

        Course originalCourse = existingCourse.get();
        
        // Check if student already has this course
        Optional<Course> studentCourse = courseRepository
            .findByUserIdAndCourseCatalog_CourseCodeIgnoreCase(userId, originalCourse.getCourseCode());
        if (studentCourse.isPresent()) {
            throw new IllegalArgumentException("You already have this course");
        }

        // Create a new course entry for the student (with custom start date)
        Course newCourse = new Course();
        newCourse.setUserId(userId);
        newCourse.setCourseCode(originalCourse.getCourseCode());
        newCourse.setCourseName(originalCourse.getCourseName());
        newCourse.setCourseCatalog(originalCourse.getCourseCatalog() != null
            ? originalCourse.getCourseCatalog()
            : resolveOrCreateCourseCatalog(originalCourse.getCourseCode(), originalCourse.getCourseName()));
        newCourse.setSlot(originalCourse.getSlot()); // Copy slot
        
        if (courseStartDate != null && !courseStartDate.isEmpty()) {
            newCourse.setCourseStartDate(LocalDate.parse(courseStartDate));
        }
        
        newCourse = courseRepository.save(newCourse);

        // Save timetable entries
        if (weeklySchedule == null || weeklySchedule.isEmpty()) {
            // If no schedule provided, copy from original course
            List<TimetableEntry> originalEntries = timetableEntryRepository.findByCourseId(courseId);
            for (TimetableEntry entry : originalEntries) {
                TimetableEntry newEntry = new TimetableEntry();
                newEntry.setCourse(newCourse);
                newEntry.setDayOfWeek(entry.getDayOfWeek());
                newEntry.setSession(entry.getSession());
                newEntry.setClassesCount(entry.getClassesCount());
                timetableEntryRepository.save(newEntry);
            }
        } else {
            // Save custom schedule
            for (Map<String, Object> item : weeklySchedule) {
                TimetableEntry timetableEntry = new TimetableEntry();
                timetableEntry.setCourse(newCourse);
                timetableEntry.setDayOfWeek(item.get("dayOfWeek").toString());
                
                Object sessionObj = item.get("session");
                if (sessionObj != null) {
                    timetableEntry.setSession(TimetableEntry.Session.valueOf(sessionObj.toString()));
                } else {
                    timetableEntry.setSession(TimetableEntry.Session.MORNING);
                }
                
                Object countObj = item.get("classesCount");
                timetableEntry.setClassesCount(countObj != null ? Integer.parseInt(countObj.toString()) : 1);
                
                timetableEntryRepository.save(timetableEntry);
            }
        }

        log.info("Added existing course {} for student {}", courseId, userId);
    }
}
