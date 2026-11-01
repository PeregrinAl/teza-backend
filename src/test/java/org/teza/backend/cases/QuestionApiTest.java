package org.teza.backend.cases;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "teza.editor.password=integration-test-password",
        "POSTGRES_PASSWORD=test-not-used"
})
@AutoConfigureMockMvc
@Testcontainers
@WithMockUser(username = "editor", roles = "EDITOR")
class QuestionApiTest {
    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    QuestionRepository questions;

    @BeforeEach
    void clearQuestions() {
        questions.deleteAllInBatch();
    }

    @Test
    void createsAndReadsPersistedQuestionWithEditorAndTimestamps() throws Exception {
        JsonNode created = createQuestion("Есть ли спрос?", "Проверка спроса");
        String id = created.get("id").asText();

        assertThat(questions.count()).isEqualTo(1);
        assertThat(created.get("version").asLong()).isZero();
        assertThat(created.get("createdAt").asText()).isNotBlank();
        assertThat(created.get("updatedAt")).isEqualTo(created.get("createdAt"));
        mvc.perform(get("/api/v1/questions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Есть ли спрос?"))
                .andExpect(jsonPath("$.description").value("Проверка спроса"))
                .andExpect(jsonPath("$.type").value("empirical"))
                .andExpect(jsonPath("$.createdBy").value("editor"));
    }

    @Test
    void updatesOnlySuppliedFieldsAndRejectsStaleRevision() throws Exception {
        JsonNode created = createQuestion("Исходный вопрос", "Сохранённое описание");
        String id = created.get("id").asText();

        mvc.perform(patch("/api/v1/questions/{id}", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"title":"Обновлённый вопрос","type":"causal"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновлённый вопрос"))
                .andExpect(jsonPath("$.description").value("Сохранённое описание"))
                .andExpect(jsonPath("$.type").value("causal"))
                .andExpect(jsonPath("$.version").value(1));

        mvc.perform(patch("/api/v1/questions/{id}", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"title":"Устаревшая правка"}
                                """))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/v1/questions/{id}", id))
                .andExpect(jsonPath("$.title").value("Обновлённый вопрос"))
                .andExpect(jsonPath("$.createdBy").value("editor"));
    }

    @Test
    void emptyDescriptionClearsDescription() throws Exception {
        String id = createQuestion("Вопрос", "Описание").get("id").asText();
        mvc.perform(patch("/api/v1/questions/{id}", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"description":""}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value(""));
    }

    @Test
    void listsPagesWithoutSerializingJpaEntities() throws Exception {
        createQuestion("Первый вопрос", "");
        createQuestion("Второй вопрос", "");

        JsonNode first = json.readTree(mvc.perform(get("/api/v1/questions?page=0&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andReturn().getResponse().getContentAsString());
        JsonNode second = json.readTree(mvc.perform(get("/api/v1/questions?page=1&size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn().getResponse().getContentAsString());
        assertThat(first.get("items").get(0).get("id"))
                .isNotEqualTo(second.get("items").get(0).get("id"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"size=0", "size=101", "page=-1", "size=invalid"})
    void rejectsInvalidPagination(String query) throws Exception {
        mvc.perform(get("/api/v1/questions?" + query)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"title\":\" \",\"type\":\"empirical\"}",
            "{\"title\":\"Вопрос\"}",
            "{\"title\":\"Вопрос\",\"type\":\"unknown\"}"
    })
    void rejectsInvalidCreateBody(String body) throws Exception {
        mvc.perform(post("/api/v1/questions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        assertThat(questions.count()).isZero();
    }

    @Test
    void rejectsOversizedTextBeforeSaving() throws Exception {
        String privateText = "synthetic-private-marker-" + "a".repeat(10001);
        String body = json.writeValueAsString(Map.of(
                "title", "Вопрос", "description", privateText, "type", "empirical"));
        String response = mvc.perform(post("/api/v1/questions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("synthetic-private-marker-");
        assertThat(questions.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"title", "description"})
    void rejectsNullCharactersBeforeDatabaseWrite(String field) throws Exception {
        Map<String, String> body = new java.util.HashMap<>(Map.of(
                "title", "Вопрос", "description", "Описание", "type", "empirical"));
        body.put(field, "text\0text");
        mvc.perform(post("/api/v1/questions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        assertThat(questions.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"version\":0}", "{\"title\":\"Вопрос\"}",
            "{\"version\":0,\"title\":\"   \"}"})
    void rejectsInvalidPatch(String body) throws Exception {
        String id = createQuestion("Вопрос", "").get("id").asText();
        mvc.perform(patch("/api/v1/questions/{id}", id).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void missingQuestionReturnsNotFoundAndMalformedIdReturnsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/questions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/questions/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithAnonymousUser
    void anonymousVisitorCannotReadWorkingQuestions() throws Exception {
        mvc.perform(get("/api/v1/questions")).andExpect(status().isUnauthorized());
    }

    @Test
    void writeWithoutCsrfDoesNotPersist() throws Exception {
        mvc.perform(post("/api/v1/questions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Вопрос","type":"empirical"}
                                """))
                .andExpect(status().isForbidden());
        assertThat(questions.count()).isZero();
    }

    private JsonNode createQuestion(String title, String description) throws Exception {
        String result = mvc.perform(post("/api/v1/questions").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "title", title, "description", description, "type", "empirical"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return json.readTree(result);
    }
}
