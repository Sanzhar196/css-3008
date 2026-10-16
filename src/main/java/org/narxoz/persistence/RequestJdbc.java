package org.narxoz.persistence;

import org.narxoz.domain.DuplicateRequest;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestStatus;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class RequestJdbc {
    private final JdbcTemplate jdbc;

    public RequestJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insert(RequestId id, String businessKey, RequestStatus status, String title) {
        try {
            jdbc.update("""
                    INSERT INTO purchase_request (id, business_key, status, title)
                    VALUES (?, ?, ?, ?)
                    """, UUID.fromString(id.value()), businessKey, status.name(), title);
        } catch (DuplicateKeyException ex) {
            throw new DuplicateRequest(businessKey);
        }
    }

    public int count(String businessKey) {
        Integer n = jdbc.queryForObject(
                "SELECT count(*) FROM purchase_request WHERE business_key = ?",
                Integer.class, businessKey);
        return n == null ? 0 : n;
    }
}
