package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.EvidenceFamily;
import java.util.List;

/** Internal annual judgment, not a ready report. Copy, comparison and persistence follow later. */
public record WealthAssessment(
    int year, String ganZhi, List<Fact> facts, List<Evidence> evidence,
    List<Decision> decisions, Focus focus, Risk risk) {
  public WealthAssessment {
    facts = List.copyOf(facts);
    evidence = List.copyOf(evidence);
    decisions = List.copyOf(decisions);
  }

  public record Fact(String id, String kind, String code, String value) {}

  public record Evidence(String id, String path, String ruleKey, String factKey,
      EvidenceFamily family, List<String> rootFactIds, int weight) {
    public Evidence { rootFactIds = List.copyOf(rootFactIds); }
  }

  public record Decision(String id, int year, String path,
      int supportWeight, int limitationWeight, int netWeight, String stance, String strength,
      List<String> supportingEvidenceIds, List<String> limitingEvidenceIds, List<String> reasonCodes) {
    public Decision {
      supportingEvidenceIds = List.copyOf(supportingEvidenceIds);
      limitingEvidenceIds = List.copyOf(limitingEvidenceIds);
      reasonCodes = List.copyOf(reasonCodes);
    }
  }

  public record Focus(String state, List<String> primaryCandidates, List<String> secondaryCandidates) {
    public Focus {
      primaryCandidates = List.copyOf(primaryCandidates);
      secondaryCandidates = List.copyOf(secondaryCandidates);
    }
  }

  /** The future narrative adds its reading block; this selection alone is not API-ready risk content. */
  public record Risk(String path, List<String> limitingEvidenceIds) {
    public Risk { limitingEvidenceIds = List.copyOf(limitingEvidenceIds); }
  }
}
