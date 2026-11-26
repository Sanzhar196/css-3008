package org.narxoz.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Security exceptions thrown inside a controller or a service. Runs before ApiErrors. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthErrors {

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> badLogin(HttpServletRequest req) {
        return ResponseEntity.status(401)
                .body(new ApiError("UNAUTHENTICATED", "bad username or password", Correlation.of(req)));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(HttpServletRequest req) {
        return ResponseEntity.status(403)
                .body(new ApiError("FORBIDDEN", "not allowed for your role", Correlation.of(req)));
    }
}
