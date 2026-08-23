package com.bazi.app.service;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.ReportComposer;
import com.bazi.app.report.CareerContext;
import com.bazi.app.config.BusinessException;
import com.bazi.app.report.ReportEdition;
import com.bazi.app.report.ReportPdfRenderer;
import com.bazi.app.report.ReportTopic;
import org.springframework.stereotype.Service;

@Service
public class ReportPreviewService {

  private final BaziService baziService;
  private final ReportComposer composer;
  private final ReportPdfRenderer renderer;

  public ReportPreviewService(BaziService baziService) {
    this.baziService = baziService;
    composer = new ReportComposer();
    renderer = new ReportPdfRenderer();
  }

  public byte[] render(
      PaipanRequest request,
      ReportTopic topic,
      ReportEdition edition,
      CareerContext careerContext) {
    if (topic == ReportTopic.CAREER && careerContext == null) {
      throw new BusinessException("CAREER_CONTEXT_REQUIRED", "生成事业命书前，请完成事业状态问卷");
    }
    PaipanResultDto result = baziService.paipan(request);
    return renderer.render(composer.compose(request, result, topic, edition, careerContext));
  }
}
