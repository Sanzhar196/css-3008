package org.narxoz.handler;

public record ApiError(String code, String message, String correlationId) {
}
