package com.campusconnect.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/** The single error shape returned by every endpoint. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, String message, List<FieldError> details, String path, Instant timestamp) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(String error, String message, String path) {
        return new ErrorResponse(error, message, null, path, Instant.now());
    }
}
