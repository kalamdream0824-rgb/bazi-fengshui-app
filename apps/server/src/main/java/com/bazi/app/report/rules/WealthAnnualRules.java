package com.bazi.app.report.rules;

import static com.bazi.app.report.rules.AnnualRuleFactory.any;
import static com.bazi.app.report.rules.AnnualRuleFactory.groups;
import static com.bazi.app.report.rules.AnnualRuleFactory.one;

import com.bazi.app.report.AnnualFindingType;
import com.bazi.app.report.AnnualRule;
import com.bazi.app.report.AnnualStage;
import com.bazi.app.report.ConfidenceLevel;
import com.bazi.app.report.ReportTopic;
import java.util.List;

public final class WealthAnnualRules {
  private WealthAnnualRules() {}

  static List<AnnualRule> create(AnnualRuleCopy copy) {
    AnnualRuleFactory rules = new AnnualRuleFactory(copy);
    return List.of(
        rules.rule("wealth.realization", ReportTopic.WEALTH, "income", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 90, ConfidenceLevel.HIGH,
            groups(one("annual.stem.group.wealth"), one("natal.balance.adequate"))),
        rules.rule("wealth.monetization", ReportTopic.WEALTH, "conversion", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 86, ConfidenceLevel.MEDIUM,
            groups(one("annual.stem.group.output"), one("natal.ten_god.group.wealth"))),
        rules.rule("wealth.cash_pressure", ReportTopic.WEALTH, "cashflow", AnnualFindingType.PRESSURE,
            AnnualStage.CAUTION, 92, ConfidenceLevel.HIGH,
            groups(one("annual.stem.group.wealth"), one("natal.balance.weak"))),
        rules.rule("wealth.competition", ReportTopic.WEALTH, "competition", AnnualFindingType.PRESSURE,
            AnnualStage.CAUTION, 84, ConfidenceLevel.MEDIUM,
            groups(one("annual.stem.group.peer"), one("natal.ten_god.group.wealth"))),
        rules.rule("wealth.contract_caution", ReportTopic.WEALTH, "contract", AnnualFindingType.PRESSURE,
            AnnualStage.CAUTION, 94, ConfidenceLevel.HIGH,
            groups(one("annual.branch.clash.*"), any("annual.stem.group.wealth", "natal.ten_god.group.wealth"))),
        rules.rule("wealth.compliance", ReportTopic.WEALTH, "compliance", AnnualFindingType.PRESSURE,
            AnnualStage.STABLE, 82, ConfidenceLevel.MEDIUM,
            groups(one("annual.stem.group.authority"), one("natal.ten_god.group.wealth"))),
        rules.rule("wealth.coordination", ReportTopic.WEALTH, "coordination", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 95, ConfidenceLevel.HIGH,
            groups(one("annual.branch.harmony.dayun"), one("annual.stem.group.wealth"))));
  }
}
