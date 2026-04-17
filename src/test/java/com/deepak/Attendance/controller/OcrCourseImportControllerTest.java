package com.deepak.Attendance.controller;

import com.deepak.Attendance.dto.ManualCourseImportConfirmResponse;
import com.deepak.Attendance.dto.ManualCourseImportPreviewRequest;
import com.deepak.Attendance.dto.ManualCourseImportPreviewResponse;
import com.deepak.Attendance.exception.OcrServiceException;
import com.deepak.Attendance.security.AuthUserResolver;
import com.deepak.Attendance.service.ExternalOcrClientService;
import com.deepak.Attendance.service.ManualCourseImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcrCourseImportControllerTest {

    @Mock
    private ExternalOcrClientService externalOcrClientService;

    @Mock
    private ManualCourseImportService manualCourseImportService;

    @Mock
    private AuthUserResolver authUserResolver;

    @InjectMocks
    private OcrCourseImportController ocrCourseImportController;

    @Test
    void preview_returnsManualPreviewResponse() {
        MockMultipartFile image = new MockMultipartFile("image", "courses.png", "image/png", "dummy".getBytes());
        ManualCourseImportPreviewRequest request = new ManualCourseImportPreviewRequest();
        ManualCourseImportPreviewResponse responsePayload = new ManualCourseImportPreviewResponse();

        when(authUserResolver.extractUserIdFromToken("Bearer token")).thenReturn(1L);
        when(externalOcrClientService.extractAndMap(image)).thenReturn(request);
        when(manualCourseImportService.preview(1L, request)).thenReturn(responsePayload);

        ResponseEntity<?> response = ocrCourseImportController.preview(image, "Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responsePayload, response.getBody());
        verify(manualCourseImportService).preview(1L, request);
    }

    @Test
    void preview_mapsOcrClientStatusCode() {
        MockMultipartFile image = new MockMultipartFile("image", "courses.png", "image/png", "dummy".getBytes());

        when(authUserResolver.extractUserIdFromToken("Bearer token")).thenReturn(1L);
        when(externalOcrClientService.extractAndMap(image))
                .thenThrow(new OcrServiceException(HttpStatus.TOO_MANY_REQUESTS, "OCR service rate limit exceeded"));

        ResponseEntity<?> response = ocrCourseImportController.preview(image, "Bearer token");

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    }

    @Test
    void confirm_reusesManualConfirmService() {
        ManualCourseImportPreviewRequest request = new ManualCourseImportPreviewRequest();
        ManualCourseImportConfirmResponse payload = new ManualCourseImportConfirmResponse();

        when(authUserResolver.extractUserIdFromToken("Bearer token")).thenReturn(7L);
        when(manualCourseImportService.confirm(7L, request)).thenReturn(payload);

        ResponseEntity<?> response = ocrCourseImportController.confirm(request, "Bearer token");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(payload, response.getBody());
        verify(manualCourseImportService).confirm(7L, request);
    }
}