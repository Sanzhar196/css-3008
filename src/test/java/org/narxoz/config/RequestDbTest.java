package org.narxoz.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.narxoz.domain.DuplicateRequest;
import org.narxoz.domain.InvalidTransition;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestStatus;
import org.narxoz.persistence.RequestJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.concurrent.DelegatingSecurityContextCallable;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RequestDbTest {

    @Autowired RequestService service;
    @Autowired RequestJpa requests;
    @Autowired JdbcTemplate jdbc;
    private final List<String> testBusinessKeys = new ArrayList<>();

    @AfterEach
    void removeTestRows() {
        testBusinessKeys.forEach(key -> jdbc.update(
                "DELETE FROM purchase_request WHERE business_key = ?", key));
    }

    @Test
    void secondStatementRollsBack() {
        String key = testBusinessKey("PR-ROLLBACK");
        assertThrows(DuplicateRequest.class, () -> service.insertTwice(key));
        assertEquals(0, requests.count(key));
    }

    @Test
    void secondRequestKeepsTheFirst() {
        String key = testBusinessKey("PR-DUPLICATE");
        service.register(RequestId.newId(), key, "Laptops");
        assertThrows(DuplicateRequest.class,
                () -> service.register(RequestId.newId(), key, "Again"));
        assertEquals(1, requests.count(key));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void managerApprovalPersistsStatus() {
        RequestId id = RequestId.newId();
        service.register(id, testBusinessKey("PR-APPROVE"), "Laptops");

        assertEquals(RequestStatus.APPROVED, service.approve(id, "manager").status());
        assertThrows(InvalidTransition.class, () -> service.approve(id, "manager"));
        assertEquals(RequestStatus.APPROVED, service.require(id).status());
    }

    @Test
    @WithMockUser(username = "clerk", roles = "CLERK")
    void clerkCannotCallApprovalServiceOrChangeStatus() {
        RequestId id = RequestId.newId();
        service.register(id, testBusinessKey("PR-CLERK"), "Laptops");

        assertThrows(AccessDeniedException.class, () -> service.approve(id, "clerk"));
        assertEquals(RequestStatus.DRAFT, service.require(id).status());
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void concurrentApprovalAllowsOnlyOneTransition() throws Exception {
        RequestId id = RequestId.newId();
        service.register(id, testBusinessKey("PR-CONCURRENT"), "Laptops");
        var ready = new CountDownLatch(2);
        Callable<Boolean> approval = () -> {
            ready.countDown();
            if (!ready.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Both approval calls must be ready");
            }
            try {
                service.approve(id, "manager");
                return true;
            } catch (InvalidTransition ex) {
                return false;
            }
        };

        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(new DelegatingSecurityContextCallable<>(approval));
            var second = pool.submit(new DelegatingSecurityContextCallable<>(approval));
            int approved = (first.get(10, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, TimeUnit.SECONDS) ? 1 : 0);
            assertEquals(1, approved);
        }
        assertEquals(RequestStatus.APPROVED, service.require(id).status());
    }

    private String testBusinessKey(String prefix) {
        String key = prefix + "-" + UUID.randomUUID();
        testBusinessKeys.add(key);
        return key;
    }
}
