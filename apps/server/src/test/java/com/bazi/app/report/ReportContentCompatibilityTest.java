package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.report.relationship.RelationshipDimension;
import com.bazi.app.report.relationship.RelationshipNarrativePlan;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportContentCompatibilityTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void deserializesRelationshipV1WithoutFallingBackToLegacyCareerShape() throws Exception {
    RelationshipNarrativePlan original = validPlan();

    RelationshipNarrativePlan restored = objectMapper.readValue(
        objectMapper.writeValueAsString(original), RelationshipNarrativePlan.class);

    assertEquals("dating", restored.relationshipStatus());
    assertEquals(5, restored.dimensions().size());
    assertEquals(3, restored.years().size());
    // Legacy snapshots intentionally contain repeated judgments: reads must never rewrite them.
    assertEquals(original, restored);
  }

  @Test
  void rejectsInvalidRelationshipSnapshotsAtTheContentBoundary() throws Exception {
    JsonNode valid = objectMapper.valueToTree(validPlan());

    JsonNode wrongYearCount = valid.deepCopy();
    ((com.fasterxml.jackson.databind.node.ArrayNode) wrongYearCount.get("years")).remove(2);
    assertThrows(Exception.class,
        () -> objectMapper.treeToValue(wrongYearCount, RelationshipNarrativePlan.class));

    JsonNode repeatedActions = valid.deepCopy();
    String firstAction = repeatedActions.at("/years/0/actions/0").asText();
    ((com.fasterxml.jackson.databind.node.ArrayNode) repeatedActions.at("/years/0/actions"))
        .set(1, objectMapper.getNodeFactory().textNode(firstAction));
    assertThrows(Exception.class,
        () -> objectMapper.treeToValue(repeatedActions, RelationshipNarrativePlan.class));

    JsonNode unresolvedToken = valid.deepCopy();
    ((com.fasterxml.jackson.databind.node.ObjectNode) unresolvedToken).put("thesis", "请继续{action}");
    assertThrows(Exception.class,
        () -> objectMapper.treeToValue(unresolvedToken, RelationshipNarrativePlan.class));

    JsonNode missingEvidence = valid.deepCopy();
    ((com.fasterxml.jackson.databind.node.ArrayNode) missingEvidence.get("evidenceKeys")).removeAll();
    assertThrows(Exception.class,
        () -> objectMapper.treeToValue(missingEvidence, RelationshipNarrativePlan.class));
  }

  private RelationshipNarrativePlan validPlan() {
    List<RelationshipNarrativePlan.DimensionSummary> dimensions = Arrays.stream(
            RelationshipDimension.values())
        .map(dimension -> new RelationshipNarrativePlan.DimensionSummary(
            dimension.code(),
            dimension.label(),
            "主要内容",
            "supportive",
            "这项有明确依据。",
            List.of("evidence." + dimension.code()),
            List.of()))
        .toList();
    List<RelationshipNarrativePlan.YearNarrative> years = List.of(
        year(2026, "丙午", "第一年先看两个人能否稳定回应。"),
        year(2027, "丁未", "第二年再看两个人能否安排生活。"),
        year(2028, "戊申", "第三年确认两个人是否适合继续。"));
    return new RelationshipNarrativePlan(
        "dating",
        "已确认交往关系",
        3,
        "先看两个人是否适合继续走下去。",
        "主要看关系连接，也要看回应与表达。",
        dimensions,
        "connection",
        "response",
        false,
        null,
        years,
        List.of("evidence.connection", "evidence.response"));
  }

  private RelationshipNarrativePlan.YearNarrative year(int year, String ganZhi, String focus) {
    return new RelationshipNarrativePlan.YearNarrative(
        year,
        ganZhi,
        focus,
        "两个人今年更容易把想法说清楚。",
        null,
        List.of("你愿意说出真实想法。", "对方愿意认真回答你。"),
        List.of("你先说清自己的需要。", "两个人约定下一步安排。"),
        "下一年继续看两个人能否把安排落实。",
        "connection",
        "response",
        null,
        List.of("evidence.connection", "evidence.response"));
  }
}
