package org.narxoz.handler;

import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.NonNull;

public record LoginRequest(
        @NotBlank(message = "must not be blank") String username,
        @NotBlank(message = "must not be blank") String password) {

    @Override
    @NonNull
    public String toString() {
        return "LoginRequest[username=" + username + ", password=***]";
    }
}
