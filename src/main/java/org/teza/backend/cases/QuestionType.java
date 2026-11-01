package org.teza.backend.cases;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

public enum QuestionType {
    EMPIRICAL, CAUSAL, NORMATIVE, PREDICTIVE, PRACTICAL;

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    @JsonCreator
    public static QuestionType fromValue(String value) {
        for (QuestionType type : values()) {
            if (type.value().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown question type");
    }
}
