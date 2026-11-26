package org.narxoz.handler;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.narxoz.config.RegistrationUsers;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@Profile(RegistrationUsers.PROFILE)
public class RegistrationController {
    private final RegistrationUsers users;

    public RegistrationController(RegistrationUsers users) {
        this.users = users;
    }

    @PostMapping("/api/v1/auth/register")
    @Operation(summary = "Create a CLERK account")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(schema = @Schema(implementation = RegistrationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid username or password",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Username unavailable",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegistrationRequest body, HttpServletRequest request) {
        String password = body.password();
        if (password.codePointCount(0, password.length()) < 8) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("VALIDATION", "password must contain at least 8 characters",
                            Correlation.of(request)));
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("VALIDATION", "password must not exceed 72 UTF-8 bytes",
                            Correlation.of(request)));
        }

        var username = users.register(body.username(), password);
        if (username.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiError("DUPLICATE", "username is not available", Correlation.of(request)));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistrationResponse(username.get(), RegistrationUsers.ROLE));
    }

    public record RegistrationResponse(String username, String role) {}
}
