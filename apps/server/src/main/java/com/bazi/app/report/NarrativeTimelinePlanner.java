package com.bazi.app.report;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@FunctionalInterface
public interface NarrativeTimelinePlanner<E, S> {

  NarrativeTimeline plan(Input<E, S> input);

  record Input<E, S>(
      String topicCode,
      E previousEvaluation,
      List<E> productEvaluations,
      Optional<S> userState) {

    public Input {
      Objects.requireNonNull(topicCode, "topicCode");
      if (topicCode.isBlank()) {
        throw new IllegalArgumentException("topicCode must not be blank");
      }
      topicCode = topicCode.strip();
      Objects.requireNonNull(previousEvaluation, "previousEvaluation");
      Objects.requireNonNull(productEvaluations, "productEvaluations");
      productEvaluations = List.copyOf(productEvaluations);
      if (productEvaluations.isEmpty()) {
        throw new IllegalArgumentException("productEvaluations must not be empty");
      }
      for (E evaluation : productEvaluations) {
        Objects.requireNonNull(evaluation, "product evaluation");
      }
      Objects.requireNonNull(userState, "userState");
    }
  }
}
