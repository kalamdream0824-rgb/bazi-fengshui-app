package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.*;

import com.bazi.app.report.wealth.v3.WealthAssessment;
import com.bazi.app.report.wealth.v3.WealthRetrospectivePlan;
import com.bazi.app.report.wealth.v3.WealthRetrospectivePlan.Citation;
import com.bazi.app.report.wealth.v3.WealthRetrospectivePlan.Observation;
import java.util.List;
import org.junit.jupiter.api.Test;

class WealthRetrospectivePlanTest {
  static Citation citation(String id, String path, String key, int weight, String value) {
    return new Citation(new WealthAssessment.Evidence(id, path, "rule." + key, key,
        EvidenceFamily.ANNUAL_TRIGGER, List.of("root." + key), weight),
        List.of(new WealthAssessment.Fact("root." + key, "annual", key, value)));
  }

  static Observation observation(String subject, String angle, Citation... sources) {
    return new Observation(subject, "supportive", "supported", angle,
        List.of(sources), List.of(sources[0].evidence().id()));
  }

  @Test void reorderedAndDuplicatedCitationsHaveTheSameSignature() {
    var a = citation("a", "stable_income", "annual.stem.ten_god", 3, "正财");
    var b = citation("b", "stable_income", "annual.branch.harmony.day", 2, "巳申");
    var first = new Observation("stable_income", "supportive", "supported", "annual_stem",
        List.of(a, b, a), List.of("a", "a"));
    var second = new Observation("stable_income", "supportive", "supported", "annual_stem",
        List.of(b, a), List.of("a"));
    assertEquals(plan(first).evidenceSignature(), plan(second).evidenceSignature());
    assertEquals(plan(first), plan(second));
  }

  @Test void rootValueAndEvidencePolarityAffectSignature() {
    var a = citation("a", "stable_income", "annual.stem.ten_god", 3, "正财");
    var b = citation("a", "stable_income", "annual.stem.ten_god", 3, "偏财");
    var c = citation("a", "stable_income", "annual.stem.ten_god", -3, "正财");
    assertNotEquals(plan(observation("stable_income", "annual_stem", a)).evidenceSignature(),
        plan(observation("stable_income", "annual_stem", b)).evidenceSignature());
    var restricted = new Observation("stable_income", "restricted", "limited", "annual_stem",
        List.of(c), List.of("a"));
    assertNotEquals(plan(observation("stable_income", "annual_stem", a)).evidenceSignature(),
        plan(restricted).evidenceSignature());
  }

  @Test void judgmentDirectionAndAngleArePartOfSignature() {
    var c = citation("a", "stable_income", "annual.stem.ten_god", 3, "正财");
    var first = observation("stable_income", "annual_stem", c);
    var other = observation("stable_income", "annual_harmony", c);
    assertNotEquals(plan(first).evidenceSignature(), plan(other).evidenceSignature());
    var weaker = new Observation("stable_income", "supportive", "limited", "annual_stem",
        List.of(c), List.of("a"));
    assertNotEquals(plan(first).evidenceSignature(), plan(weaker).evidenceSignature());
  }

  @Test void repeatedSubjectsAndAnglesCannotFillSecondarySlot() {
    var primary = observation("stable_income", "annual_stem",
        citation("a", "stable_income", "annual.stem.ten_god", 3, "正财"));
    assertThrows(IllegalArgumentException.class, () -> new WealthRetrospectivePlan(2025,
        primary, primary, null));
    var sameAngle = observation("skill_income", "annual_stem",
        citation("b", "skill_income", "annual.stem.ten_god", 3, "食神"));
    assertThrows(IllegalArgumentException.class, () -> new WealthRetrospectivePlan(2025,
        primary, sameAngle, null));
  }

  @Test void unsupportedSlotsAreExplicitlyAbsentAndNeverFabricated() {
    var empty = new WealthRetrospectivePlan(2025, null, null, null);
    assertEquals("empty", empty.completeness());
    assertEquals(List.of("primary", "secondary", "hidden"), empty.missingSlots());
    var primary = observation("stable_income", "annual_stem",
        citation("a", "stable_income", "annual.stem.ten_god", 3, "正财"));
    assertEquals("partial", plan(primary).completeness());
    assertThrows(IllegalArgumentException.class,
        () -> new WealthRetrospectivePlan(2025, null, primary, null));
  }

  @Test void observationsRequireKnownFieldsAndTraceableDominantEvidence() {
    var c = citation("a", "stable_income", "annual.stem.ten_god", 3, "正财");
    assertThrows(IllegalArgumentException.class,
        () -> observation("", "annual_stem", c));
    assertThrows(IllegalArgumentException.class, () -> new Observation("stable_income",
        "supportive", "supported", "annual_stem", List.of(), List.of()));
    assertThrows(IllegalArgumentException.class, () -> new Observation("stable_income",
        "supportive", "supported", "annual_stem", List.of(c), List.of("missing")));
    assertThrows(IllegalArgumentException.class, () -> new Citation(c.evidence(), List.of()));
  }

  @Test void sameSubjectHiddenAngleMustHaveIndependentRoots() {
    var a = citation("a", "retention", "annual.stem.ten_god", 3, "正财");
    var primary = observation("retention", "annual_stem", a);
    var duplicated = observation("retention", "annual_harmony", a);
    assertThrows(IllegalArgumentException.class,
        () -> new WealthRetrospectivePlan(2025, primary, null, duplicated));
    var hidden = observation("retention", "annual_harmony",
        citation("b", "retention", "annual.branch.harmony.day", 2, "巳申"));
    assertEquals("partial", new WealthRetrospectivePlan(2025, primary, null, hidden).completeness());
  }

  @Test void renamingTransportIdsDoesNotInventSemanticDifferences() {
    var c = citation("a", "stable_income", "annual.stem.ten_god", 3, "正财");
    var renamed = new Citation(new WealthAssessment.Evidence("alias", "stable_income",
        c.evidence().ruleKey(), c.evidence().factKey(), c.evidence().family(), List.of("alias-root"), 3),
        List.of(new WealthAssessment.Fact("alias-root", "annual", "annual.stem.ten_god", "正财")));
    assertEquals(plan(observation("stable_income", "annual_stem", c)).evidenceSignature(),
        plan(observation("stable_income", "annual_stem", renamed)).evidenceSignature());
  }

  private WealthRetrospectivePlan plan(Observation primary) {
    return new WealthRetrospectivePlan(2025, primary, null, null);
  }
}
