package org.narxoz.domain;

public interface NotificationPort {
    void statusChanged(RequestId id, RequestStatus to);
}
