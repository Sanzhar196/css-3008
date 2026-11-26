package org.narxoz.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.narxoz.domain.DuplicateRequest;
import org.narxoz.domain.RequestId;
import org.narxoz.persistence.RequestJpa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RequestDbTest {

    @Autowired RequestService service;
    @Autowired RequestJpa requests;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void empty() {
        jdbc.update("DELETE FROM purchase_request");
    }

    @Test
    void secondStatementRollsBack() {
        assertThrows(DuplicateRequest.class, () -> service.insertTwice("PR-19"));
        assertEquals(0, requests.count("PR-19"));
    }

    @Test
    void secondRequestKeepsTheFirst() {
        service.register(RequestId.newId(), "PR-19", "Laptops");
        assertThrows(DuplicateRequest.class,
                () -> service.register(RequestId.newId(), "PR-19", "Again"));
        assertEquals(1, requests.count("PR-19"));
    }

    @Test
    @WithMockUser(username = "manager", roles = "MANAGER")
    void managerApprovalPersistsStatus() {
        RequestId id = RequestId.newId();
        service.register(id, "PR-APPROVE", "Laptops");

        assertEquals(org.narxoz.domain.RequestStatus.APPROVED, service.approve(id, "manager").status());
        assertEquals(org.narxoz.domain.RequestStatus.APPROVED, service.require(id).status());
    }

    @Test
    @WithMockUser(username = "clerk", roles = "CLERK")
    void clerkCannotCallApprovalServiceOrChangeStatus() {
        RequestId id = RequestId.newId();
        service.register(id, "PR-CLERK", "Laptops");

        assertThrows(AccessDeniedException.class, () -> service.approve(id, "clerk"));
        assertEquals(org.narxoz.domain.RequestStatus.DRAFT, service.require(id).status());
    }
}
