package com.bazi.app.report.classic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class R05RootedSupportManualReviewTest {

  private static final Path INPUTS = Path.of(
      "src/test/resources/report/wealth-v2-baseline-inputs.json");

  @Test
  void keepsR05InWeakBandButRecordsThatItHasRoots() throws Exception {
    RootedSupportManualReview.Review review =
        RootedSupportManualReview.review(INPUTS, "R05", "广东省 广州市");

    assertEquals("广东省 广州市", review.reviewedBirthPlace());
    assertEquals("1992-07-18T06:15", review.originalTime());
    assertEquals("1992-07-18T05:41", review.adjustedTrueSolarTime());
    assertEquals(-27, review.longitudeOffsetMinutes());
    assertEquals(-6.1, review.equationOfTimeMinutes());
    assertEquals("壬申·丁未·乙未·己卯", review.standardTimePillars());
    assertEquals(review.standardTimePillars(), review.trueSolarTimePillars());
    assertFalse(review.trueSolarBoundaryChanged());
    assertEquals("偏弱", review.servingBand());
    assertEquals(1.5, review.servingScore());
    assertEquals(1.8, review.shadowScore());
    assertEquals("偏弱", review.manualBandDecision());
    assertEquals("有根", review.manualModifier());
    assertEquals("KEEP_SERVING_BAND_USE_ROOT_AS_MODIFIER", review.recommendation());
    assertEquals(
        List.of("时支卯为直接根", "年干壬为明透印星", "申中壬为远位藏印", "月支未与日支未均藏乙"),
        review.supportingFactors());
    assertEquals(
        List.of("未月不直接生扶乙木", "月干丁与未中丁泄木", "月日两未及时干己使财星偏重", "申中庚对乙木形成约束"),
        review.limitingFactors());
  }
}
