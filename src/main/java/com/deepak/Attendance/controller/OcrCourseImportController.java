package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.ManualCourseImportConfirmResponse;
import com.deepak.Attendance.dto.ManualCourseImportPreviewRequest;
import com.deepak.Attendance.dto.ManualCourseImportPreviewResponse;
import com.deepak.Attendance.exception.OcrServiceException;
import com.deepak.Attendance.security.AuthUserResolver;
import com.deepak.Attendance.service.ExternalOcrClientService;
import com.deepak.Attendance.service.ManualCourseImportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/courses/import/ocr")
public class OcrCourseImportController {

    private final ExternalOcrClientService externalOcrClientService;
    private final ManualCourseImportService manualCourseImportService;
    private final AuthUserResolver authUserResolver;

    public OcrCourseImportController(ExternalOcrClientService externalOcrClientService,
                                     ManualCourseImportService manualCourseImportService,
                                     AuthUserResolver authUserResolver) {
        this.externalOcrClientService = externalOcrClientService;
        this.manualCourseImportService = manualCourseImportService;
        this.authUserResolver = authUserResolver;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> preview(@RequestParam("image") MultipartFile image,
                                     @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = authUserResolver.extractUserIdFromToken(authHeader);
            ManualCourseImportPreviewRequest request = externalOcrClientService.extractAndMap(image);
            ManualCourseImportPreviewResponse response = manualCourseImportService.preview(userId, request);
            return ResponseEntity.ok(response);
        } catch (OcrServiceException e) {
            return ResponseEntity.status(e.getStatus()).body(error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error generating OCR import preview", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to generate OCR preview"));
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
            log.error("Error confirming OCR import", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error("Failed to confirm OCR import"));
        }
    }

    private Map<String, String> error(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }
}