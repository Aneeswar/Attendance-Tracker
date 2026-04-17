package com.deepak.Attendance.service;

import com.deepak.Attendance.config.properties.OcrApiProperties;
import com.deepak.Attendance.dto.ManualCourseImportPreviewRequest;
import com.deepak.Attendance.dto.ManualCourseImportRowRequest;
import com.deepak.Attendance.dto.ocr.OcrExtractResponseDTO;
import com.deepak.Attendance.dto.ocr.OcrExtractRowDTO;
import com.deepak.Attendance.exception.OcrServiceException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;

@Service
public class ExternalOcrClientService {

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
            DateTimeFormatter.ISO_LOCAL_DATE,
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("d-MMM-uuuu").toFormatter(Locale.ENGLISH),
            new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("dd-MMM-uuuu").toFormatter(Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d/M/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d-M-uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ENGLISH)
    };

    private final WebClient ocrWebClient;
    private final String ocrBaseUrl;
    private final String ocrApiKey;
    private final long timeoutMs;

    public ExternalOcrClientService(WebClient ocrWebClient, OcrApiProperties ocrApiProperties) {
        this.ocrWebClient = ocrWebClient;
        this.ocrBaseUrl = ocrApiProperties.getBaseUrl();

        String envApiKey = System.getenv("OCR_API_KEY");
        this.ocrApiKey = (envApiKey != null && !envApiKey.isBlank()) ? envApiKey : ocrApiProperties.getKey();
        this.timeoutMs = ocrApiProperties.getTimeoutMs();
    }

    public ManualCourseImportPreviewRequest extractAndMap(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new OcrServiceException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        if (ocrApiKey == null || ocrApiKey.isBlank()) {
            throw new OcrServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "OCR API key is not configured");
        }

        OcrExtractResponseDTO response = invokeExtract(image);
        List<ManualCourseImportRowRequest> rows = mapRows(response == null ? null : response.getData());

        if (rows.isEmpty()) {
            throw new OcrServiceException(HttpStatus.BAD_REQUEST, "OCR service returned no importable course rows");
        }

        return new ManualCourseImportPreviewRequest(rows);
    }

    private OcrExtractResponseDTO invokeExtract(MultipartFile image) {
        MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
        try {
            bodyBuilder.part("image", new MultipartInputStreamFileResource(image.getBytes(), image.getOriginalFilename()))
                    .contentType(resolveContentType(image));
        } catch (IOException e) {
            throw new OcrServiceException(HttpStatus.BAD_REQUEST, "Unable to read uploaded image");
        }

        try {
            return ocrWebClient.post()
                    .uri(ocrBaseUrl + "/api/extract")
                    .header("X-API-Key", ocrApiKey)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
                    .retrieve()
                    .bodyToMono(OcrExtractResponseDTO.class)
                    .timeout(java.time.Duration.ofMillis(timeoutMs))
                    .block();
        } catch (WebClientResponseException e) {
            throw mapStatusError(e.getStatusCode(), e.getResponseBodyAsString());
        } catch (WebClientRequestException e) {
            throw new OcrServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to reach OCR service");
        } catch (RuntimeException e) {
            if (hasTimeoutCause(e)) {
                throw new OcrServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "OCR service timed out");
            }
            throw new OcrServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected OCR service failure");
        }
    }

    private OcrServiceException mapStatusError(org.springframework.http.HttpStatusCode statusCode, String responseBody) {
        int code = statusCode == null ? 500 : statusCode.value();
        String bodySnippet = summarizeBody(responseBody);
        if (code == 400) {
            return new OcrServiceException(HttpStatus.BAD_REQUEST, "OCR service rejected the image" + bodySnippet);
        }
        if (code == 401) {
            return new OcrServiceException(HttpStatus.UNAUTHORIZED, "OCR service authentication failed");
        }
        if (code == 429) {
            return new OcrServiceException(HttpStatus.TOO_MANY_REQUESTS, "OCR service rate limit exceeded");
        }
        return new OcrServiceException(HttpStatus.INTERNAL_SERVER_ERROR, "OCR service failed" + bodySnippet);
    }

    private String summarizeBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        if (compact.length() > 120) {
            compact = compact.substring(0, 120) + "...";
        }
        return ": " + compact;
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private List<ManualCourseImportRowRequest> mapRows(List<OcrExtractRowDTO> data) {
        List<ManualCourseImportRowRequest> rows = new ArrayList<>();
        if (data == null) {
            return rows;
        }

        for (OcrExtractRowDTO item : data) {
            if (item == null) {
                continue;
            }

            String courseCode = normalizeUpper(item.getCourseCode());
            String courseName = normalizeText(item.getCourseName());
            String slotString = normalizeSlot(item.getSlot());
            LocalDate registeredDate = parseDate(item.getRegisteredDate());
            String courseType = inferCourseType(slotString, courseName);

            if (courseCode == null && courseName == null && slotString == null && registeredDate == null) {
                continue;
            }

            rows.add(new ManualCourseImportRowRequest(courseCode, courseName, slotString, registeredDate, courseType));
        }

        return rows;
    }

    private String inferCourseType(String slotString, String courseName) {
        String normalizedName = normalizeUpper(courseName);
        if (normalizedName != null && normalizedName.contains("LAB")) {
            return "LAB";
        }
        if (slotString != null && slotString.matches("^L\\d{1,2}(\\+L\\d{1,2})*$")) {
            return "LAB";
        }
        return "THEORY";
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(raw.trim(), formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        return null;
    }

    private String normalizeUpper(String value) {
        String normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeSlot(String value) {
        String normalized = normalizeUpper(value);
        return normalized == null ? null : normalized.replaceAll("\\s*\\+\\s*", "+");
    }

    private MediaType resolveContentType(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static final class MultipartInputStreamFileResource extends ByteArrayResource {

        private final String filename;

        private MultipartInputStreamFileResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename == null || filename.isBlank() ? "upload-image" : filename;
        }
    }
}
