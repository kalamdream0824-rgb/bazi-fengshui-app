package com.bazi.app.report;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnnualTopicRulesTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"),
      ZoneId.of("Asia/Shanghai"));

  private BaziService baziService;
  private AnnualRuleCatalog catalog;
  private ThreeYearAssessor assessor;

  @BeforeEach
  void setUp() {
    baziService = new BaziService();
    catalog = new AnnualRuleCatalog();
    assessor = new ThreeYearAssessor(CLOCK, catalog);
  }

  @Test
  void shipsFiveToSevenReviewedRulesForEveryLaunchTopic() {
    for (ReportTopic topic : ReportTopic.values()) {
      int size = catalog.rulesFor(topic).size();
      assertTrue(size >= 5 && size <= 7, topic + " rules=" + size);
    }
  }

  @Test
  void everyRuleHasSeparateResolvablePlainAndProfessionalCopyKeys() {
    AnnualContext context = contexts(fixture1995()).get(0);
    for (ReportTopic topic : ReportTopic.values()) {
      for (AnnualRule rule : catalog.rulesFor(topic)) {
        AnnualRuleResult draft = rule.build(context);
        assertNotEquals(draft.plainCopyKey(), draft.professionalCopyKey());
        assertFalse(catalog.copy(draft.plainCopyKey()).isBlank());
        assertFalse(catalog.copy(draft.professionalCopyKey()).isBlank());
        assertFalse(draft.realitySignals().isEmpty());
      }
    }
  }

  @Test
  void fourTopicsProduceDifferentRulePathsForTheSameThreeYears() {
    Map<ReportTopic, ThreeYearAssessment> reports = assessAllTopics(fixture1995());

    assertEquals(4, reports.size());
    assertEquals(4, reports.values().stream()
        .map(report -> report.years().stream()
            .flatMap(year -> year.ruleKeys().stream())
            .collect(joining("|")))
        .distinct()
        .count());
  }

  @Test
  void noTopicRepeatsTheSameHeadlineOrEvidenceAcrossThreeYears() {
    for (ReportTopic topic : ReportTopic.values()) {
      ThreeYearAssessment report = assess(fixture1995(), topic);
      assertEquals(3, report.years().stream().map(YearAssessment::headline).distinct().count(), topic.name());
      assertEquals(3, report.years().stream()
          .map(year -> year.evidence().stream().map(ReportEvidence::key).sorted().toList())
          .distinct()
          .count(), topic.name());
    }
  }

  @Test
  void aSecondNatalChartChangesAtLeastTwoCareerYears() {
    ThreeYearAssessment male = assess(fixture1995(), ReportTopic.CAREER);
    ThreeYearAssessment female = assess(fixture1996(), ReportTopic.CAREER);

    long differences = 0;
    for (int index = 0; index < 3; index++) {
      YearAssessment left = male.years().get(index);
      YearAssessment right = female.years().get(index);
      if (!left.headline().equals(right.headline()) || left.stage() != right.stage()) {
        differences++;
      }
    }
    assertTrue(differences >= 2, "different years=" + differences);
  }

  @Test
  void relationshipActivationUsesGenderSpecificSpouseStarWithoutEventClaims() {
    ThreeYearAssessment male = assess(fixture1995(), ReportTopic.RELATIONSHIP);
    ThreeYearAssessment female = assess(fixture1996(), ReportTopic.RELATIONSHIP);

    assertTrue(male.years().stream().limit(2)
        .flatMap(year -> year.ruleKeys().stream())
        .anyMatch(key -> key.equals("relationship.activation.spouse_star")));
    assertFalse(female.years().stream()
        .flatMap(year -> year.ruleKeys().stream())
        .anyMatch(key -> key.equals("relationship.activation.spouse_star")));
    String text = flatten(male) + flatten(female);
    for (String forbidden : List.of("结婚", "离婚", "分手", "出轨", "婚期")) {
      assertFalse(text.contains(forbidden), forbidden);
    }
  }

  @Test
  void exportsTwelveReviewedResultsForContentReview() throws Exception {
    Map<String, Object> debug = new LinkedHashMap<>();
    debug.put("fixture", "male-1995-10-08-1430");
    debug.put("asOf", "2026-08-23");
    Map<String, ThreeYearAssessment> topics = new LinkedHashMap<>();
    for (ReportTopic topic : ReportTopic.values()) {
      topics.put(topic.code(), assess(fixture1995(), topic));
    }
    debug.put("topics", topics);

    Path output = Path.of("target", "report-debug", "annual-topic-rules-1995.json");
    Files.createDirectories(output.getParent());
    new ObjectMapper()
        .findAndRegisterModules()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .writeValue(output.toFile(), debug);

    assertTrue(Files.size(output) > 1000);
    assertEquals(12, topics.values().stream().mapToInt(report -> report.years().size()).sum());
  }

  private Map<ReportTopic, ThreeYearAssessment> assessAllTopics(PaipanRequest request) {
    Map<ReportTopic, ThreeYearAssessment> reports = new EnumMap<>(ReportTopic.class);
    for (ReportTopic topic : ReportTopic.values()) {
      reports.put(topic, assess(request, topic));
    }
    return reports;
  }

  private ThreeYearAssessment assess(PaipanRequest request, ReportTopic topic) {
    return assessor.assess(request, baziService.paipan(request), topic);
  }

  private List<AnnualContext> contexts(PaipanRequest request) {
    PaipanResultDto chart = baziService.paipan(request);
    return new AnnualContextFactory(CLOCK).create(request, chart);
  }

  private PaipanRequest fixture1995() {
    return new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
  }

  private PaipanRequest fixture1996() {
    return new PaipanRequest("周女士", "female", "1996-03-18T10:00:00", "上海", false);
  }

  private String flatten(ThreeYearAssessment report) {
    return report.years().stream()
        .map(year -> year.headline() + year.conclusion()
            + String.join("", year.actions()) + String.join("", year.realitySignals()))
        .collect(joining());
  }
}
