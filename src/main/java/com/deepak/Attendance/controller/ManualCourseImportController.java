package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.ManualCourseImportConfirmResponse;
import com.deepak.Attendance.dto.ManualCourseImportPreviewRequest;
import com.deepak.Attendance.dto.ManualCourseImportPreviewResponse;
import com.deepak.Attendance.security.AuthUserResolver;
import com.deepak.Attendance.service.ManualCourseImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/student/manual-course-import")
public class ManualCourseImportController {

    private final ManualCourseImportService manualCourseImportService;
    private final AuthUserResolver authUserResolver;

    public ManualCourseImportController(ManualCourseImportService manualCourseImportService,
                                        AuthUserResolver authUserResolver) {
        this.manualCourseImportService = manualCourseImportService;
        this.authUserResolver = authUserResolver;
    }

    @PostMapping("/preview")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> preview(@RequestBody ManualCourseImportPreviewRequest request,
                                     @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            ManualCourseImportPreviewResponse response = manualCourseImportService.preview(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating manual import preview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to generate preview"));
        }
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> confirm(@RequestBody ManualCourseImportPreviewRequest request,
                                     @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            ManualCourseImportConfirmResponse response = manualCourseImportService.confirm(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error confirming manual course import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to import courses"));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}
