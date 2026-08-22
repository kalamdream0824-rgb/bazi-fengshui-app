package com.bazi.app.report.rules;

import com.bazi.app.report.AnnualRule;
import com.bazi.app.report.ReportTopic;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class AnnualRuleCatalog {

  private final AnnualRuleCopy copy = new AnnualRuleCopy();
  private final Map<ReportTopic, List<AnnualRule>> rules;

  public AnnualRuleCatalog() {
    Map<ReportTopic, List<AnnualRule>> catalog = new EnumMap<>(ReportTopic.class);
    catalog.put(ReportTopic.OVERALL, OverallAnnualRules.create(copy));
    catalog.put(ReportTopic.CAREER, CareerAnnualRules.create(copy));
    catalog.put(ReportTopic.WEALTH, WealthAnnualRules.create(copy));
    catalog.put(ReportTopic.RELATIONSHIP, RelationshipAnnualRules.create(copy));
    rules = Map.copyOf(catalog);
  }

  public List<AnnualRule> rulesFor(ReportTopic topic) {
    return rules.getOrDefault(topic, List.of());
  }

  public String copy(String key) {
    return copy.get(key);
  }
}
