package com.bazi.app.report.overall;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class OverallEvidenceAngleResolver {

  public OverallEvidenceAngle resolve(OverallDimensionEvaluation evaluation) {
    Objects.requireNonNull(evaluation, "evaluation");
    List<String> preferred = switch (evaluation.stance()) {
      case PRESSURED, MIXED -> evaluation.limitingEvidenceKeys();
      case SUPPORTIVE, BALANCED -> evaluation.supportingEvidenceKeys();
    };
    List<String> opposite = switch (evaluation.stance()) {
      case PRESSURED, MIXED -> evaluation.supportingEvidenceKeys();
      case SUPPORTIVE, BALANCED -> evaluation.limitingEvidenceKeys();
    };
    String primary = preferred.stream()
        .max(Comparator.comparingInt(this::priority).thenComparing(Comparator.naturalOrder()))
        .orElseGet(() -> evaluation.allEvidenceKeys().get(0));
    String secondary = opposite.stream()
        .max(Comparator.comparingInt(this::priority).thenComparing(Comparator.naturalOrder()))
        .orElseGet(() -> evaluation.allEvidenceKeys().stream()
            .filter(key -> !key.equals(primary))
            .max(Comparator.comparingInt(this::priority).thenComparing(Comparator.naturalOrder()))
            .orElse(""));
    return new OverallEvidenceAngle(primary, secondary);
  }

  private int priority(String key) {
    if (key.startsWith("annual.branch.clash.")) return 900 + scopePriority(key);
    if (key.startsWith("annual.branch.punishment.")) return 800 + scopePriority(key);
    if (key.startsWith("annual.branch.harm.")) return 700 + scopePriority(key);
    if (key.startsWith("annual.branch.break.")) return 600 + scopePriority(key);
    if (key.startsWith("annual.stem.group.")) return 550;
    if (key.startsWith("annual.branch.harmony.")) return 500 + scopePriority(key);
    if (key.startsWith("dayun.stem.group.")) return 300;
    if (key.startsWith("natal.balance.")) return 100;
    return 0;
  }

  private int scopePriority(String key) {
    if (key.endsWith(".dayun")) return 50;
    if (key.endsWith(".day")) return 40;
    if (key.endsWith(".month")) return 30;
    if (key.endsWith(".time")) return 20;
    if (key.endsWith(".year")) return 10;
    return 0;
  }
}
