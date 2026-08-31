package com.bazi.app.report.overall;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class OverallEvidenceAngleResolverTest {

  private final OverallEvidenceAngleResolver resolver = new OverallEvidenceAngleResolver();

  @Test
  void pressuredReadingExplainsAnnualDisruptionBeforeBackgroundSupport() {
    OverallDimensionEvaluation evaluation = evaluation(
        OverallStance.PRESSURED,
        List.of("natal.balance.middle", "annual.stem.group.resource"),
        List.of("annual.branch.harm.month", "annual.branch.clash.dayun"));

    OverallEvidenceAngle angle = resolver.resolve(evaluation);

    assertEquals("annual.branch.clash.dayun", angle.primaryKey());
    assertEquals("annual.stem.group.resource", angle.secondaryKey());
  }

  @Test
  void supportiveReadingKeepsAnnualFunctionAndUsesDisruptionAsQualifier() {
    OverallDimensionEvaluation evaluation = evaluation(
        OverallStance.SUPPORTIVE,
        List.of("natal.balance.strong", "annual.stem.group.output", "annual.branch.harmony.time"),
        List.of("annual.branch.harm.day"));

    OverallEvidenceAngle angle = resolver.resolve(evaluation);

    assertEquals("annual.stem.group.output", angle.primaryKey());
    assertEquals("annual.branch.harm.day", angle.secondaryKey());
  }

  @Test
  void mixedReadingKeepsOneSupportAndOneLimitation() {
    OverallDimensionEvaluation evaluation = evaluation(
        OverallStance.MIXED,
        List.of("natal.balance.middle", "annual.branch.harmony.day"),
        List.of("annual.branch.punishment.month"));

    OverallEvidenceAngle angle = resolver.resolve(evaluation);

    assertEquals("annual.branch.punishment.month", angle.primaryKey());
    assertEquals("annual.branch.harmony.day", angle.secondaryKey());
  }

  private OverallDimensionEvaluation evaluation(
      OverallStance stance, List<String> supports, List<String> limitations) {
    return new OverallDimensionEvaluation(
        OverallDimension.RELATIONSHIP,
        stance,
        supports.size() * 2,
        limitations.size() * 2,
        supports,
        limitations,
        java.util.stream.Stream.concat(supports.stream(), limitations.stream())
            .filter(key -> key.startsWith("annual."))
            .toList());
  }
}
