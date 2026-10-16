package org.narxoz.config;

import org.narxoz.domain.NotificationPort;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestStatus;
import org.narxoz.domain.Rule;
import org.narxoz.persistence.RequestJdbc;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestService {
    private final Rule rules;
    private final RequestJdbc requests;
    private final NotificationPort notifications;

    public RequestService(Rule rules, RequestJdbc requests, NotificationPort notifications) {
        this.rules = rules;
        this.requests = requests;
        this.notifications = notifications;
    }

    public RequestStatus move(RequestId id, RequestStatus from, RequestStatus to) {
        rules.check(from, to);
        notifications.statusChanged(id, to);
        return to;
    }

    /** One transaction per call. A duplicate key throws and nothing from this call is kept. */
    @Transactional
    public void register(RequestId id, String businessKey, String title) {
        requireText(businessKey, "businessKey");
        requireText(title, "title");
        requests.insert(id, businessKey, RequestStatus.DRAFT, title);
    }

    /** One transaction, two inserts. The second fails, so the first is rolled back too. */
    @Transactional
    public void insertTwice(String businessKey) {
        requests.insert(RequestId.newId(), businessKey, RequestStatus.DRAFT, "First");
        requests.insert(RequestId.newId(), businessKey, RequestStatus.DRAFT, "Second");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
