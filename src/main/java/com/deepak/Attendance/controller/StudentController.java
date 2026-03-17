package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.AttendanceInputRequest;
import com.deepak.Attendance.dto.AttendanceReportDTO;
import com.deepak.Attendance.dto.StudentHolidayRequestDTO;
import com.deepak.Attendance.dto.TimetableConfirmRequest;
import com.deepak.Attendance.dto.TimetableEntryDTO;
import com.deepak.Attendance.entity.User;
import com.deepak.Attendance.repository.UserRepository;
import com.deepak.Attendance.security.AuthUserResolver;
import com.deepak.Attendance.security.JwtTokenProvider;
import com.deepak.Attendance.service.StudentHolidayService;
import com.deepak.Attendance.service.StudentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private StudentHolidayService studentHolidayService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthUserResolver authUserResolver;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Confirm and save timetable after student edits
     * POST /api/student/timetable/confirm
     */
    @PostMapping("/timetable/confirm")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> confirmTimetable(@RequestBody List<TimetableConfirmRequest> confirmedData,
                                              @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);

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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);

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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            List<TimetableEntryDTO> courses = studentService.getStudentTimetableDTOs(userId);
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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            
            // Check if any report is stale for this user
            List<com.deepak.Attendance.entity.Course> courses = studentService.getStudentCourses(userId);
            boolean isAnyStale = false;
            for (com.deepak.Attendance.entity.Course c : courses) {
                if (studentService.isReportStale(c.getId())) {
                    isAnyStale = true;
                    break;
                }
            }
            
            List<AttendanceReportDTO> reports = studentService.getAttendanceReport(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("reports", reports);
            response.put("count", reports.size());
            response.put("isStale", isAnyStale);
            response.put("generatedAt", java.time.LocalDateTime.now());
            response.put("totalCourses", courses.size());

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
     * Refresh attendance report for all courses
     * POST /api/student/attendance-report/refresh
     */
    @PostMapping("/attendance-report/refresh")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> refreshAttendanceReport(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            studentService.refreshAttendanceReports(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Attendance reports refreshed successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error refreshing attendance report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to refresh attendance report");
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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            
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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            
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
     * Auto-generate "Present" attendance for a course since its start date
     * POST /api/student/courses/{courseId}/auto-generate-attendance
     */
    @PostMapping("/courses/{courseId}/auto-generate-attendance")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> autoGenerateAttendance(@PathVariable Long courseId,
                                                   @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            int count = studentService.autoGeneratePresentAttendance(userId, courseId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Auto-generation successful");
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        } catch (Exception e) {
            log.error("Error auto-generating attendance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Auto-generation failed");
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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
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
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            
            if (request.get("courseId") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "courseId is required"));
            }
            
            Long courseId = Long.parseLong(request.get("courseId").toString());
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> weeklySchedule = (List<Map<String, Object>>) request.get("weeklySchedule");
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
     * Get student profile
     * GET /api/student/profile
     */
    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            Optional<User> user = userRepository.findById(userId);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User userData = user.get();
            Map<String, Object> response = new HashMap<>();
            response.put("id", userData.getId());
            response.put("username", userData.getUsername());
            response.put("email", userData.getEmail());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error fetching profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch profile");
                    }});
        }
    }

    /**
     * Update student profile
     * POST /api/student/profile/update
     */
    @PostMapping("/profile/update")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> updateData,
                                           @RequestHeader("Authorization") String authHeader) {
        try {
            // Manual token extraction to get username before update
            String token = authHeader.substring(7);
            String currentUsernameFromToken = jwtTokenProvider.extractUsername(token);
            
            Optional<User> user = userRepository.findByUsername(currentUsernameFromToken);

            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HashMap<String, String>() {{
                            put("error", "User not found");
                        }});
            }

            User userData = user.get();
            Long userId = userData.getId();
            String newUsername = updateData.get("username");
            String newEmail = updateData.get("email");
            String currentPassword = updateData.get("currentPassword");
            String newPassword = updateData.get("newPassword");

            // Validate required fields
            if (newUsername == null || newUsername.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, String>() {{
                            put("error", "Username is required");
                        }});
            }

            if (newEmail == null || newEmail.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, String>() {{
                            put("error", "Email is required");
                        }});
            }

            // Check if username already exists (and it's not the same user)
            Optional<User> existingUser = userRepository.findByUsername(newUsername);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, String>() {{
                            put("error", "Username already exists");
                        }});
            }

            // Check if email already exists (and it's not the same user)
            Optional<User> existingEmailUser = userRepository.findByEmail(newEmail);
            if (existingEmailUser.isPresent() && !existingEmailUser.get().getId().equals(userId)) {
                return ResponseEntity.badRequest()
                        .body(new HashMap<String, String>() {{
                            put("error", "Email already exists");
                        }});
            }

            userData.setUsername(newUsername);
            userData.setEmail(newEmail);

            // If password change is requested, update it
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                if (currentPassword == null || currentPassword.trim().isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(new HashMap<String, String>() {{
                                put("error", "Current password is required to change password");
                            }});
                }

                // Verify current password
                if (!passwordEncoder.matches(currentPassword, userData.getPassword())) {
                    return ResponseEntity.badRequest()
                            .body(new HashMap<String, String>() {{
                                put("error", "Current password is incorrect");
                            }});
                }

                // Encode and update the password
                userData.setPassword(passwordEncoder.encode(newPassword));
            }

            userRepository.save(userData);

            // Generate new token if username was changed
            String newToken = null;
            if (!currentUsernameFromToken.equals(userData.getUsername())) {
                newToken = jwtTokenProvider.generateTokenForUsername(userData.getUsername());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Profile updated successfully");
            response.put("username", userData.getUsername());
            response.put("email", userData.getEmail());
            if (newToken != null) {
                response.put("token", newToken);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error updating profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to update profile: " + e.getMessage());
                    }});
        }
    }

    /**
     * Get student's holiday requests
     * GET /api/student/holiday-requests
     */
    @GetMapping("/holiday-requests")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> getHolidayRequests(@RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            List<StudentHolidayRequestDTO> myRequests = studentHolidayService.getRequestsForStudent(userId);
            List<StudentHolidayRequestDTO> otherRequests = studentHolidayService.getRequestsByOthers(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("myRequests", myRequests);
            response.put("otherRequests", otherRequests);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching holiday requests", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", "Failed to fetch holiday requests");
                    }});
        }
    }

    /**
     * Create a holiday request
     * POST /api/student/holiday-requests
     */
    @PostMapping("/holiday-requests")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> createHolidayRequest(@RequestBody StudentHolidayRequestDTO dto,
                                                 @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            StudentHolidayRequestDTO saved = studentHolidayService.createRequest(userId, dto);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            log.error("Error creating holiday request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

    /**
     * Update a holiday request
     * PUT /api/student/holiday-requests/{requestId}
     */
    @PutMapping("/holiday-requests/{requestId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> updateHolidayRequest(@PathVariable Long requestId,
                                                 @RequestBody StudentHolidayRequestDTO dto,
                                                 @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            StudentHolidayRequestDTO updated = studentHolidayService.updateRequest(userId, requestId, dto);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error updating holiday request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

    /**
     * Delete a holiday request
     * DELETE /api/student/holiday-requests/{requestId}
     */
    @DeleteMapping("/holiday-requests/{requestId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> deleteHolidayRequest(@PathVariable Long requestId,
                                                 @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            studentHolidayService.deleteRequest(userId, requestId);
            return ResponseEntity.ok(new HashMap<String, String>() {{
                put("message", "Request deleted successfully");
            }});
        } catch (Exception e) {
            log.error("Error deleting holiday request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<String, String>() {{
                        put("error", e.getMessage());
                    }});
        }
    }

}
