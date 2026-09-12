package com.bazi.app.report.overall.v3;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Injectable boundary for calculating and validating a complete overall v3 snapshot. */
@Component
public class OverallV3ReportGenerator {

  public OverallV3NarrativePlan generate(
      PaipanRequest request,
      PaipanResultDto chart,
      AnnualContext previous,
      List<AnnualContext> productYears,
      Clock clock) {
    if (previous == null || productYears == null || productYears.isEmpty()) {
      throw new IllegalArgumentException("overall v3 analysis window is required");
    }
    List<AnnualContext> fullWindow = new ArrayList<>();
    fullWindow.add(previous);
    fullWindow.addAll(productYears);

    List<OverallTopicSnapshot> snapshots = new OverallSnapshotFactory(clock)
        .create(request, chart, fullWindow);
    List<OverallAnnualDecision> decisions = new OverallDecisionArbitrator()
        .arbitratePeriod(snapshots);
    List<OverallTopicSnapshot> previousSnapshots = snapshots.stream()
        .filter(snapshot -> snapshot.year() == previous.year())
        .toList();
    List<OverallTopicSnapshot> productSnapshots = snapshots.stream()
        .filter(snapshot -> snapshot.year() != previous.year())
        .toList();
    return new OverallV3NarrativePlanner().plan(
        productSnapshots,
        decisions.subList(1, decisions.size()),
        previousSnapshots,
        decisions.get(0));
  }
}
