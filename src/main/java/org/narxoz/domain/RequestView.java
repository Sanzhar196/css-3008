package org.narxoz.domain;

public record RequestView(RequestId id, String businessKey, RequestStatus status, String title) {
}
