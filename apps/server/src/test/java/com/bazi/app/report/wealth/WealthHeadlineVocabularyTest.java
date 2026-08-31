package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.report.wealth.v3.WealthHeadlineVocabulary;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WealthHeadlineVocabularyTest {

  @Test
  void catalogUsesClosedNonBlankSemanticKeys() {
    var entries = WealthHeadlineVocabulary.entries();
    assertFalse(entries.isEmpty());
    Set<String> signatures = new HashSet<>();
    for (var entry : entries) {
      for (String value : java.util.List.of(entry.themeKey(), entry.pathKey(), entry.subjectKey(),
          entry.angleKey(), entry.objectKey(), entry.text())) {
        assertFalse(value.isBlank(), entry.toString());
      }
      assertTrue(signatures.add(entry.themeKey()), entry.themeKey());
      assertFalse(entry.corePhraseKeys().isEmpty(), entry.toString());
      assertTrue(WealthHeadlineVocabulary.corePhraseKeys().containsAll(entry.corePhraseKeys()), entry.toString());
      assertTrue(WealthHeadlineVocabulary.pathKeys().contains(entry.pathKey()), entry.toString());
    }
  }

  @Test
  void everyPathAndUsableStanceHasLicensedCopy() {
    for (String path : WealthHeadlineVocabulary.pathKeys()) {
      for (String stance : Set.of("supportive", "mixed", "restricted")) {
        assertFalse(WealthHeadlineVocabulary.forDecision(path, stance).isEmpty(), path + ":" + stance);
      }
    }
  }

  @Test
  void everyHeadlineNamesItsLicensedConcreteObjectAndAvoidsBannedLanguage() {
    for (var entry : WealthHeadlineVocabulary.entries()) {
      assertNotNull(entry.objectText());
      assertFalse(entry.objectText().isBlank(), entry.toString());
      assertTrue(entry.text().contains(entry.objectText()), entry.toString());
      for (String banned : Set.of("现有证据不足", "先观察再判断", "卡点", "卡住", "承接", "交付", "兑现", "复盘",
          "第一年", "第二年", "第三年")) {
        assertFalse(entry.text().contains(banned), entry.text());
      }
    }
  }

  @Test
  void catalogOrderIsUniqueAndStable() {
    var orders = WealthHeadlineVocabulary.entries().stream().map(WealthHeadlineVocabulary.Entry::catalogOrder).toList();
    assertEquals(orders.size(), new HashSet<>(orders).size());
    assertEquals(orders.stream().sorted().toList(), orders);
  }
}
