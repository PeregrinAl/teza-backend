package org.teza.backend.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"teza.editor.password=integration-test-password", "POSTGRES_PASSWORD=test"})
@AutoConfigureMockMvc
@Testcontainers
class SecurityIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> database = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void healthIsPublicButWorkspaceRequiresAuthentication() throws Exception {
        mvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mvc.perform(get("/api/v1/questions"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(get("/swagger-ui/index.html").accept(MediaType.TEXT_HTML)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/login")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/auth/login")));
    }

    @Test
    void sessionLoginRotatesCsrfAndLogoutRevokesSession() throws Exception {
        MvcResult beforeLogin = mvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk()).andReturn();
        JsonNode initialToken = mapper.readTree(beforeLogin.getResponse().getContentAsString());
        MockHttpSession session = (MockHttpSession) beforeLogin.getRequest().getSession(false);

        mvc.perform(post("/api/v1/auth/login").session(session)
                        .header(initialToken.get("headerName").asText(), initialToken.get("token").asText())
                        .param("username", "editor").param("password", "integration-test-password"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("editor"));

        mvc.perform(post("/api/v1/questions").session(session)
                        .header(initialToken.get("headerName").asText(), initialToken.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"type\":\"empirical\"}"))
                .andExpect(status().isForbidden());

        MvcResult afterLogin = mvc.perform(get("/api/v1/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        JsonNode token = mapper.readTree(afterLogin.getResponse().getContentAsString());
        mvc.perform(post("/api/v1/questions").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"type\":\"empirical\"}"))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/auth/logout").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidPasswordIsRejectedWithoutEchoingInput() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/auth/csrf")).andReturn();
        JsonNode token = mapper.readTree(result.getResponse().getContentAsString());
        mvc.perform(post("/api/v1/auth/login")
                        .session((MockHttpSession) result.getRequest().getSession(false))
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .param("username", "editor").param("password", "wrong-password"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    @WithMockUser(username = "editor", roles = "EDITOR")
    void authenticatedWriteStillRequiresCsrf() throws Exception {
        mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Test\",\"type\":\"empirical\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/questions']").exists())
                .andExpect(jsonPath("$.components.securitySchemes.csrfToken.name").value("X-CSRF-TOKEN"))
                .andExpect(jsonPath("$.paths['/api/v1/questions'].post.security[0].csrfToken").isArray());
    }

    @Test
    @WithMockUser(roles = "READER")
    void nonEditorCannotReadWorkspace() throws Exception {
        mvc.perform(get("/api/v1/questions")).andExpect(status().isForbidden());
    }
}
