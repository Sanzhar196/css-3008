package org.narxoz.domain;

import java.util.Map;
import java.util.Set;

public class TransitionRule implements Rule {
    private static final Map<RequestStatus, Set<RequestStatus>> ALLOWED = Map.of(
            RequestStatus.DRAFT,     Set.of(RequestStatus.APPROVED, RequestStatus.REJECTED),
            RequestStatus.APPROVED,  Set.of(RequestStatus.CANCELLED),
            RequestStatus.REJECTED,  Set.of(),
            RequestStatus.CANCELLED, Set.of());

    @Override
    public void check(RequestStatus from, RequestStatus to) {
        if (!ALLOWED.get(from).contains(to)) {
            throw new InvalidTransition(from + " -> " + to + " is forbidden");
        }
    }
}
