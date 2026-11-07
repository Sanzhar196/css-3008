package org.narxoz.handler;

import org.narxoz.domain.RequestView;

import java.util.UUID;

public record RequestResponse(UUID id, String businessKey, String status, String title) {

    public static RequestResponse from(RequestView view) {
        return new RequestResponse(
                UUID.fromString(view.id().value()),
                view.businessKey(),
                view.status().name(),
                view.title());
    }
}
