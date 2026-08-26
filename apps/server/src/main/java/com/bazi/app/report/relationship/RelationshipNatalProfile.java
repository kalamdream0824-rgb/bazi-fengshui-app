package com.bazi.app.report.relationship;

import com.bazi.app.report.TenGodGroup;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RelationshipNatalProfile(
    String genderCode,
    String dayMaster,
    String dayBranch,
    String balanceLevel,
    TenGodGroup spouseStarGroup,
    List<TenGodOccurrence> tenGodOccurrences,
    Map<TenGodGroup, Integer> groupCounts,
    List<RelationshipEvidence> evidence) {

  public RelationshipNatalProfile {
    Objects.requireNonNull(genderCode, "genderCode");
    Objects.requireNonNull(dayMaster, "dayMaster");
    Objects.requireNonNull(dayBranch, "dayBranch");
    Objects.requireNonNull(balanceLevel, "balanceLevel");
    Objects.requireNonNull(spouseStarGroup, "spouseStarGroup");
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
