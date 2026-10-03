package org.narxoz.config;

import org.junit.jupiter.api.Test;
import org.narxoz.domain.RequestStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class RequestServiceTest {

    @Autowired
    RequestService service;

    @Test
    void allowedMoveReturnsNewStatus() {
        assertEquals(RequestStatus.APPROVED,
                service.move(RequestStatus.DRAFT, RequestStatus.APPROVED));
    }

    @Test
    void forbiddenMoveThrows() {
        assertThrows(IllegalStateException.class,
                () -> service.move(RequestStatus.DRAFT, RequestStatus.CANCELLED));
    }
}