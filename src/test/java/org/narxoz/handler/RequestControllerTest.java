package org.narxoz.handler;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.narxoz.config.RequestService;
import org.narxoz.domain.DuplicateRequest;
import org.narxoz.domain.MissingRequest;
import org.narxoz.domain.RequestId;
import org.narxoz.domain.RequestStatus;
import org.narxoz.domain.RequestView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestController.class)
@ContextConfiguration(classes = {RequestController.class, ApiErrors.class})
class RequestControllerTest {

    private static final String BASE = "/api/v1/purchase-requests";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    RequestService service;

    @Test
    void validBodyIs201WithLocationAndFirstStatus() throws Exception {
        when(service.register(any(RequestId.class), eq("PR-19"), eq("Monitors for room 3")))
                .thenAnswer(call -> new RequestView(
                        call.getArgument(0), "PR-19", RequestStatus.DRAFT, "Monitors for room 3"));

        MvcResult result = mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"PR-19\",\"title\":\"Monitors for room 3\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.businessKey").value("PR-19"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        String id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        String location = result.getResponse().getHeader("Location");
        assertTrue(location.endsWith(id), "Location must end with the id");
    }

    @Test
    void getByUuidIs200() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.require(any(RequestId.class))).thenReturn(
                new RequestView(new RequestId(id.toString()), "PR-19", RequestStatus.DRAFT, "Monitors"));

        mvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.title").value("Monitors"));
    }

    @Test
    void blankTitleIs400NamesTheFieldAndSkipsTheService() throws Exception {
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"PR-19\",\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("title")));

        verifyNoInteractions(service);
    }

    @Test
    void missingRequestIs404() throws Exception {
        when(service.require(any(RequestId.class))).thenThrow(new MissingRequest("x"));

        mvc.perform(get(BASE + "/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void duplicateRequestIs409() throws Exception {
        when(service.register(any(RequestId.class), any(String.class), any(String.class)))
                .thenThrow(new DuplicateRequest("PR-19"));

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"PR-19\",\"title\":\"Again\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PR-19")));
    }

    @Test
    void businessKeyInThePathIs400IdMustBeUuid() throws Exception {
        mvc.perform(get(BASE + "/PR-19"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION"))
                .andExpect(jsonPath("$.message").value("id must be a UUID"));
    }

    @Test
    void correlationHeaderComesBackOnA400() throws Exception {
        mvc.perform(post(BASE)
                        .header("X-Correlation-Id", "abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"businessKey\":\"PR-19\",\"title\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correlationId").value("abc123"));
    }
}
