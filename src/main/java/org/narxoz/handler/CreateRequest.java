package org.narxoz.handler;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRequest(
        @NotBlank String businessKey,
        @NotBlank @Size(max = 120) String title) {
}
