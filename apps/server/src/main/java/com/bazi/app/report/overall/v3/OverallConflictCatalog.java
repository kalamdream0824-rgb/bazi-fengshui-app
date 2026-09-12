package com.bazi.app.report.overall.v3;

import java.util.List;

final class OverallConflictCatalog {

  String resolve(OverallTopicSnapshot primary, OverallTopicSnapshot secondary) {
    if (primary.stance() == OverallTopicSnapshot.Stance.PRESSURED) {
      if (primary.topic() == OverallTopicSnapshot.Topic.RHYTHM
          && secondary.topic() == OverallTopicSnapshot.Topic.CAREER) {
        return "capacity_before_career_expansion";
      }
      if (primary.topic() == OverallTopicSnapshot.Topic.WEALTH
          && secondary.topic() == OverallTopicSnapshot.Topic.CAREER) {
        return "cash_buffer_before_growth";
      }
      if (primary.topic() == OverallTopicSnapshot.Topic.RELATIONSHIP
          && secondary.topic() == OverallTopicSnapshot.Topic.CAREER) {
        return "relationship_stability_before_growth";
      }
      return code(primary) + "_before_" + code(secondary);
    }
    return code(primary) + "_with_" + code(secondary) + "_watch";
  }

  private String code(OverallTopicSnapshot snapshot) {
    return snapshot.topic().name().toLowerCase(java.util.Locale.ROOT);
  }

  int dependencyRank(
      OverallTopicSnapshot.Topic primary, OverallTopicSnapshot.Topic candidate) {
    List<OverallTopicSnapshot.Topic> order = switch (primary) {
      case RHYTHM -> List.of(
          OverallTopicSnapshot.Topic.CAREER,
          OverallTopicSnapshot.Topic.WEALTH,
          OverallTopicSnapshot.Topic.RELATIONSHIP);
      case CAREER -> List.of(
          OverallTopicSnapshot.Topic.WEALTH,
          OverallTopicSnapshot.Topic.RHYTHM,
          OverallTopicSnapshot.Topic.RELATIONSHIP);
      case WEALTH -> List.of(
          OverallTopicSnapshot.Topic.CAREER,
          OverallTopicSnapshot.Topic.RELATIONSHIP,
          OverallTopicSnapshot.Topic.RHYTHM);
      case RELATIONSHIP -> List.of(
          OverallTopicSnapshot.Topic.CAREER,
          OverallTopicSnapshot.Topic.RHYTHM,
          OverallTopicSnapshot.Topic.WEALTH);
    };
    int rank = order.indexOf(candidate);
    return rank < 0 ? order.size() : rank;
  }
}
