package org.teza.backend.cases;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/questions")
class QuestionController {
    private final QuestionService questions;

    QuestionController(QuestionService questions) {
        this.questions = questions;
    }

    @PostMapping
    ResponseEntity<QuestionResponse> create(@Valid @RequestBody CreateQuestionRequest request, Principal editor) {
        QuestionResponse result = questions.create(request, editor.getName());
        return ResponseEntity.created(URI.create("/api/v1/questions/" + result.id())).body(result);
    }

    @GetMapping
    QuestionPageResponse list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return questions.list(page, size);
    }

    @GetMapping("/{id}")
    QuestionResponse get(@PathVariable UUID id) {
        return questions.get(id);
    }

    @PatchMapping("/{id}")
    QuestionResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateQuestionRequest request) {
        return questions.update(id, request);
    }
}
