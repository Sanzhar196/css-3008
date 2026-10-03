package org.narxoz.domain;

public class OnlyApprovedCanCancelRule implements Rule {
    @Override
    public void check(RequestStatus from, RequestStatus to) {
        if (to == RequestStatus.CANCELLED && from != RequestStatus.APPROVED) {
            throw new IllegalStateException("Only an APPROVED request can be cancelled");
        }
    }
}