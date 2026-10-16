package org.narxoz.domain;

import java.util.UUID;

public record RequestId(String value) {
    public RequestId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ID must not be blank");
        }
    }

    public static RequestId newId() {
        return new RequestId(UUID.randomUUID().toString());
    }
}
