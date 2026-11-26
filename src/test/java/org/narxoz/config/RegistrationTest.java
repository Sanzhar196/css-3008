package org.narxoz.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "registration"})
class RegistrationTest {
    private static final String REGISTER = "/api/v1/auth/register";
    private static final String REQUESTS = "/api/v1/purchase-requests";
    private static final String PASSWORD = "RegistrationTest-123";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired RegistrationUsers users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    private final List<String> testUsernames = new ArrayList<>();

    @AfterEach
    void removeRegisteredTestAccounts() {
        for (String username : testUsernames) {
            jdbc.update("DELETE FROM registration.account WHERE username = ?", username);
        }
    }

    @Test
    void registrationCreatesOnlyAClerkWithABcryptPassword() throws Exception {
        String username = username();
        register(username.toUpperCase(Locale.ROOT), PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("CLERK"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        var account = jdbc.queryForMap("""
                SELECT username, password_hash, role, created_at
                FROM registration.account WHERE username = ?
                """, username);
        String storedHash = (String) account.get("password_hash");
        assertEquals(username, account.get("username"));
        assertEquals("CLERK", account.get("role"));
        assertNotNull(account.get("created_at"));
        assertNotEquals(PASSWORD, storedHash);
        assertTrue(storedHash.startsWith("{bcrypt}"));
        assertTrue(encoder.matches(PASSWORD, storedHash));
        assertTrue(users.loadUserByUsername("manager").getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_MANAGER")));

        String clerkToken = login(username.toUpperCase(Locale.ROOT), PASSWORD);
        mvc.perform(get("/api/me").headers(bearer(clerkToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_CLERK"));
        login(username, PASSWORD);

        String businessKey = "REG-" + UUID.randomUUID();
        try {
            String created = mvc.perform(post(REQUESTS)
                            .headers(bearer(clerkToken))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("businessKey", businessKey, "title", "Laptop"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value("DRAFT"))
                    .andReturn().getResponse().getContentAsString();
            String requestPath = REQUESTS + "/" + JsonPath.<String>read(created, "$.id");
            mvc.perform(post(requestPath + "/approve").headers(bearer(clerkToken)))
                    .andExpect(status().isForbidden());
            mvc.perform(get(requestPath).headers(bearer(clerkToken)))
                    .andExpect(jsonPath("$.status").value("DRAFT"));
            mvc.perform(post(requestPath + "/approve").headers(bearer(login("manager", "manager-dev"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("APPROVED"));
        } finally {
            jdbc.update("DELETE FROM purchase_request WHERE business_key = ?", businessKey);
        }
    }

    @Test
    void loginLoadsAnAccountAlreadyStoredInPostgres() throws Exception {
        String username = username();
        jdbc.update("""
                INSERT INTO registration.account (username, password_hash, role)
                VALUES (?, ?, ?)
                """, username, encoder.encode(PASSWORD), "CLERK");

        String token = login(username, PASSWORD);
        mvc.perform(get("/api/me").headers(bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_CLERK"));
    }

    @Test
    void duplicateUsernameCannotOverwriteExistingCredentials() throws Exception {
        String username = username();
        register(username, PASSWORD).andExpect(status().isCreated());
        for (String taken : new String[]{username.toUpperCase(Locale.ROOT), "MANAGER", "clerk"}) {
            register(taken, "different-password")
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE"))
                    .andExpect(jsonPath("$.correlationId").value("register-test"));
        }
        login(username, PASSWORD);
        login("manager", "manager-dev");
        login("clerk", "clerk-dev");
    }

    @Test
    void simultaneousRegistrationsCannotBothClaimTheSameUsername() throws Exception {
        String username = username();
        var ready = new CountDownLatch(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var firstRequest = executor.submit(() -> tryRegistration(ready, username));
            var secondRequest = executor.submit(() -> tryRegistration(ready, username.toUpperCase(Locale.ROOT)));
            int first = firstRequest.get(10, TimeUnit.SECONDS);
            int second = secondRequest.get(10, TimeUnit.SECONDS);
            assertTrue((first == 201 && second == 409) || (first == 409 && second == 201));
        }
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM registration.account WHERE username = ?", Integer.class, username));
    }

    @ParameterizedTest
    @MethodSource("invalidPasswords")
    void invalidPasswordsAreRejected(String password) throws Exception {
        register(username(), password)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.correlationId").value("register-test"));
    }

    private static Stream<String> invalidPasswords() {
        return Stream.of("", "short", "😀".repeat(4), "я".repeat(37));
    }

    @Test
    void passwordAtBcryptByteLimitCanSignIn() throws Exception {
        String username = username();
        String password = "я".repeat(36);
        register(username, password).andExpect(status().isCreated());
        login(username, password);
    }

    @Test
    void registrationPageAndScriptAreAvailable() throws Exception {
        mvc.perform(get("/register.html")).andExpect(status().isOk());
        mvc.perform(get("/registration.js")).andExpect(status().isOk());
    }

    @Test
    void invalidUsernameIsRejected() throws Exception {
        register("not a username", PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"));
    }

    private ResultActions register(String username, String password) throws Exception {
        return mvc.perform(post(REGISTER)
                .header("X-Correlation-Id", "register-test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", password, "role", "MANAGER"))));
    }

    private int tryRegistration(CountDownLatch ready, String username) throws Exception {
        ready.countDown();
        if (!ready.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Both registrations must be ready");
        }
        return register(username, PASSWORD).andReturn().getResponse().getStatus();
    }

    private String login(String username, String password) throws Exception {
        String reply = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(reply, "$.accessToken");
    }

    private static HttpHeaders bearer(String token) {
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private String username() {
        String name = "user-" + UUID.randomUUID().toString().substring(0, 12);
        testUsernames.add(name);
        return name;
    }
}
