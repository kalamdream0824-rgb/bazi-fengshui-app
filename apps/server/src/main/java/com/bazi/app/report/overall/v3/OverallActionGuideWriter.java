package com.bazi.app.report.overall.v3;

import com.bazi.app.report.AnnualActionGuide;
import java.util.Locale;

/** Turns a semantic cross-topic decision into one observable action loop. */
final class OverallActionGuideWriter {

  private final OverallV3CopyCatalog catalog = new OverallV3CopyCatalog();

  AnnualActionGuide write(OverallAnnualDecision decision) {
    if (decision == null) {
      throw new IllegalArgumentException("overall decision must not be null");
    }
    OverallV3CopyCatalog.Copy copy = catalog.find(decision);
    AnnualActionGuide guide = new AnnualActionGuide(
        focusKey(decision),
        copy.problem(),
        copy.action(),
        copy.expectedChange(),
        copy.checkTiming(),
        copy.successSignal(),
        copy.adjustmentCondition(),
        copy.fallbackAction(),
        decision.evidenceKeys());
    OverallV3NarrativePolicy.validate(java.util.List.of(guide));
    return guide;
  }

  private String focusKey(OverallAnnualDecision decision) {
    return "overall."
        + decision.primary().topic().name().toLowerCase(Locale.ROOT)
        + "."
        + decision.secondary().topic().name().toLowerCase(Locale.ROOT)
        + "."
        + decision.conflictKey();
  }
}
