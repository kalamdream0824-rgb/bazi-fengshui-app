package com.bazi.app.report.relationship;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class RelationshipDimensionEvaluator {

  public List<RelationshipYearEvaluation> evaluate(List<RelationshipYearFacts> years) {
    if (years == null || years.isEmpty()) {
      throw new IllegalArgumentException("relationship evaluation requires annual facts");
    }
    List<RelationshipYearEvaluation> result = new ArrayList<>();
    for (int index = 0; index < years.size(); index++) {
      RelationshipYearFacts year = years.get(index);
      if (index > 0 && year.year() != years.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("relationship evaluation years must be consecutive");
      }
      result.add(new RelationshipYearEvaluation(
          year.year(), year.ganZhi(), evaluateDimensions(year)));
    }
    return List.copyOf(result);
  }

  public Map<RelationshipDimension, RelationshipDimensionEvaluation> evaluateDimensions(
      RelationshipYearFacts year) {
    if (year == null) {
      throw new IllegalArgumentException("relationship year facts are required");
    }
    Map<RelationshipDimension, Map<String, RelationshipEvidence>> grouped =
        new EnumMap<>(RelationshipDimension.class);
    for (RelationshipDimension dimension : RelationshipDimension.values()) {
      grouped.put(dimension, new TreeMap<>());
    }

    addAll(grouped, year.natalProfile().evidence());
    addAll(grouped, year.evidence());

    Map<RelationshipDimension, RelationshipDimensionEvaluation> result =
        new EnumMap<>(RelationshipDimension.class);
    grouped.forEach((dimension, byKey) -> {
      List<RelationshipEvidence> evidence = List.copyOf(byKey.values());
      int support = evidence.stream()
          .filter(item -> item.weight() > 0)
          .mapToInt(RelationshipEvidence::weight)
          .sum();
      int limitation = evidence.stream()
          .filter(item -> item.weight() < 0)
          .mapToInt(item -> Math.abs(item.weight()))
          .sum();
      result.put(dimension, new RelationshipDimensionEvaluation(
          dimension,
          support,
          limitation,
          support + limitation,
          support - limitation,
          RelationshipDimensionEvaluation.toneFor(support, limitation),
          evidence));
    });
    return Map.copyOf(result);
  }

  private void addAll(
      Map<RelationshipDimension, Map<String, RelationshipEvidence>> grouped,
      List<RelationshipEvidence> evidence) {
    for (RelationshipEvidence item : evidence) {
      Map<String, RelationshipEvidence> dimensionEvidence = grouped.get(item.dimension());
      RelationshipEvidence existing = dimensionEvidence.putIfAbsent(item.key(), item);
      if (existing != null && !existing.equals(item)) {
        throw new IllegalArgumentException(
            "conflicting relationship evidence key: " + item.key());
      }
    }
  }
}
