package org.narxoz.handler;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.narxoz.config.RequestService;
import org.narxoz.config.SecurityConfig;
import org.narxoz.domain.DuplicateRequest;
import org.narxoz.domain.MissingRequest;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestStatus;
import org.narxoz.domain.RequestView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementContextAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RequestController.class, MeController.class})
@ImportAutoConfiguration({
        EndpointAutoConfiguration.class,
        HealthEndpointAutoConfiguration.class,
        WebEndpointAutoConfiguration.class,
        ManagementContextAutoConfiguration.class
})
@ContextConfiguration(classes = {RequestController.class, MeController.class, ApiErrors.class, SecurityConfig.class, ApiSecurityHandlers.class})
class RequestControllerTest {

    private static final String BASE = "/api/v1/purchase-requests";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RequestService service;

    @MockitoBean
    JwtDecoder jwtDecoder;

    @Test
    void validBodyIs201WithLocationAndFirstStatus() throws Exception {
        when(service.register(any(RequestId.class), eq("PR-19"), eq("Monitors for room 3")))
                .thenAnswer(call -> new RequestView(
                        call.getArgument(0), "PR-19", RequestStatus.DRAFT, "Monitors for room 3"));

        MvcResult result = mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"PR-19\",\"title\":\"Monitors for room 3\"}")
                        .with(jwt().authorities(() -> "ROLE_CLERK")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessKey").value("PR-19"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        String location = result.getResponse().getHeader("Location");
        assertNotNull(location, "Location header must be present");
        assertTrue(location.endsWith(id), "Location must end with the id");
    }

    @Test
    void getByUuidIs200() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.require(any(RequestId.class))).thenReturn(
                new RequestView(new RequestId(id.toString()), "PR-19", RequestStatus.DRAFT, "Monitors"));

        mvc.perform(get(BASE + "/" + id).with(jwt().authorities(() -> "ROLE_CLERK")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Monitors"));
    }

    @Test
    void blankTitleIs400NamesTheFieldAndSkipsTheService() throws Exception {
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"PR-19\",\"title\":\"\"}")
                        .with(jwt().authorities(() -> "ROLE_CLERK")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("title")));

        verifyNoInteractions(service);
    }

    @Test
    void missingRequestIs404() throws Exception {
        when(service.require(any(RequestId.class))).thenThrow(new MissingRequest("x"));

        mvc.perform(get(BASE + "/" + UUID.randomUUID()).with(jwt().authorities(() -> "ROLE_CLERK")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void duplicateRequestIs409() throws Exception {
        when(service.register(any(RequestId.class), any(String.class), any(String.class)))
                .thenThrow(new DuplicateRequest("PR-19"));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"PR-19\",\"title\":\"Again\"}")
                        .with(jwt().authorities(() -> "ROLE_CLERK")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PR-19")));
    }

    @Test
    void businessKeyInThePathIs400IdMustBeUuid() throws Exception {
        mvc.perform(get(BASE + "/PR-19").with(jwt().authorities(() -> "ROLE_CLERK")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("id must be a UUID"));
    }

    @Test
    void correlationHeaderComesBackOnA400() throws Exception {
        mvc.perform(post(BASE)
                        .header("X-Correlation-Id", "abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"PR-19\",\"title\":\"\"}")
                        .with(jwt().authorities(() -> "ROLE_CLERK")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").value("abc123"));
    }

    @Test
    void actuatorHealthIsOpenWithoutToken() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void anonymousUnknownApiRouteIs401() throws Exception {
        mvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(service);
    }

    @Test
    void registrationStaysClosedInTheLabProfile() throws Exception {
        mvc.perform(post("/api/v1/auth/register")).andExpect(status().isUnauthorized());
        mvc.perform(get("/register.html")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/register").with(jwt().authorities(() -> "ROLE_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ROLE_CLERK", "ROLE_MANAGER"})
    void authenticatedUnknownApiRouteIs403(String role) throws Exception {
        mvc.perform(get("/api/v1/unknown")
                        .with(jwt().authorities(() -> role))
                        .header("X-Correlation-Id", "unknown-route"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("not allowed for your role"))
                .andExpect(jsonPath("$.correlationId").value("unknown-route"));
        verifyNoInteractions(service);
    }

    @Test
    void anonymousCreateIs401WithoutCallingService() throws Exception {
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"PR-19\",\"title\":\"Monitors\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "DELETE", "OPTIONS"})
    void unsupportedResourceMethodIs403(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), BASE)
                        .with(jwt().authorities(() -> "ROLE_MANAGER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(service);
    }

    @Test
    void anonymousReadIs401WithErrorRecord() throws Exception {
        mvc.perform(get(BASE + "/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
                .andExpect(jsonPath("$.message").value("valid token required"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void clerkCannotApproveAndManagerCan() throws Exception {
        UUID id = UUID.randomUUID();
        mvc.perform(post(BASE + "/" + id + "/approve").with(jwt().authorities(() -> "ROLE_CLERK")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        verifyNoInteractions(service);

        when(service.approve(any(RequestId.class), eq("manager"))).thenReturn(
                new RequestView(new RequestId(id.toString()), "PR-19", RequestStatus.APPROVED, "Monitor"));
        mvc.perform(post(BASE + "/" + id + "/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actor\":\"ignored\",\"role\":\"CLERK\"}")
                        .with(jwt().jwt(token -> token.subject("manager")).authorities(() -> "ROLE_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
        verify(service).approve(new RequestId(id.toString()), "manager");
    }

    @Test
    void meReturnsUsernameAndAuthoritiesFromJwt() throws Exception {
        mvc.perform(get("/api/me").with(jwt().jwt(token -> token.subject("clerk"))
                        .authorities(() -> "ROLE_CLERK")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("clerk"))
                .andExpect(jsonPath("$.authorities[0]").value("ROLE_CLERK"));
    }

}
