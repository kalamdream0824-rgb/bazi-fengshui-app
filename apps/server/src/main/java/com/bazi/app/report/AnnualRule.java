package com.bazi.app.report;

import java.util.List;

public interface AnnualRule {

  String key();

  ReportTopic topic();

  int priority();

  boolean matches(AnnualContext context);

  List<AnnualFact> supportingEvidence(AnnualContext context);

  List<AnnualFact> counterEvidence(AnnualContext context);

  AnnualRuleResult build(AnnualContext context);
}
