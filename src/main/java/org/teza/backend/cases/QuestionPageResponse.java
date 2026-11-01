package org.teza.backend.cases;

import java.util.List;

public record QuestionPageResponse(
        List<QuestionResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
