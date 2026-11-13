package org.narxoz.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.narxoz.domain.DuplicateRequest;
import org.narxoz.domain.MissingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.UUID;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger log = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex, HttpServletRequest req) {
        FieldError error = ex.getBindingResult().getFieldError();
        String message = error == null
                ? "request is not valid"
                : words(error.getField()) + " " + error.getDefaultMessage();
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION", message, correlation(req)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> badId(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION", "id must be a UUID", correlation(req)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> notJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION", "body must be JSON", correlation(req)));
    }

    @ExceptionHandler(MissingRequest.class)
    ResponseEntity<ApiError> missing(MissingRequest ex, HttpServletRequest req) {
        return ResponseEntity.status(404)
                .body(new ApiError("NOT_FOUND", ex.getMessage(), correlation(req)));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(NoResourceFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(404)
                .body(new ApiError("NOT_FOUND", "not found", correlation(req)));
    }

    @ExceptionHandler(DuplicateRequest.class)
    ResponseEntity<ApiError> duplicate(DuplicateRequest ex, HttpServletRequest req) {
        return ResponseEntity.status(409)
                .body(new ApiError("DUPLICATE", ex.getMessage(), correlation(req)));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupported(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return ResponseEntity.status(415)
                .body(new ApiError("UNSUPPORTED", "unsupported content type", correlation(req)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        String id = correlation(req);
        log.error("unexpected error, correlationId={}", id, ex);
        return ResponseEntity.status(500)
                .body(new ApiError("INTERNAL", "unexpected error", id));
    }

    private String correlation(HttpServletRequest req) {
        String id = req.getHeader("X-Correlation-Id");
        if (id == null || id.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return id;
    }

    /** businessKey becomes "business key", title stays "title". */
    private static String words(String field) {
        return field.replaceAll("([A-Z])", " $1").toLowerCase();
    }
}
