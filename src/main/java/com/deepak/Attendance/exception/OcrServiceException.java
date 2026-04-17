package com.deepak.Attendance.exception;

import org.springframework.http.HttpStatus;

public class OcrServiceException extends RuntimeException {

    private final HttpStatus status;

    public OcrServiceException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
