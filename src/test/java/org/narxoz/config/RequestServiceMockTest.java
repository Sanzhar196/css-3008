package org.narxoz.config;

import org.junit.jupiter.api.Test;
import org.narxoz.domain.NotificationPort;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestPolicy;
import org.narxoz.domain.RequestStatus;
import org.narxoz.persistence.RequestJdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RequestServiceMockTest {
    private final NotificationPort port = mock(NotificationPort.class);
    private final RequestService service =
            new RequestService(new RequestPolicy(), mock(RequestJdbc.class), port);
    private final RequestId id = new RequestId("REQ-001");

    @Test
    void allowedMoveNotifiesPort() {
        assertEquals(RequestStatus.APPROVED,
                service.move(id, RequestStatus.DRAFT, RequestStatus.APPROVED));
        verify(port).statusChanged(id, RequestStatus.APPROVED);
    }

    @Test
    void forbiddenMoveDoesNotNotify() {
        assertThrows(IllegalStateException.class,
                () -> service.move(id, RequestStatus.DRAFT, RequestStatus.CANCELLED));
        verifyNoInteractions(port);
    }
}
