package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bazi.app.config.BusinessException;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReportTopicTest {

  @Test
  void acceptsSupportedTopicsAndRejectsUnknownValues() {
    assertEquals(ReportTopic.CAREER, ReportTopic.fromCode("career"));
    assertThrows(BusinessException.class, () -> ReportTopic.fromCode("fortune_telling"));
  }

  @Test
  void shipsOnlyFourReviewedTopics() {
    assertEquals(
        List.of("overall", "career", "wealth", "relationship"),
        Arrays.stream(ReportTopic.values()).map(ReportTopic::code).toList());
    assertThrows(BusinessException.class, () -> ReportTopic.fromCode("family"));
  }

  @Test
  void acceptsSupportedEditionsAndRejectsUnknownValues() {
    assertEquals(ReportEdition.PLAIN, ReportEdition.fromCode("plain"));
    assertEquals(ReportEdition.PROFESSIONAL, ReportEdition.fromCode("professional"));
    assertThrows(BusinessException.class, () -> ReportEdition.fromCode("premium"));
  }
}
