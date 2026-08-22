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

public final class RelationshipAnnualRules {
  private RelationshipAnnualRules() {}

  static List<AnnualRule> create(AnnualRuleCopy copy) {
    AnnualRuleFactory rules = new AnnualRuleFactory(copy);
    return List.of(
        rules.rule("relationship.activation.spouse_star", ReportTopic.RELATIONSHIP, "activation", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 94, ConfidenceLevel.HIGH,
            groups(any("natal.gender.male", "natal.gender.female"), any("annual.stem.group.wealth", "annual.stem.group.authority"), one("natal.day.branch")),
            context -> context.factKeys().contains("natal.gender.male")
                ? context.factKeys().contains("annual.stem.group.wealth")
                : context.factKeys().contains("annual.stem.group.authority")),
        rules.rule("relationship.cooperation", ReportTopic.RELATIONSHIP, "cooperation", AnnualFindingType.OPPORTUNITY,
            AnnualStage.ADVANCE, 90, ConfidenceLevel.MEDIUM,
            groups(one("annual.branch.harmony.day"), one("natal.day.branch"))),
        rules.rule("relationship.boundary", ReportTopic.RELATIONSHIP, "boundary", AnnualFindingType.PRESSURE,
            AnnualStage.CAUTION, 92, ConfidenceLevel.HIGH,
            groups(any("annual.branch.clash.day", "annual.branch.harm.day", "annual.branch.punishment.day"), one("natal.day.branch"))),
        rules.rule("relationship.communication", ReportTopic.RELATIONSHIP, "communication", AnnualFindingType.OPPORTUNITY,
            AnnualStage.PREPARE, 82, ConfidenceLevel.MEDIUM,
            groups(any("annual.stem.group.resource", "annual.stem.group.output"), any("natal.ten_god.group.output", "natal.ten_god.group.resource"))),
        rules.rule("relationship.pace", ReportTopic.RELATIONSHIP, "pace", AnnualFindingType.PRESSURE,
            AnnualStage.CAUTION, 98, ConfidenceLevel.HIGH,
            groups(any("annual.stem.group.wealth", "annual.stem.group.authority"), any("annual.branch.clash.dayun", "annual.branch.punishment.dayun", "annual.branch.harm.dayun"))),
        rules.rule("relationship.context_support", ReportTopic.RELATIONSHIP, "context", AnnualFindingType.OPPORTUNITY,
            AnnualStage.STABLE, 96, ConfidenceLevel.HIGH,
            groups(one("annual.branch.harmony.*"), one("natal.day.branch"))),
        rules.rule("relationship.context_pressure", ReportTopic.RELATIONSHIP, "context", AnnualFindingType.PRESSURE,
            AnnualStage.CAUTION, 78, ConfidenceLevel.MEDIUM,
            groups(any("annual.branch.clash.*", "annual.branch.harm.*", "annual.branch.punishment.*"), one("natal.day.branch"))));
  }
}
