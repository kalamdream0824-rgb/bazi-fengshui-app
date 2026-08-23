package com.bazi.app.report.wealth;

import com.bazi.app.report.TenGodGroup;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record WealthNatalProfile(
    String dayMaster,
    String balanceLevel,
    List<TenGodOccurrence> tenGodOccurrences,
    Map<TenGodGroup, Integer> groupCounts,
    List<WealthEvidence> evidence) {

  public WealthNatalProfile {
    Objects.requireNonNull(dayMaster, "dayMaster");
    Objects.requireNonNull(balanceLevel, "balanceLevel");
    tenGodOccurrences = tenGodOccurrences == null ? List.of() : List.copyOf(tenGodOccurrences);
    groupCounts = groupCounts == null ? Map.of() : Map.copyOf(groupCounts);
    evidence = evidence == null ? List.of() : List.copyOf(evidence);
  }

  public record TenGodOccurrence(
      String tenGod,
      TenGodGroup group,
      String position,
      boolean visible) {

    public TenGodOccurrence {
      Objects.requireNonNull(tenGod, "tenGod");
      Objects.requireNonNull(group, "group");
      Objects.requireNonNull(position, "position");
    }
  }
}
