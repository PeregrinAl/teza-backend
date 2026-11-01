package org.teza.backend.cases;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
        @NotBlank @Size(max = 300) @Pattern(regexp = "[^\\x00]*") String title,
        @Size(max = 10000) @Pattern(regexp = "[^\\x00]*") String description,
        @NotNull QuestionType type) {
}
