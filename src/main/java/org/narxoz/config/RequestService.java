package org.narxoz.config;

import org.narxoz.domain.DuplicateRequest;
import org.narxoz.domain.MissingRequest;
import org.narxoz.domain.NotificationPort;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestStatus;
import org.narxoz.domain.RequestView;
import org.narxoz.domain.Rule;
import org.narxoz.persistence.RequestJpa;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RequestService {
    private final Rule rules;
    private final RequestJpa requests;
    private final NotificationPort notifications;

    public RequestService(Rule rules, RequestJpa requests, NotificationPort notifications) {
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
    public RequestView register(RequestId id, String businessKey, String title) {
        requireText(businessKey, "businessKey");
        requireText(title, "title");
        insertOrDuplicate(id, businessKey, title);
        return new RequestView(id, businessKey, RequestStatus.DRAFT, title);
    }

    /** Read only. A missing row throws MissingRequest. It never inserts, moves or flushes. */
    @Transactional(readOnly = true)
    public RequestView require(RequestId id) {
        return requests.find(id).orElseThrow(() -> new MissingRequest(id.value()));
    }

    /** One transaction, two inserts. The second fails, so the first is rolled back too. */
    @Transactional
    public void insertTwice(String businessKey) {
        insertOrDuplicate(RequestId.newId(), businessKey, "First");
        insertOrDuplicate(RequestId.newId(), businessKey, "Second");
    }

    private void insertOrDuplicate(RequestId id, String businessKey, String title) {
        try {
            requests.insert(id, businessKey, RequestStatus.DRAFT, title);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateRequest(businessKey); // unchecked, rethrown: Spring rolls back
        }
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
