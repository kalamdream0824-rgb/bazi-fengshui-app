package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.bazi.app.report.wealth.v3.WealthHeadlineSentenceWriter;
import com.bazi.app.report.wealth.v3.WealthHeadlineVocabulary;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class WealthHeadlineSentenceSpecTest {

  @Test
  void mixedRetentionNamesTheExpectedChangeTheLimitationAndTheCheck() {
    var entry = WealthHeadlineVocabulary.entry("retention_variable_spending");

    assertEquals("今年存钱进度有改善机会，但零散花费也会增加。先确认临时开销预留的金额。", entry.text());
    assertEquals("今年存钱进度", entry.sentenceSpec().subject());
    assertEquals("有改善机会", entry.sentenceSpec().judgment());
    assertEquals("零散花费也会增加", entry.sentenceSpec().limitation());
    assertEquals("确认临时开销预留的金额", entry.sentenceSpec().verification());
    assertEquals(entry.text(), new WealthHeadlineSentenceWriter().write(entry.sentenceSpec()));
  }

  @Test
  void frequentThemesDoNotEchoTheSamePhraseAcrossBothSentences() {
    assertEquals(
        "今年月底能留下的钱可能增多，但新增花费也可能消耗结余。先设好日常开销的最高金额。",
        WealthHeadlineVocabulary.entry("retention_expense_limit").text());
    assertEquals(
        "今年新增投入带来的收入较可能同步提高。先比较相同投入能否再次产生进账。",
        WealthHeadlineVocabulary.entry("skill_repeat_payment").text());
    assertEquals(
        "今年一起安排资金能让用钱更方便，但同时可能增加责任和花费。先写清共同承担的开销。",
        WealthHeadlineVocabulary.entry("cooperation_shared_expense").text());
  }

  @Test
  void everyHeadlineUsesTwoCompleteSentencesWithoutAbstractGlue() {
    assertEquals("wealth-headline-v3", WealthHeadlineVocabulary.VERSION);
    List<String> forbidden = List.of(
        "有利条件", "好坏条件", "改善空间", "存在变数", "有变数",
        "值得重点留意", "同时记录", "这方面");

    for (var entry : WealthHeadlineVocabulary.entries()) {
      assertEquals(entry.stance(), entry.sentenceSpec().tone(), entry.themeKey());
      assertEquals(entry.text(), new WealthHeadlineSentenceWriter().write(entry.sentenceSpec()),
          entry.themeKey());
      assertEquals(2, List.of(entry.text().split("。")).stream()
          .filter(sentence -> !sentence.isBlank()).count(), entry.themeKey() + ": " + entry.text());
      assertFalse(entry.sentenceSpec().subject().isBlank(), entry.themeKey());
      assertFalse(entry.sentenceSpec().judgment().isBlank(), entry.themeKey());
      assertFalse(entry.sentenceSpec().verification().isBlank(), entry.themeKey());
      for (String phrase : forbidden) {
        assertFalse(entry.text().contains(phrase), entry.themeKey() + ": " + entry.text());
      }
    }
  }

  @Test
  void everyThemeWithinOneMoneyPathUsesItsOwnVisibleCheckObject() {
    Map<String, List<WealthHeadlineVocabulary.Entry>> byPath =
        WealthHeadlineVocabulary.entries().stream()
            .collect(Collectors.groupingBy(WealthHeadlineVocabulary.Entry::pathKey));

    for (var path : byPath.entrySet()) {
      assertEquals(path.getValue().size(), path.getValue().stream()
          .map(WealthHeadlineVocabulary.Entry::objectText).distinct().count(),
          () -> path.getKey() + " reuses visible objects: " + path.getValue().stream()
              .map(entry -> entry.themeKey() + "=" + entry.objectText()).toList());
    }
  }
}
