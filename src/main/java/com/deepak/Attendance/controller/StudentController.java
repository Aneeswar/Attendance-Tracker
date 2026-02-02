package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.AttendanceInputRequest;
import com.deepak.Attendance.dto.AttendanceReportDTO;
import com.deepak.Attendance.dto.TimetableConfirmRequest;
import com.deepak.Attendance.dto.TimetableEntryDTO;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.security.JwtTokenProvider;
import com.deepak.Attendance.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    /**
     * Confirm and save timetable after student edits
     * POST /api/student/timetable/confirm
     */
    @PostMapping("/timetable/confirm")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> confirmTimetable(@RequestBody List<TimetableConfirmRequest> confirmedData,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);

            if (confirmedData == null || confirmedData.isEmpty()) {
                return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                    put("error", "No timetable data provided");
                }});
            }

            // Save timetable with course start dates
            studentService.confirmTimetableWithDates(userId, confirmedData);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Timetable confirmed and saved successfully");
            response.put("coursesCount", String.valueOf(confirmedData.size()));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error confirming timetable", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to confirm timetable: " + e.getMessage());
                    }});
        }
    }

    /**
     * Get student's current timetable
     * GET /api/student/timetable
     */
    @GetMapping("/timetable")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getTimetable(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            List<TimetableEntryDTO> timetables = studentService.getStudentTimetable(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("timetables", timetables);
            response.put("count", timetables.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching timetable", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch timetable");
                    }});
        }
    }

    /**
     * Submit attendance for a course
     * POST /api/student/attendance
     */
    @PostMapping("/attendance")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> submitAttendance(@RequestBody AttendanceInputRequest request,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);

            if (request.getCourseCode() == null || request.getCourseCode().isEmpty()) {
                return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                    put("error", "Course code is required");
                }});
            }

            if (request.getTotalClassesConducted() == null || request.getClassesAttended() == null) {
                return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                    put("error", "Total classes and attended classes are required");
                }});
            }

            if (request.getClassesAttended() > request.getTotalClassesConducted()) {
                return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                    put("error", "Attended classes cannot exceed total classes");
                }});
            }

            // Save attendance
            studentService.saveAttendanceInput(userId, request.getCourseCode(),
                    request.getTotalClassesConducted(), request.getClassesAttended());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Attendance recorded successfully");
            response.put("courseCode", request.getCourseCode());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        } catch (Exception e) {
            log.error("Error submitting attendance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to submit attendance");
                    }});
        }
    }

    /**
     * Get all courses for the student with their weekly schedules
     * GET /api/student/courses
     */
    @GetMapping("/courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getCourses(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            List<TimetableEntryDTO> courses = studentService.getStudentCourses(userId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            log.error("Error fetching courses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch courses");
                    }});
        }
    }

    /**
     * Get attendance report for all courses
     * GET /api/student/attendance-report
     */
    @GetMapping("/attendance-report")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getAttendanceReport(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            List<AttendanceReportDTO> reports = studentService.getAttendanceReport(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("reports", reports);
            response.put("count", reports.size());
            response.put("generatedAt", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching attendance report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch attendance report");
                    }});
        }
    }

    /**
     * Get holidays list (accessible by students)
     * GET /api/student/holidays
     */
    @GetMapping("/holidays")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getHolidays() {
        try {
            List<com.deepak.Attendance.dto.HolidayDTO> holidays = studentService.getAllHolidays();
            return ResponseEntity.ok(holidays);
        } catch (Exception e) {
            log.error("Error fetching holidays", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch holidays");
                    }});
        }
    }

    /**
     * Update a course
     * PUT /api/student/courses/{courseId}
     */
    @PutMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateCourse(@PathVariable Long courseId,
                                         @RequestBody TimetableConfirmRequest courseData,
                                         @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            
            // Use confirmTimetableWithDates to save course start date
            studentService.confirmTimetableWithDates(userId, List.of(courseData));
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Course updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating course", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to update course");
                    }});
        }
    }

    /**
     * Get class dates for a course (from start date until today, excluding holidays)
     * GET /api/student/courses/{courseId}/class-dates
     */
    @GetMapping("/courses/{courseId}/class-dates")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getClassDates(@PathVariable Long courseId,
                                          @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            List<Map<String, Object>> classDates = studentService.getClassDatesForCourse(userId, courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("dates", classDates);
            response.put("count", classDates.size());
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        } catch (Exception e) {
            log.error("Error fetching class dates", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch class dates");
                    }});
        }
    }

    /**
     * Save date-based attendance for a course
     * POST /api/student/courses/{courseId}/date-attendance
     */
    @PostMapping("/courses/{courseId}/date-attendance")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> saveDateBasedAttendance(@PathVariable Long courseId,
                                                    @RequestBody Map<String, Object> attendanceData,
                                                    @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> attendance = (List<Map<String, Object>>) attendanceData.get("attendance");
            
            if (attendance == null || attendance.isEmpty()) {
                return ResponseEntity.badRequest().body(new HashMap<String, String>() {{
                    put("error", "No attendance data provided");
                }});
            }
            
            studentService.saveDateBasedAttendance(userId, courseId, attendance);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Attendance saved successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        } catch (Exception e) {
            log.error("Error saving date-based attendance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to save attendance");
                    }});
        }
    }

    /**
     * Delete a course
     * DELETE /api/student/courses/{courseId}
     */
    @DeleteMapping("/courses/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long courseId,
                                         @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            studentService.deleteCourse(userId, courseId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Course deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting course", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to delete course");
                    }});
        }
    }

    /**
     * Get all available courses (for adding existing course)
     * GET /api/student/available-courses
     */
    @GetMapping("/available-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getAvailableCourses(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            List<Map<String, Object>> courses = studentService.getAllAvailableCourses(userId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            log.error("Error fetching available courses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch available courses");
                    }});
        }
    }

    /**
     * Search courses by code or name
     * GET /api/student/search-courses?q=searchTerm
     */
    @GetMapping("/search-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> searchCourses(@RequestParam String q, @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            List<Map<String, Object>> courses = studentService.searchCourses(q, userId);
            return ResponseEntity.ok(courses);
        } catch (Exception e) {
            log.error("Error searching courses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to search courses");
                    }});
        }
    }

    /**
     * Add student to existing course
     * POST /api/student/add-existing-course
     */
    @PostMapping("/add-existing-course")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> addExistingCourse(@RequestBody Map<String, Object> request,
                                               @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = extractUserIdFromToken(authHeader);
            Long courseId = Long.parseLong(request.get("courseId").toString());
            @SuppressWarnings("unchecked")
            Map<String, Integer> weeklySchedule = (Map<String, Integer>) request.get("weeklySchedule");
            String courseStartDate = (String) request.get("courseStartDate");

            studentService.addExistingCourse(userId, courseId, weeklySchedule, courseStartDate);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Course added successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error adding existing course", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

    /**
     * Extract user ID from JWT token in Authorization header
     */
    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String username = jwtTokenProvider.extractUsername(token);
            Optional<User> user = userRepository.findByUsername(username);
            if (user.isPresent()) {
                return user.get().getId();
            }
            throw new IllegalArgumentException("User not found");
        }
        throw new IllegalArgumentException("Invalid authorization header");
    }
}
