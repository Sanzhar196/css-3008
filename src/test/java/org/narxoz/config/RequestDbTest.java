package org.narxoz.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.narxoz.domain.DuplicateRequest;
import org.narxoz.domain.RequestId;
import org.narxoz.persistence.RequestJdbc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RequestDbTest {

    @Autowired RequestService service;
    @Autowired RequestJdbc requests;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() throws IOException {
        jdbc.execute("DROP TABLE IF EXISTS purchase_request");
        jdbc.execute(Files.readString(Path.of("src/main/resources/db/schema.sql")));
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
}
