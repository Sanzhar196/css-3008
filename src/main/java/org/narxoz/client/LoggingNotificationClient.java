package org.narxoz.client;

import org.narxoz.domain.NotificationPort;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestStatus;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationClient implements NotificationPort {
    private final DownstreamProperties downstream;

    public LoggingNotificationClient(DownstreamProperties downstream) {
        this.downstream = downstream;
    }

    @Override
    public void statusChanged(RequestId id, RequestStatus to) {
        // Lab 6 makes the real call to downstream.baseUrl() and enforces downstream.timeout()
        System.out.println("notify [" + downstream.baseUrl() + ", timeout " + downstream.timeout()
                + "]: " + id.value() + " -> " + to);
    }
}
