package com.bazi.app.controller;

import com.bazi.app.dto.ReportPreviewRequest;
import com.bazi.app.report.ReportEdition;
import com.bazi.app.report.ReportTopic;
import com.bazi.app.report.CareerContext;
import com.bazi.app.service.ReportPreviewService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportPreviewController {

  private final ReportPreviewService reportPreviewService;

  public ReportPreviewController(ReportPreviewService reportPreviewService) {
    this.reportPreviewService = reportPreviewService;
  }

  @PostMapping(value = "/preview", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> preview(@Valid @RequestBody ReportPreviewRequest request) {
    ReportTopic topic = ReportTopic.fromCode(request.topic());
    ReportEdition edition = ReportEdition.fromCode(request.edition());
    CareerContext careerContext = request.careerContext() == null
        ? null
        : request.careerContext().toDomain();
    byte[] pdf = reportPreviewService.render(request.request(), topic, edition, careerContext);
    String fileName = "命书-" + topic.label() + "-" + edition.label() + ".pdf";
    ContentDisposition disposition = ContentDisposition.attachment()
        .filename(fileName, StandardCharsets.UTF_8)
        .build();
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
        .body(pdf);
  }
}
