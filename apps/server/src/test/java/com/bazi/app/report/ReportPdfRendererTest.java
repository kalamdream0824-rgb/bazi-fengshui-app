package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.service.BaziService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ReportPdfRendererTest {

  private static byte[] plainPdf;
  private static byte[] professionalPdf;

  @BeforeAll
  static void generateSamples() throws Exception {
    PaipanRequest request = new PaipanRequest("林先生", "male", "1995-10-08T14:30:00", "上海", false);
    PaipanResultDto result = new BaziService().paipan(request);
    ReportComposer composer = new ReportComposer();
    ReportPdfRenderer renderer = new ReportPdfRenderer();

    plainPdf = renderer.render(composer.compose(request, result, ReportTopic.CAREER, ReportEdition.PLAIN));
    professionalPdf = renderer.render(composer.compose(request, result, ReportTopic.CAREER, ReportEdition.PROFESSIONAL));

    Path output = Path.of("target", "report-samples");
    Files.createDirectories(output);
    Files.write(output.resolve("命书样本-通俗版-事业与职场.pdf"), plainPdf);
    Files.write(output.resolve("命书样本-专业版-事业与职场.pdf"), professionalPdf);
  }

  @Test
  void legacyPlainPdfContainsSearchableTwoYearContent() throws Exception {
    try (PDDocument document = Loader.loadPDF(plainPdf)) {
      String text = new PDFTextStripper().getText(document);
      assertTrue(text.contains("壹、未来两年事业运势总览"));
      assertTrue(text.contains("贰、2026年事业运势详解"));
      assertTrue(text.contains("肆、两年事业行动路线"));
      assertTrue(text.contains("乙亥 · 乙酉 · 壬申 · 丁未"));
      assertTrue(document.getNumberOfPages() > 0);
    }
  }

  @Test
  void professionalPdfKeepsEvidenceAndMethodNotesAtProfessionalDepth() throws Exception {
    try (PDDocument document = Loader.loadPDF(professionalPdf)) {
      String text = new PDFTextStripper().getText(document);
      assertTrue(text.contains("证据链"));
      assertTrue(text.contains("方法注"));
      assertTrue(text.contains("置信等级"));
      assertTrue(text.contains("方法边界"));
      assertTrue(text.contains("career."));
      assertTrue(document.getNumberOfPages() > 0);
    }
  }

  @Test
  void everyPageContainsExtractableText() throws Exception {
    for (byte[] pdf : new byte[][] {plainPdf, professionalPdf}) {
      try (PDDocument document = Loader.loadPDF(pdf)) {
        PDFTextStripper stripper = new PDFTextStripper();
        for (int page = 1; page <= document.getNumberOfPages(); page++) {
          stripper.setStartPage(page);
          stripper.setEndPage(page);
          assertTrue(stripper.getText(document).trim().length() >= 10, "第 " + page + " 页不应为纯图片或空页");
        }
      }
    }
  }
}
