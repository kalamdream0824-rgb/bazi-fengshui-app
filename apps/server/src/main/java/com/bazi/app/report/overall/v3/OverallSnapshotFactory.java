package com.bazi.app.report.overall.v3;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualPeriodAssessor;
import com.bazi.app.report.rules.AnnualRuleCatalog;
import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class OverallSnapshotFactory {

  private final Clock clock;

  public OverallSnapshotFactory(Clock clock) {
    if (clock == null) throw new IllegalArgumentException("overall snapshot clock is required");
    this.clock = clock;
  }

  public List<OverallTopicSnapshot> create(
      PaipanRequest request,
      PaipanResultDto chart,
      List<AnnualContext> contexts) {
    if (request == null || chart == null || contexts == null || contexts.isEmpty()) {
      throw new IllegalArgumentException("overall snapshot inputs are required");
    }
    Map<OverallTopicSnapshot.Topic, List<OverallTopicSnapshot>> byTopic =
        new EnumMap<>(OverallTopicSnapshot.Topic.class);
    byTopic.put(OverallTopicSnapshot.Topic.RHYTHM,
        new RhythmOverallSnapshotAdapter().adapt(contexts));
    byTopic.put(OverallTopicSnapshot.Topic.CAREER,
        new CareerOverallSnapshotAdapter(new AnnualPeriodAssessor(clock, new AnnualRuleCatalog()))
            .adapt(contexts));
    byTopic.put(OverallTopicSnapshot.Topic.WEALTH,
        new WealthOverallSnapshotAdapter().adapt(chart, contexts));
    byTopic.put(OverallTopicSnapshot.Topic.RELATIONSHIP,
        new RelationshipOverallSnapshotAdapter().adapt(request, chart, contexts));

    List<OverallTopicSnapshot> result = new ArrayList<>();
    for (int index = 0; index < contexts.size(); index++) {
      int year = contexts.get(index).year();
      for (OverallTopicSnapshot.Topic topic : OverallTopicSnapshot.Topic.values()) {
        OverallTopicSnapshot snapshot = byTopic.get(topic).get(index);
        if (snapshot.year() != year) {
          throw new IllegalArgumentException("overall snapshot years do not align");
        }
        result.add(snapshot);
      }
    }
    return List.copyOf(result);
  }
}
