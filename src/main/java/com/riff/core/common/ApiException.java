package com.riff.core.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Application-level exception carrying an HTTP status, a human-readable message, and a
 * machine-readable error code. Thrown by services/controllers and translated into an
 * RFC 7807 problem response by {@link GlobalExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;

    public ApiException(int status, String message, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public ApiException(int status, String message) {
        this(status, message, null);
    }

    public ApiException(HttpStatus httpStatus, String message) {
        this(httpStatus.value(), message, null);
    }

    public ApiException(HttpStatusCode httpStatus, String message) {
        this(httpStatus.value(), message, null);
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
