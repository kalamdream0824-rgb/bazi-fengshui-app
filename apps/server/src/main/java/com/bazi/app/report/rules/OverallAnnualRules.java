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

public final class OverallAnnualRules {
  private OverallAnnualRules() {}

  static List<AnnualRule> create(AnnualRuleCopy copy) {
    AnnualRuleFactory rules = new AnnualRuleFactory(copy);
    return List.of(
        rules.rule("overall.restore_support", ReportTopic.OVERALL, "support", AnnualFindingType.OPPORTUNITY,
            AnnualStage.PREPARE, 84, ConfidenceLevel.HIGH,
            groups(any("annual.stem.group.resource", "annual.stem.group.peer"), any("natal.balance.weak", "natal.balance.middle"))),
        rules.rule("overall.express_advance", ReportTopic.OVERALL, "activation", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 82, ConfidenceLevel.HIGH,
            groups(one("annual.stem.group.output"), one("natal.balance.adequate"))),
        rules.rule("overall.resource_stewardship", ReportTopic.OVERALL, "resources", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 80, ConfidenceLevel.MEDIUM,
            groups(one("annual.stem.group.wealth"), one("natal.balance.adequate"))),
        rules.rule("overall.responsibility_capacity", ReportTopic.OVERALL, "responsibility", AnnualFindingType.PRESSURE,
            AnnualStage.STABLE, 78, ConfidenceLevel.MEDIUM,
            groups(one("annual.stem.group.authority"), one("natal.balance.adequate"))),
        rules.rule("overall.load_pressure", ReportTopic.OVERALL, "load", AnnualFindingType.PRESSURE,
            AnnualStage.CAUTION, 86, ConfidenceLevel.HIGH,
            groups(any("annual.stem.group.authority", "annual.stem.group.wealth"), one("natal.balance.weak"))),
        rules.rule("overall.coordination", ReportTopic.OVERALL, "coordination", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 92, ConfidenceLevel.HIGH,
            groups(one("annual.branch.harmony.dayun"), one("annual.stem.group.*"))),
        rules.rule("overall.transition", ReportTopic.OVERALL, "transition", AnnualFindingType.PRESSURE,
            AnnualStage.TRANSITION, 96, ConfidenceLevel.HIGH,
            groups(any("annual.branch.clash.dayun", "annual.branch.punishment.dayun", "annual.branch.harm.dayun"), one("dayun.stem.group.*"))));
  }
}
