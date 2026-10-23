package org.narxoz.persistence;

import jakarta.persistence.EntityManager;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestStatus;
import org.springframework.stereotype.Repository;

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

    public int count(String businessKey) {
        Long n = em.createQuery(
                        "select count(r) from RequestEntity r where r.businessKey = :key", Long.class)
                .setParameter("key", businessKey)
                .getSingleResult();
        return n.intValue();
    }
}
