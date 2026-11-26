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
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
                .body(new ApiError("VALIDATION", message, Correlation.of(req)));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> badId(HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION", "id must be a UUID", Correlation.of(req)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> notJson(HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(new ApiError("VALIDATION", "body must be JSON", Correlation.of(req)));
    }

    @ExceptionHandler(MissingRequest.class)
    ResponseEntity<ApiError> missing(MissingRequest ex, HttpServletRequest req) {
        return ResponseEntity.status(404)
                .body(new ApiError("NOT_FOUND", ex.getMessage(), Correlation.of(req)));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    ResponseEntity<ApiError> noResource(HttpServletRequest req) {
        String apiRoot = req.getContextPath() + "/api/v1";
        String path = req.getRequestURI();
        if (path.equals(apiRoot) || path.startsWith(apiRoot + "/")) {
            return ResponseEntity.status(403)
                    .body(new ApiError("FORBIDDEN", "not allowed for your role", Correlation.of(req)));
        }
        return ResponseEntity.status(404)
                .body(new ApiError("NOT_FOUND", "not found", Correlation.of(req)));
    }

    @ExceptionHandler(DuplicateRequest.class)
    ResponseEntity<ApiError> duplicate(DuplicateRequest ex, HttpServletRequest req) {
        return ResponseEntity.status(409)
                .body(new ApiError("DUPLICATE", ex.getMessage(), Correlation.of(req)));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> invalidTransition(IllegalStateException ex, HttpServletRequest req) {
        return ResponseEntity.status(409)
                .body(new ApiError("INVALID_TRANSITION", ex.getMessage(), Correlation.of(req)));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> unsupported(HttpServletRequest req) {
        return ResponseEntity.status(415)
                .body(new ApiError("UNSUPPORTED", "unsupported content type", Correlation.of(req)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest req) {
        String id = Correlation.of(req);
        log.error("unexpected error, correlationId={}", id, ex);
        return ResponseEntity.status(500)
                .body(new ApiError("INTERNAL", "unexpected error", id));
    }

    /** businessKey becomes "business key", title stays "title". */
    private static String words(String field) {
        return field.replaceAll("([A-Z])", " $1").toLowerCase();
    }
}
