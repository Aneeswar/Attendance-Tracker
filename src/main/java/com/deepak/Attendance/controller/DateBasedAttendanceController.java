package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.CourseAttendanceCalendarDTO;
import com.deepak.Attendance.dto.DateBasedAttendanceDTO;
import com.deepak.Attendance.security.JwtTokenProvider;
import com.deepak.Attendance.service.DateBasedAttendanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for date-based attendance tracking
 */
@RestController
@RequestMapping("/api/student/attendance")
public class DateBasedAttendanceController {

    private static final Logger logger = LoggerFactory.getLogger(DateBasedAttendanceController.class);

    @Autowired
    private DateBasedAttendanceService dateBasedAttendanceService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Get attendance calendar for a specific course
     * GET /api/student/attendance/calendar/{courseId}
     */
    @GetMapping("/calendar/{courseId}")
    public ResponseEntity<?> getAttendanceCalendar(
            @PathVariable Long courseId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtTokenProvider.extractUsername(token);

            // Verify user owns this course (optional but recommended for security)
            logger.info("Fetching attendance calendar for course {} by user {}", courseId, username);

            CourseAttendanceCalendarDTO calendar = dateBasedAttendanceService.getCourseAttendanceCalendar(courseId);
            return ResponseEntity.ok(calendar);

        } catch (Exception e) {
            logger.error("Error getting attendance calendar", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to get attendance calendar: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * Update date-based attendance for a course
     * POST /api/student/attendance/update/{courseId}
     * 
     * Body: List of DateBasedAttendanceDTO with date and attended flag
     */
    @PostMapping("/update/{courseId}")
    public ResponseEntity<?> updateAttendance(
            @PathVariable Long courseId,
            @RequestBody List<DateBasedAttendanceDTO> attendanceDates,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtTokenProvider.extractUsername(token);

            logger.info("Updating date-based attendance for course {} by user {}", courseId, username);

            dateBasedAttendanceService.updateDateBasedAttendance(courseId, attendanceDates);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Attendance updated successfully");
            response.put("recordsUpdated", attendanceDates.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error updating attendance", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to update attendance: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
