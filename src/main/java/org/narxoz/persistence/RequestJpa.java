package org.narxoz.persistence;

import jakarta.persistence.EntityManager;
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
        RequestEntity found = em.find(RequestEntity.class, UUID.fromString(id.value()));
        if (found == null) {
            return Optional.empty();
        }
        // an unknown status text throws here: it never becomes a new status
        return Optional.of(new RequestView(id, found.getBusinessKey(),
                RequestStatus.valueOf(found.getStatus()), found.getTitle()));
    }

    public void updateStatus(RequestId id, RequestStatus status) {
        RequestEntity found = em.find(RequestEntity.class, UUID.fromString(id.value()));
        if (found == null) {
            return;
        }
        em.createQuery("update RequestEntity r set r.status = :status where r.id = :id")
                .setParameter("status", status.name())
                .setParameter("id", UUID.fromString(id.value()))
                .executeUpdate();
        em.clear();
    }

    public int count(String businessKey) {
        Long n = em.createQuery(
                        "select count(r) from RequestEntity r where r.businessKey = :key", Long.class)
                .setParameter("key", businessKey)
                .getSingleResult();
        return n.intValue();
    }
}
