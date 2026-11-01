package org.teza.backend.cases;

import java.time.Instant;
import java.util.UUID;

public record QuestionResponse(
        UUID id,
        String title,
        String description,
        QuestionType type,
        String createdBy,
        Instant createdAt,
        Instant updatedAt,
        long version) {
}
