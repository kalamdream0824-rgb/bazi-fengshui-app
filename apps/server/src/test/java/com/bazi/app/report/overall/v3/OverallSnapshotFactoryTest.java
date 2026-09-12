package com.bazi.app.report.overall.v3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.AnnualPeriodAssessor;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.ReportTopic;
import com.bazi.app.report.relationship.RelationshipDimensionEvaluator;
import com.bazi.app.report.relationship.RelationshipFactExtractor;
import com.bazi.app.report.relationship.RelationshipPeriodArbitrator;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.bazi.app.report.wealth.v3.WealthV3Analyzer;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OverallSnapshotFactoryTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private PaipanRequest request;
  private PaipanResultDto chart;
  private List<AnnualContext> contexts;

  @BeforeEach
  void setUp() {
    request = new PaipanRequest(
        "综合快照样本", "male", "1995-10-08T14:30:00", "上海", false);
    chart = new BaziService().paipan(request);
    contexts = new AnnualContextFactory(CLOCK)
        .create(request, chart, ReportHorizon.of(3));
  }

  @Test
  void createsFourOrderedReadOnlySnapshotsForEveryYear() {
    List<OverallTopicSnapshot> snapshots = new OverallSnapshotFactory(CLOCK)
        .create(request, chart, contexts);

    assertEquals(12, snapshots.size());
    for (AnnualContext context : contexts) {
      List<OverallTopicSnapshot> annual = snapshots.stream()
          .filter(snapshot -> snapshot.year() == context.year())
          .toList();
      assertEquals(List.of(OverallTopicSnapshot.Topic.values()),
          annual.stream().map(OverallTopicSnapshot::topic).toList());
      assertTrue(annual.stream().allMatch(snapshot -> snapshot.evidenceKeys().size() >= 2));
      assertTrue(annual.stream().allMatch(snapshot -> snapshot.directEvidenceCount() >= 1));
    }
  }

  @Test
  void wealthSnapshotUsesTheWealthV3FocusAndEvidence() {
    List<OverallTopicSnapshot> snapshots = new OverallSnapshotFactory(CLOCK)
        .create(request, chart, contexts);
    var assessments = new WealthV3Analyzer().analyze(chart, contexts);

    for (int index = 0; index < contexts.size(); index++) {
      var assessment = assessments.get(index);
      OverallTopicSnapshot snapshot = snapshot(
          snapshots, contexts.get(index).year(), OverallTopicSnapshot.Topic.WEALTH);
      String expectedPath = !assessment.focus().primaryCandidates().isEmpty()
          ? assessment.focus().primaryCandidates().get(0)
          : assessment.risk() != null ? assessment.risk().path()
          : assessment.decisions().stream()
              .max(java.util.Comparator.comparingInt(decision ->
                  decision.supportWeight() + decision.limitationWeight()))
              .orElseThrow().path();
      Set<String> wealthRuleKeys = assessment.evidence().stream()
          .map(evidence -> evidence.ruleKey())
          .collect(java.util.stream.Collectors.toSet());

      assertTrue(snapshot.focusKey().startsWith("wealth." + expectedPath), snapshot.focusKey());
      assertTrue(wealthRuleKeys.containsAll(snapshot.evidenceKeys()), snapshot.evidenceKeys().toString());
    }
  }

  @Test
  void careerSnapshotUsesOnlyTheCareerRuleAssessment() {
    List<OverallTopicSnapshot> snapshots = new OverallSnapshotFactory(CLOCK)
        .create(request, chart, contexts);
    AnnualPeriodAssessor assessor = new AnnualPeriodAssessor(CLOCK, new AnnualRuleCatalog());

    for (AnnualContext context : contexts) {
      var assessment = assessor.assessYear(context, ReportTopic.CAREER);
      OverallTopicSnapshot snapshot = snapshot(
          snapshots, context.year(), OverallTopicSnapshot.Topic.CAREER);
      Set<String> available = Stream.concat(
              assessment.evidence().stream(), assessment.counterEvidence().stream())
          .map(evidence -> evidence.key())
          .collect(java.util.stream.Collectors.toSet());

      assertTrue(snapshot.focusKey().startsWith("career."), snapshot.focusKey());
      assertTrue(available.containsAll(snapshot.evidenceKeys()), snapshot.evidenceKeys().toString());
      assertTrue(assessment.ruleKeys().contains(snapshot.focusKey()));
    }
  }

  @Test
  void relationshipSnapshotUsesStatusFreeRelationshipEvaluation() {
    List<OverallTopicSnapshot> snapshots = new OverallSnapshotFactory(CLOCK)
        .create(request, chart, contexts);
    var period = new RelationshipPeriodArbitrator().arbitrate(
        new RelationshipDimensionEvaluator().evaluate(
            new RelationshipFactExtractor().extract(request, chart, contexts)));

    for (var year : period.years()) {
      OverallTopicSnapshot snapshot = snapshot(
          snapshots, year.year(), OverallTopicSnapshot.Topic.RELATIONSHIP);
      Set<String> available = new HashSet<>(year.focus().primaryEvidenceKeys());
      available.addAll(year.focus().secondaryEvidenceKeys());
      if (year.mainRisk() != null) available.addAll(year.mainRisk().evidenceKeys());

      assertTrue(snapshot.focusKey().startsWith(
          "relationship." + year.focus().primaryDimension().code() + "."));
      assertTrue(available.containsAll(snapshot.evidenceKeys()), snapshot.evidenceKeys().toString());
    }
  }

  @Test
  void semanticKeysNeverEncodeAnUnconfirmedUserStatus() {
    List<OverallTopicSnapshot> snapshots = new OverallSnapshotFactory(CLOCK)
        .create(request, chart, contexts);
    String keys = snapshots.stream()
        .flatMap(snapshot -> Stream.concat(
            Stream.of(snapshot.focusKey(), snapshot.opportunityKey(), snapshot.riskKey()),
            snapshot.actionCandidateKeys().stream()))
        .filter(java.util.Objects::nonNull)
        .collect(java.util.stream.Collectors.joining("|"));

    for (String forbidden : List.of(
        "employed", "business", "job_seeking", "studying", "single", "dating", "married")) {
      assertFalse(keys.contains(forbidden), forbidden + " found in " + keys);
    }
  }

  private OverallTopicSnapshot snapshot(
      List<OverallTopicSnapshot> snapshots,
      int year,
      OverallTopicSnapshot.Topic topic) {
    return snapshots.stream()
        .filter(snapshot -> snapshot.year() == year && snapshot.topic() == topic)
        .findFirst()
        .orElseThrow();
  }
}
