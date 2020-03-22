package com.lukas.v_app.data;

import java.util.Objects;

public class RecommendationAction {

    private String message;

    public RecommendationAction(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final RecommendationAction that = (RecommendationAction) o;
        return message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message);
    }
}
