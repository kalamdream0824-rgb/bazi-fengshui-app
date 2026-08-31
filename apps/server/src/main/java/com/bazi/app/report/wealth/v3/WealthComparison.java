package com.bazi.app.report.wealth.v3;

import java.util.List;

/** Internal comparison facts. The final API comparison additionally requires a reading block. */
public record WealthComparison(int toYear, String direction, List<Change> changes) {
  public WealthComparison { changes = List.copyOf(changes); }

  public record Change(String path, int supportDelta, int limitationDelta,
      List<String> addedEvidenceIds, List<String> removedEvidenceIds) {
    public Change {
      addedEvidenceIds = List.copyOf(addedEvidenceIds);
      removedEvidenceIds = List.copyOf(removedEvidenceIds);
    }
  }
}
