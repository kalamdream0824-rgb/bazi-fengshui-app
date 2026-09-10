package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.report.wealth.v3.WealthHeadlineVocabulary;
import com.bazi.app.report.wealth.v3.WealthSemanticClaim;
import com.bazi.app.report.wealth.v3.WealthSemanticClaimWriter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WealthSemanticClaimWriterTest {
  private static final List<String> PATHS = List.of(
      "stable_income", "skill_income", "project_income", "cooperation_income", "retention");
  private static final List<String> STANCES = List.of("supportive", "mixed", "restricted");
  private static final List<String> AWKWARD = List.of(
      "应先做到不能",
      "应先做到长期",
      "防止出现这种情况：更容易",
      "防范更容易");

  @Test
  void everyPathAndStanceHasEightReadableDeterministicConclusionVariants() {
    var writer = new WealthSemanticClaimWriter();
    for (String path : PATHS) {
      for (String stance : STANCES) {
        WealthSemanticClaim claim = claim(path, stance);
        Set<String> variants = new LinkedHashSet<>();
        for (int index = 0; index < 2_000 && variants.size() < 8; index++) {
          variants.add(writer.conclusion(claim, "corpus-seed-" + index));
        }
        assertEquals(8, variants.size(), path + " " + stance + " variants=" + variants);
        for (String text : variants) {
          assertTrue(AWKWARD.stream().noneMatch(text::contains), path + " " + stance + ": " + text);
        }
      }
    }
  }

  private WealthSemanticClaim claim(String path, String stance) {
    var entry = WealthHeadlineVocabulary.forDecision(path, stance).get(0);
    return new WealthSemanticClaim(2026, entry.themeKey(), path, stance, "supported",
        entry.objectKey(), entry.objectText(), "annual_direct", "generic", "decision." + path,
        List.of("evidence." + path), List.of(), List.of("evidence." + path),
        List.of("ANNUAL_TRIGGER:" + path));
  }
}
