package org.teza.backend.cases;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(name = "questions")
class Question {
    @Id
    private UUID id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, length = 10000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType type;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Question() {
    }

    Question(String title, String description, QuestionType type, String createdBy) {
        this.id = UUID.randomUUID();
        this.title = title.strip();
        this.description = description == null ? "" : description;
        this.type = type;
        this.createdBy = createdBy;
    }

    @PrePersist
    void initializeTimestamps() {
        createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        updatedAt = createdAt;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    void update(UpdateQuestionRequest request) {
        if (request.title() != null) {
            title = request.title().strip();
        }
        if (request.description() != null) {
            description = request.description();
        }
        if (request.type() != null) {
            type = request.type();
        }
    }

    QuestionResponse response() {
        return new QuestionResponse(id, title, description, type, createdBy, createdAt, updatedAt, version);
    }

    long version() {
        return version;
    }
}
