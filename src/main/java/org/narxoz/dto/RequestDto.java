package org.narxoz.dto;

import org.narxoz.domain.RequestStatus;

public record RequestDto(String id, String status) {
    public RequestStatus toStatus() {
        return RequestStatus.valueOf(status.trim().toUpperCase());
    }
}