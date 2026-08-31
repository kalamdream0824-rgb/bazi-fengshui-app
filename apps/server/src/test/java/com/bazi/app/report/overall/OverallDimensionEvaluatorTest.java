package com.bazi.app.report.overall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class OverallDimensionEvaluatorTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"),
      ZoneId.of("Asia/Shanghai"));

  private final BaziService baziService = new BaziService();
  private final OverallDimensionEvaluator evaluator = new OverallDimensionEvaluator();

  @Test
  void evaluatesAllFourLifeDimensionsForEveryConfiguredYear() {
    for (PaipanRequest request : List.of(fixture1995(), fixture1996())) {
      for (AnnualContext context : contexts(request)) {
        List<OverallDimensionEvaluation> dimensions = evaluator.evaluate(context);

        assertEquals(OverallDimension.values().length, dimensions.size());
        assertEquals(OverallDimension.values().length,
            dimensions.stream().map(OverallDimensionEvaluation::dimension).distinct().count());
        assertTrue(dimensions.stream().allMatch(dimension ->
            !dimension.supportingEvidenceKeys().isEmpty()
                || !dimension.limitingEvidenceKeys().isEmpty()));
      }
    }
  }

  @Test
  void producesTraceableAndDeterministicEvaluation() {
    AnnualContext context = contexts(fixture1995()).get(0);

    List<OverallDimensionEvaluation> first = evaluator.evaluate(context);
    List<OverallDimensionEvaluation> second = evaluator.evaluate(context);

    assertEquals(first, second);
    assertTrue(first.stream().flatMap(item -> item.allEvidenceKeys().stream())
        .allMatch(context.factKeys()::contains));
    assertTrue(first.stream().allMatch(item -> item.salience() >= 0));
  }

  @Test
  void differentAnnualFactsCanChangeTheDimensionRanking() {
    List<OverallDimensionEvaluation> firstYear = evaluator.evaluate(contexts(fixture1995()).get(0));
    List<OverallDimensionEvaluation> secondYear = evaluator.evaluate(contexts(fixture1995()).get(1));

    assertNotEquals(
        firstYear.stream().map(OverallDimensionEvaluation::salience).toList(),
        secondYear.stream().map(OverallDimensionEvaluation::salience).toList());
    assertFalse(firstYear.stream().flatMap(item -> item.directAnnualEvidenceKeys().stream()).toList().isEmpty());
  }

  private List<AnnualContext> contexts(PaipanRequest request) {
    PaipanResultDto chart = baziService.paipan(request);
    return new AnnualContextFactory(CLOCK).create(request, chart, ReportHorizon.of(3));
  }

  private PaipanRequest fixture1995() {
    return new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
  }

  private PaipanRequest fixture1996() {
    return new PaipanRequest("周女士", "female", "1996-03-18T10:00:00", "上海", false);
  }
}
