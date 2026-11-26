package org.narxoz.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.narxoz.domain.MissingRequest;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestStatus;
import org.narxoz.domain.RequestView;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RequestJpa {
    private final EntityManager em;

    public RequestJpa(EntityManager em) {
        this.em = em;
    }

    public void insert(RequestId id, String businessKey, RequestStatus status, String title) {
        em.persist(new RequestEntity(UUID.fromString(id.value()), businessKey, status.name(), title));
        em.flush(); // send the INSERT now, so a UNIQUE violation surfaces here
    }

    public Optional<RequestView> find(RequestId id) {
        return Optional.ofNullable(em.find(RequestEntity.class, UUID.fromString(id.value())))
                .map(found -> toView(id, found));
    }

    public Optional<RequestView> findForUpdate(RequestId id) {
        return Optional.ofNullable(em.find(RequestEntity.class, UUID.fromString(id.value()),
                        LockModeType.PESSIMISTIC_WRITE))
                .map(found -> toView(id, found));
    }

    public void updateStatus(RequestId id, RequestStatus status) {
        RequestEntity found = em.find(RequestEntity.class, UUID.fromString(id.value()));
        if (found == null) {
            throw new MissingRequest(id.value());
        }
        found.setStatus(status.name());
    }

    private static RequestView toView(RequestId id, RequestEntity found) {
        return new RequestView(id, found.getBusinessKey(),
                RequestStatus.valueOf(found.getStatus()), found.getTitle());
    }

    public int count(String businessKey) {
        Long n = em.createQuery(
                        "select count(r) from RequestEntity r where r.businessKey = :key", Long.class)
                .setParameter("key", businessKey)
                .getSingleResult();
        return n.intValue();
    }
}
