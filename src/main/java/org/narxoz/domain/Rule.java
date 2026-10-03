package org.narxoz.domain;

public interface Rule {
    void check(RequestStatus from, RequestStatus to);
}