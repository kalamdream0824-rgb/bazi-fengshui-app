package com.bazi.app.report.overall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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

class OverallPeriodArbitratorTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"),
      ZoneId.of("Asia/Shanghai"));

  @Test
  void givesAStrongPressurePriorityOverOrdinarySupport() {
    OverallDimensionEvaluation supportive = evaluation(
        OverallDimension.CAREER, OverallStance.SUPPORTIVE, 4, 0, "annual.stem.group.output");
    OverallDimensionEvaluation pressured = evaluation(
        OverallDimension.WEALTH, OverallStance.PRESSURED, 0, 7, "annual.branch.clash.dayun");

    OverallDimension selected = OverallPeriodArbitrator.selectPrimary(
        List.of(supportive, pressured), null);

    assertEquals(OverallDimension.WEALTH, selected);
  }

  @Test
  void usesAStableDimensionOrderToBreakExactTies() {
    OverallDimensionEvaluation rhythm = evaluation(
        OverallDimension.RHYTHM, OverallStance.SUPPORTIVE, 4, 0, "annual.stem.group.resource");
    OverallDimensionEvaluation career = evaluation(
        OverallDimension.CAREER, OverallStance.SUPPORTIVE, 4, 0, "annual.stem.group.output");

    assertEquals(OverallDimension.RHYTHM,
        OverallPeriodArbitrator.selectPrimary(List.of(career, rhythm), null));
  }

  @Test
  void changesARepeatedFocusOnlyWhenAnotherDirectAnnualSignalIsClose() {
    OverallDimensionEvaluation career = evaluation(
        OverallDimension.CAREER, OverallStance.SUPPORTIVE, 5, 0, "annual.stem.group.output");
    OverallDimensionEvaluation closeWealth = evaluation(
        OverallDimension.WEALTH, OverallStance.MIXED, 4, 1, "annual.stem.group.wealth");
    OverallDimensionEvaluation weakRelationship = evaluation(
        OverallDimension.RELATIONSHIP, OverallStance.BALANCED, 1, 0, "natal.balance.middle");

    assertEquals(OverallDimension.WEALTH, OverallPeriodArbitrator.selectPrimary(
        List.of(career, closeWealth, weakRelationship), OverallDimension.CAREER));
    assertEquals(OverallDimension.CAREER, OverallPeriodArbitrator.selectPrimary(
        List.of(career, weakRelationship), OverallDimension.CAREER));
  }

  @Test
  void buildsDeterministicThreeYearFocusAndTransitions() {
    PaipanRequest request = new PaipanRequest(
        "林先生", "male", "1995-10-08T14:30:00", "上海", false);
    PaipanResultDto chart = new BaziService().paipan(request);
    List<AnnualContext> contexts = new AnnualContextFactory(CLOCK)
        .create(request, chart, ReportHorizon.of(3));
    OverallPeriodArbitrator arbitrator = new OverallPeriodArbitrator();

    OverallPeriodEvaluation first = arbitrator.arbitrate(contexts);
    OverallPeriodEvaluation second = arbitrator.arbitrate(contexts);

    assertEquals(first, second);
    assertEquals(3, first.years().size());
    assertEquals(2, first.transitions().size());
    for (OverallYearEvaluation year : first.years()) {
      assertNotEquals(year.primaryDimension(), year.secondaryDimension());
      assertEquals(4, year.dimensions().size());
    }
  }

  private OverallDimensionEvaluation evaluation(
      OverallDimension dimension,
      OverallStance stance,
      int support,
      int limitation,
      String evidenceKey) {
    boolean pressured = limitation > support;
    List<String> supporting = pressured ? List.of() : List.of(evidenceKey);
    List<String> limiting = pressured ? List.of(evidenceKey) : List.of();
    List<String> direct = evidenceKey.startsWith("annual.") ? List.of(evidenceKey) : List.of();
    return new OverallDimensionEvaluation(
        dimension, stance, support, limitation, supporting, limiting, direct);
  }
}
