package org.teza.backend.cases;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateQuestionRequest(
        @NotNull @PositiveOrZero Long version,
        @Size(min = 1, max = 300) @Pattern(regexp = "(?sU)(?=.*\\S)[^\\x00]+") String title,
        @Size(max = 10000) @Pattern(regexp = "[^\\x00]*") String description,
        QuestionType type) {
}
