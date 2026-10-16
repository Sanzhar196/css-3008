package org.narxoz.client;

import org.narxoz.domain.NotificationPort;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestStatus;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationClient implements NotificationPort {
    @Override
    public void statusChanged(RequestId id, RequestStatus to) {
        System.out.println("notify: " + id.value() + " -> " + to);
    }
}
