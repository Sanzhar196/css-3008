package org.narxoz.handler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRequest(
        @NotBlank(message = "must not be blank") String businessKey,
        @NotBlank(message = "must not be blank")
        @Size(max = 120, message = "size must be between 0 and 120") String title) {
}
