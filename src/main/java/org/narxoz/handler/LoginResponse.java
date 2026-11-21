package org.narxoz.handler;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
