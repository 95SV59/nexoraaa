package com.nexora.exception;

import java.time.Instant;
import java.util.Map;

public class ErrorResponse {
    private final int status;
    private final String error;
    private final String message;
    private final Instant timestamp;
    private final Map<String, String> fieldErrors;

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, null);
    }

    public ErrorResponse(int status, String error, String message, Map<String, String> fieldErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
        this.fieldErrors = fieldErrors;
    }

    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
}
