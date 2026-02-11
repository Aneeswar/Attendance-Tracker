package com.deepak.Attendance.service;

import com.deepak.Attendance.dto.AcademicCalendarDTO;
import com.deepak.Attendance.dto.AttendanceReportDTO;
import com.deepak.Attendance.dto.TimetableConfirmRequest;
import com.deepak.Attendance.dto.TimetableEntryDTO;
import com.deepak.Attendance.entity.*;
import com.deepak.Attendance.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.upload.dir:uploads/timetables}")
    private String uploadDir;

    /**
     * Confirm and save timetable (add or update courses)
     */
    public void confirmTimetable(Long userId, List<TimetableEntryDTO> confirmedData) {
        // Save new courses and timetable entries (add/update, don't delete)
        for (TimetableEntryDTO dto : confirmedData) {
            // Check if course already exists
            Optional<Course> existing = courseRepository
                    .findByUserIdAndCourseCode(userId, dto.getCourseCode());

            Course course;
            if (existing.isPresent()) {
                course = existing.get();
                course.setCourseName(dto.getCourseName());
            } else {
                course = new Course();
                course.setUserId(userId);
                course.setCourseCode(dto.getCourseCode());
                course.setCourseName(dto.getCourseName());
                course.setTimetableEntries(new ArrayList<>());
            }

            course = courseRepository.save(course);

            // Delete old timetable entries for this course and recreate them
            List<TimetableEntry> oldEntries = timetableEntryRepository.findByCourseId(course.getId());
            if (!oldEntries.isEmpty()) {
                timetableEntryRepository.deleteAll(oldEntries);
            }

            // Save timetable entries
            for (Map.Entry<String, Integer> entry : dto.getWeeklySchedule().entrySet()) {
                TimetableEntry timetableEntry = new TimetableEntry();
                timetableEntry.setCourse(course);
                timetableEntry.setDayOfWeek(entry.getKey());
                timetableEntry.setClassesCount(entry.getValue());
                timetableEntryRepository.save(timetableEntry);
            }
        }

        log.info("Timetable confirmed and saved for user: {}", userId);
    }

    /**
     * Confirm and save timetable with course start dates
     */
    public void confirmTimetableWithDates(Long userId, List<TimetableConfirmRequest> confirmedData) {
        for (TimetableConfirmRequest req : confirmedData) {
            // Check if course already exists
            Optional<Course> existing = courseRepository
                    .findByUserIdAndCourseCode(userId, req.getCourseCode());

            Course course;
            if (existing.isPresent()) {
                course = existing.get();
                course.setCourseName(req.getCourseName());
                if (req.getCourseStartDate() != null) {
                    course.setCourseStartDate(req.getCourseStartDate());
                }
            } else {
                course = new Course();
                course.setUserId(userId);
                course.setCourseCode(req.getCourseCode());
                course.setCourseName(req.getCourseName());
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
            for (Map.Entry<String, Integer> entry : req.getWeeklySchedule().entrySet()) {
                TimetableEntry timetableEntry = new TimetableEntry();
                timetableEntry.setCourse(course);
                timetableEntry.setDayOfWeek(entry.getKey());
                timetableEntry.setClassesCount(entry.getValue());
                timetableEntryRepository.save(timetableEntry);
            }
        }

        log.info("Timetable confirmed and saved for user: {}", userId);
    }

    /**
     * Save attendance input for a course
     */
    public void saveAttendanceInput(Long userId, String courseCode, 
                                   Integer totalConducted, Integer attended) {
        Optional<Course> course = courseRepository.findByUserIdAndCourseCode(userId, courseCode);
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
            for (int i = 0; i < classDates.size() && i < total; i++) {
                Map<String, Object> dateInfo = classDates.get(i);
                LocalDate date = LocalDate.parse((String) dateInfo.get("date"));
                
                DateBasedAttendance dba = new DateBasedAttendance();
                dba.setCourse(course);
                dba.setAttendanceDate(date);
                dba.setAttended(i < attended); // First 'attended' count = true, rest = false
                
                dateBasedAttendanceRepository.save(dba);
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
            
            reports.add(report);
        }

        return reports;
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
                totalClasses = dateBasedRecords.size();
                attendedClasses = (int) dateBasedRecords.stream().filter(DateBasedAttendance::getAttended).count();
            } else {
                Optional<AttendanceInput> input = attendanceInputRepository.findByCourseId(course.getId());
                if (input.isEmpty()) {
                    return; // No attendance data to cache
                }
                totalClasses = input.get().getTotalClassesConducted();
                attendedClasses = input.get().getClassesAttended();
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

    /**
     * Refresh attendance reports for a specific student's courses
     */
    @Transactional
    public void refreshAttendanceReports(Long userId) {
        log.info("Refreshing attendance reports for user: {}", userId);
        List<Course> courses = courseRepository.findByUserId(userId);
        for (Course course : courses) {
            calculateAndCacheAttendanceReport(course);
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
            Map<String, Integer> weeklySchedule = new LinkedHashMap<>();
            List<TimetableEntry> entries = timetableEntryRepository.findByCourseId(course.getId());

            for (TimetableEntry entry : entries) {
                weeklySchedule.put(entry.getDayOfWeek(), entry.getClassesCount());
            }

            TimetableEntryDTO dto = new TimetableEntryDTO(
                    course.getId(),
                    course.getCourseCode(),
                    course.getCourseName(),
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
    public List<TimetableEntryDTO> getStudentCourses(Long userId) {
        return getStudentTimetable(userId);
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
        Optional<Course> course = courseRepository.findByUserIdAndCourseCode(userId, courseCode);
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
        Set<LocalDate> excludedDates = new HashSet<>();
        
        // Add holidays
        List<com.deepak.Attendance.dto.HolidayDTO> holidayDTOs = holidayService.getAllHolidays();
        holidayDTOs.forEach(h -> excludedDates.add(h.getDate()));
        
        // Add exam periods (CAT-1, CAT-2, FAT)
        Optional<AcademicCalendar> currentCalendar = academicCalendarRepository
                .findBySemesterStartDateLessThanEqualAndExamStartDateGreaterThanEqual(today, today);
        
        if (currentCalendar.isPresent()) {
            AcademicCalendar calendar = currentCalendar.get();
            
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
        }

        // Build a set of days of the week when classes occur (0=Sunday, 1=Monday, etc.)
        Set<Integer> classDays = new HashSet<>();
        Map<String, Integer> classCountMap = new HashMap<>();
        
        for (TimetableEntry entry : timetableEntries) {
            String dayName = entry.getDayOfWeek();
            int dayOfWeek = getDayOfWeekNumber(dayName);
            classDays.add(dayOfWeek);
            classCountMap.put(dayName, entry.getClassesCount());
        }

        // Generate list of class dates
        List<Map<String, Object>> classDates = new ArrayList<>();
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(today)) {
            int dayOfWeek = currentDate.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
            
            // Check if this day has classes scheduled
            if (classDays.contains(dayOfWeek)) {
                // Check if it's not a holiday or exam date
                if (!excludedDates.contains(currentDate)) {
                    Map<String, Object> dateInfo = new HashMap<>();
                    dateInfo.put("date", currentDate.toString());
                    dateInfo.put("dayOfWeek", getDayNameFromNumber(dayOfWeek));
                    
                    // Check if attendance already saved for this date
                    Optional<DateBasedAttendance> savedAttendance = dateBasedAttendanceRepository
                            .findByCourseIdAndAttendanceDate(courseId, currentDate);
                    
                    boolean attended = false;
                    if (savedAttendance.isPresent()) {
                        attended = savedAttendance.get().getAttended();
                    }
                    dateInfo.put("attended", attended);
                    
                    classDates.add(dateInfo);
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
     * Convert day of week string to numeric day (1-7, where 1=Monday, 7=Sunday)
     */
    private int getDayOfWeekNumber(String dayName) {
        return switch (dayName.toUpperCase()) {
            case "MONDAY" -> 1;
            case "TUESDAY" -> 2;
            case "WEDNESDAY" -> 3;
            case "THURSDAY" -> 4;
            case "FRIDAY" -> 5;
            case "SATURDAY" -> 6;
            case "SUNDAY" -> 7;
            default -> -1;
        };
    }

    /**
     * Convert numeric day to day name
     */
    private String getDayNameFromNumber(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 7 -> "Sunday";
            default -> "Unknown";
        };
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
    public void addExistingCourse(Long userId, Long courseId, Map<String, Integer> weeklySchedule, String courseStartDate) {
        Optional<Course> existingCourse = courseRepository.findById(courseId);
        if (existingCourse.isEmpty()) {
            throw new IllegalArgumentException("Course not found");
        }

        Course originalCourse = existingCourse.get();
        
        // Check if student already has this course
        Optional<Course> studentCourse = courseRepository
                .findByUserIdAndCourseCode(userId, originalCourse.getCourseCode());
        if (studentCourse.isPresent()) {
            throw new IllegalArgumentException("You already have this course");
        }

        // Create a new course entry for the student (with custom start date)
        Course newCourse = new Course();
        newCourse.setUserId(userId);
        newCourse.setCourseCode(originalCourse.getCourseCode());
        newCourse.setCourseName(originalCourse.getCourseName());
        
        if (courseStartDate != null && !courseStartDate.isEmpty()) {
            newCourse.setCourseStartDate(LocalDate.parse(courseStartDate));
        }
        
        newCourse = courseRepository.save(newCourse);

        // Save timetable entries
        for (Map.Entry<String, Integer> entry : weeklySchedule.entrySet()) {
            TimetableEntry timetableEntry = new TimetableEntry();
            timetableEntry.setCourse(newCourse);
            timetableEntry.setDayOfWeek(entry.getKey());
            timetableEntry.setClassesCount(entry.getValue());
            timetableEntryRepository.save(timetableEntry);
        }

        log.info("Added existing course {} for student {}", courseId, userId);
    }
}
