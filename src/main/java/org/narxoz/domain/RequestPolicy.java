package org.narxoz.domain;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RequestPolicy {
    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = Map.of(
            RequestStatus.DRAFT,     Set.of(RequestStatus.APPROVED, RequestStatus.REJECTED),
            RequestStatus.APPROVED,  Set.of(RequestStatus.CANCELLED),
            RequestStatus.REJECTED,  Set.of(),
            RequestStatus.CANCELLED, Set.of());

    public RequestStatus move(RequestStatus from, RequestStatus to) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (!ALLOWED.get(from).contains(to)) {
            throw new IllegalStateException(from + " -> " + to + " is forbidden");
        }
        return to;
    }
}