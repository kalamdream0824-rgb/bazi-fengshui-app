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

public final class CareerAnnualRules {
  private CareerAnnualRules() {}

  static List<AnnualRule> create(AnnualRuleCopy copy) {
    AnnualRuleFactory rules = new AnnualRuleFactory(copy);
    return List.of(
        rules.rule("career.role_transition", ReportTopic.CAREER, "role", AnnualFindingType.PRESSURE,
            AnnualStage.TRANSITION, 96, ConfidenceLevel.HIGH,
            groups(any("annual.branch.clash.dayun", "annual.branch.punishment.dayun"), one("annual.stem.group.*"))),
        rules.rule("career.coordination_window", ReportTopic.CAREER, "coordination", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 94, ConfidenceLevel.HIGH,
            groups(one("annual.branch.harmony.dayun"), any("annual.stem.group.wealth", "annual.stem.group.authority", "annual.stem.group.output"))),
        rules.rule("career.responsibility_upgrade", ReportTopic.CAREER, "responsibility", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 92, ConfidenceLevel.HIGH,
            groups(one("annual.stem.group.authority"), any("natal.ten_god.group.resource", "natal.balance.adequate"))),
        rules.rule("career.visibility", ReportTopic.CAREER, "visibility", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 88, ConfidenceLevel.MEDIUM,
            groups(one("annual.stem.group.output"), any("natal.ten_god.group.output", "natal.ten_god.group.resource"))),
        rules.rule("career.preparation", ReportTopic.CAREER, "preparation", AnnualFindingType.OPPORTUNITY,
            AnnualStage.PREPARE, 80, ConfidenceLevel.MEDIUM,
            groups(one("annual.stem.group.resource"), any("next.annual.stem.group.output", "next.annual.stem.group.wealth", "next.annual.stem.group.authority"))),
        rules.rule("career.resource_delivery", ReportTopic.CAREER, "delivery", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 78, ConfidenceLevel.MEDIUM,
            groups(one("annual.stem.group.wealth"), one("natal.ten_god.group.output"), one("natal.balance.adequate"))),
        rules.rule("career.overextension", ReportTopic.CAREER, "load", AnnualFindingType.PRESSURE,
            AnnualStage.CAUTION, 86, ConfidenceLevel.HIGH,
            groups(any("annual.stem.group.output", "annual.stem.group.wealth"), one("natal.balance.weak"), one("annual.branch.*"))));
  }
}
