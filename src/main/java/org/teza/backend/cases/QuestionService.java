package org.teza.backend.cases;

import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
class QuestionService {
    private final QuestionRepository questions;

    QuestionService(QuestionRepository questions) {
        this.questions = questions;
    }

    @Transactional
    QuestionResponse create(CreateQuestionRequest request, String editor) {
        return questions.saveAndFlush(
                new Question(request.title(), request.description(), request.type(), editor)).response();
    }

    QuestionPageResponse list(int page, int size) {
        var result = questions.findAll(PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new QuestionPageResponse(result.map(Question::response).getContent(), page, size,
                result.getTotalElements(), result.getTotalPages());
    }

    QuestionResponse get(UUID id) {
        return find(id).response();
    }

    @Transactional
    QuestionResponse update(UUID id, UpdateQuestionRequest request) {
        if (request.title() == null && request.description() == null && request.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one field must be supplied");
        }
        Question question = find(id);
        if (question.version() != request.version()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Question has changed; reload before editing");
        }
        question.update(request);
        questions.flush();
        return question.response();
    }

    private Question find(UUID id) {
        return questions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));
    }
}
