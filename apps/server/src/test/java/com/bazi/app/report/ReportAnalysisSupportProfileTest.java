package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.service.BaziService;
import org.junit.jupiter.api.Test;

class ReportAnalysisSupportProfileTest {

  @Test
  void classifiesR05AsWeakWithRootsAndVisibleResource() {
    PaipanRequest request = new PaipanRequest(
        "测试命盘R05", "female", "1992-07-18T06:15:00", "广东省 广州市", false);

    ReportAnalysis analysis = ReportAnalysis.from(request, new BaziService().paipan(request));

    assertEquals("偏弱", analysis.wangShuaiLevel());
    assertEquals(
        WeakSupportProfile.ROOTED_WITH_VISIBLE_RESOURCE,
        analysis.weakSupportProfile());
  }

  @Test
  void doesNotApplyWeakSupportProfileOutsideWeakBand() {
    PaipanRequest request = new PaipanRequest(
        "非弱档", "male", "1995-10-08T14:30:00", "上海", false);

    ReportAnalysis analysis = ReportAnalysis.from(request, new BaziService().paipan(request));

    assertEquals("偏强", analysis.wangShuaiLevel());
    assertEquals(WeakSupportProfile.NOT_WEAK, analysis.weakSupportProfile());
  }
}
