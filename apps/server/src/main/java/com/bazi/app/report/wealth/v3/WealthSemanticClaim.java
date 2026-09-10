package com.bazi.app.report.wealth.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Set;

/** One traceable money claim shared by report-level and timeline copy. */
public record WealthSemanticClaim(
    int year,
    String themeKey,
    String path,
    String stance,
    String strength,
    String objectKey,
    String objectText,
    String evidenceAngle,
    String evidenceDetailKey,
    String decisionId,
    List<String> supportingEvidenceIds,
    List<String> limitingEvidenceIds,
    List<String> dominantEvidenceIds,
    List<String> semanticEvidenceKeys) {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Set<String> STANCES = Set.of("supportive", "mixed", "restricted", "quiet");
  private static final Set<String> STRENGTHS = Set.of("none", "limited", "supported", "pronounced");

  public WealthSemanticClaim {
    if (year < 1 || !STANCES.contains(required(stance)) || !STRENGTHS.contains(required(strength))) {
      throw new IllegalArgumentException("invalid wealth semantic claim");
    }
    required(themeKey);
    required(path);
    required(objectKey);
    required(objectText);
    required(evidenceAngle);
    required(evidenceDetailKey);
    required(decisionId);
    supportingEvidenceIds = distinct(supportingEvidenceIds);
    limitingEvidenceIds = distinct(limitingEvidenceIds);
    dominantEvidenceIds = distinct(dominantEvidenceIds);
    semanticEvidenceKeys = distinct(semanticEvidenceKeys);
    List<String> allEvidenceIds = java.util.stream.Stream
        .concat(supportingEvidenceIds.stream(), limitingEvidenceIds.stream()).toList();
    if (allEvidenceIds.isEmpty() || dominantEvidenceIds.isEmpty()
        || !allEvidenceIds.containsAll(dominantEvidenceIds) || semanticEvidenceKeys.isEmpty()) {
      throw new IllegalArgumentException("wealth semantic claim must retain its evidence");
    }
  }

  /** Stable meaning key; never used to randomize copy. */
  public String semanticKey() {
    return json(List.of(themeKey, path, stance, strength, objectKey, evidenceAngle,
        evidenceDetailKey, semanticEvidenceKeys));
  }

  /** Neutral real-world subject that does not assume the customer's occupation. */
  public String subjectText() {
    return switch (path) {
      case "stable_income" -> "持续进账";
      case "skill_income" -> "新增的时间或资金投入";
      case "project_income" -> "已经约定但尚未到账的款项";
      case "cooperation_income" -> "与他人共同承担或分配的资金";
      case "retention" -> "扣除各项支出后实际留下的钱";
      default -> throw new IllegalArgumentException("unknown wealth claim path: " + path);
    };
  }

  private static List<String> distinct(List<String> values) {
    return values == null ? List.of() : values.stream().distinct().sorted().toList();
  }

  private static String required(String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("blank wealth semantic claim field");
    }
    return value;
  }

  private static String json(Object value) {
    try {
      return JSON.writeValueAsString(value);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("cannot encode wealth semantic claim", error);
    }
  }
}
