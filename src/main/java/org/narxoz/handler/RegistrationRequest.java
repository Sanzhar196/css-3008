package org.narxoz.handler;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.lang.NonNull;

public record RegistrationRequest(
        @NotBlank(message = "must not be blank")
        @Pattern(regexp = "[a-zA-Z0-9][a-zA-Z0-9_.-]{2,31}",
                message = "must be 3–32 letters, digits, dots, underscores or hyphens and start with a letter or digit")
        String username,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotBlank(message = "must not be blank")
        @Size(max = 72, message = "must not exceed 72 UTF-8 bytes")
        String password) {

    @Override
    @NonNull
    public String toString() {
        return "RegistrationRequest[username=" + username + ", password=***]";
    }
}
