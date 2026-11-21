package org.narxoz.handler;

import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

final class Correlation {
    private Correlation() {
    }

    static String of(HttpServletRequest req) {
        String id = req.getHeader("X-Correlation-Id");
        if (id == null || id.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return id;
    }
}
