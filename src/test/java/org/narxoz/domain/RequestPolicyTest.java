package org.narxoz.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class RequestPolicyTest {
    private final RequestPolicy policy = new RequestPolicy();

    @ParameterizedTest
    @CsvSource({
            "DRAFT, APPROVED, true",
            "APPROVED, CANCELLED, true",
            "DRAFT, CANCELLED, false",
            "REJECTED, CANCELLED, false"
    })
    void fourReadmeRows(RequestStatus from, RequestStatus to, boolean allowed) {
        if (allowed) {
            assertEquals(to, policy.move(from, to));
        } else {
            assertThrows(IllegalStateException.class, () -> policy.move(from, to));
        }
    }

    @Test
    void nullIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> new RequestId(null));
    }

    @Test
    void blankIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> new RequestId("  "));
    }
}