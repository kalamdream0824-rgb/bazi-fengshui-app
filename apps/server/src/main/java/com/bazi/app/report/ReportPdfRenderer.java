package com.bazi.app.report;

import com.bazi.app.report.ReportDocument.ReportChapter;
import com.bazi.app.report.ReportDocument.ReportPoint;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopConfParser;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.SAXException;

public final class ReportPdfRenderer {

  private static final String FONT_NAME = "Mingshu Serif";
  private static final Path MACOS_FALLBACK_FONT = Path.of("/System/Library/Fonts/Supplemental/Songti.ttc");

  public byte[] render(ReportDocument document) {
    try {
      FopFactory factory = createFactory();
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      Fop fop = factory.newFop(MimeConstants.MIME_PDF, output);
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
      transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
      transformer.transform(
          new StreamSource(new ByteArrayInputStream(buildFo(document).getBytes(StandardCharsets.UTF_8))),
          new SAXResult(fop.getDefaultHandler()));
      return output.toByteArray();
    } catch (Exception error) {
      throw new IllegalStateException("命书 PDF 生成失败", error);
    }
  }

  private FopFactory createFactory() throws IOException, URISyntaxException, FOPException, SAXException {
    FontSource font = resolveFont();
    String config = resourceText("/report/fop.xconf")
        .replace("{{FONT_URI}}", xml(font.uri().toString()))
        .replace("{{SUB_FONT_ATTRIBUTE}}", font.subFont() == null ? "" : "sub-font=\"" + xml(font.subFont()) + "\"");
    URI baseUri = ReportPdfRenderer.class.getResource("/report/").toURI();
    return new FopConfParser(
        new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8)),
        baseUri).getFopFactoryBuilder().build();
  }

  private FontSource resolveFont() throws IOException, URISyntaxException {
    var bundled = ReportPdfRenderer.class.getResource("/report/fonts/NotoSerifSC-Regular.ttf");
    if (bundled != null) {
      return new FontSource(bundled.toURI(), null);
    }
    if (Files.isRegularFile(MACOS_FALLBACK_FONT)) {
      return new FontSource(MACOS_FALLBACK_FONT.toUri(), "Songti SC Regular");
    }
    throw new IOException("未找到可嵌入的中文字体 NotoSerifSC-Regular.ttf");
  }

  private String buildFo(ReportDocument document) throws IOException {
    String reportId = "MS-" + document.contentVersion().replaceAll("[^A-Za-z0-9]", "").toUpperCase()
        + "-" + document.editionCode().toUpperCase() + "-" + document.topicCode().toUpperCase();
    return resourceText("/report/mingshu.fo.xml")
        .replace("{{EDITION_LABEL}}", xml(document.editionLabel()))
        .replace("{{TOPIC_LABEL}}", xml(document.topicLabel()))
        .replace("{{REPORT_ID}}", xml(reportId))
        .replace("{{BODY}}", body(document));
  }

  private String body(ReportDocument document) {
    StringBuilder fo = new StringBuilder();
    fo.append(cover(document));
    fo.append(readingGuide(document));
    boolean professional = ReportEdition.PROFESSIONAL.code().equals(document.editionCode());
    for (ReportChapter chapter : document.chapters()) {
      fo.append(chapterOpener(chapter));
      List<ReportPoint> points = chapter.sections().stream().flatMap(section -> section.points().stream()).toList();
      if (professional) {
        int paired = Math.min(2, points.size());
        fo.append(professionalPairPage(chapter, points.subList(0, paired), points.size()));
        for (int index = paired; index < points.size(); index++) {
          fo.append(professionalPointPage(chapter, points, index));
        }
      } else {
        fo.append(plainChapterPage(chapter, points));
      }
    }
    return fo.toString();
  }

  private String cover(ReportDocument document) {
    var profile = document.profile();
    return page("""
        <fo:block-container height="200mm" border="0.8pt solid #B78B43" padding="12mm" background-color="#F7F1E3">
          <fo:block color="#9E392C" font-size="10pt" letter-spacing="4pt" text-align="center" space-after="18mm">东方命理结构研读</fo:block>
          <fo:block font-size="32pt" font-weight="bold" letter-spacing="5pt" text-align="center" space-after="5mm">命　书</fo:block>
          <fo:block color="#A97C35" font-size="12pt" letter-spacing="2pt" text-align="center" space-after="18mm">%s · %s</fo:block>
          <fo:block border-top="0.6pt solid #C8AA72" border-bottom="0.6pt solid #C8AA72" padding-top="8mm" padding-bottom="8mm" text-align="center" space-after="14mm">
            <fo:block font-size="18pt" font-weight="bold" space-after="4mm">%s</fo:block>
            <fo:block color="#6F6252" font-size="10pt">%s　｜　%s</fo:block>
          </fo:block>
          %s
          <fo:block color="#7A6A55" font-size="8.5pt" line-height="15pt" text-align="center" space-before="16mm">同一证据引擎 · 固定内容版本 · 可检索文字型 PDF</fo:block>
        </fo:block-container>
        """.formatted(
        xml(document.editionLabel()), xml(document.topicLabel()), xml(document.subject()),
        xml(profile.solarText()), xml(profile.lunarText()), profileTable(document)));
  }

  private String profileTable(ReportDocument document) {
    var p = document.profile();
    return """
        <fo:table table-layout="fixed" width="100%%" border-collapse="collapse" font-size="9pt">
          <fo:table-column column-width="30%%"/><fo:table-column column-width="70%%"/>
          <fo:table-body>
            %s%s%s%s
          </fo:table-body>
        </fo:table>
        """.formatted(
        tableRow("四柱", p.pillarsText()), tableRow("日主", p.dayMaster()),
        tableRow("当前大运", p.currentDaYun()), tableRow("流年", p.currentLiuNian()));
  }

  private String tableRow(String label, String value) {
    return """
        <fo:table-row>
          <fo:table-cell border-bottom="0.35pt solid #D8C7A5" padding="3mm"><fo:block color="#9E392C">%s</fo:block></fo:table-cell>
          <fo:table-cell border-bottom="0.35pt solid #D8C7A5" padding="3mm"><fo:block>%s</fo:block></fo:table-cell>
        </fo:table-row>
        """.formatted(xml(label), xml(value));
  }

  private String readingGuide(ReportDocument document) {
    StringBuilder toc = new StringBuilder();
    for (ReportChapter chapter : document.chapters()) {
      toc.append("<fo:block border-bottom=\"0.35pt solid #D8C7A5\" padding=\"4mm 1mm\" font-size=\"12pt\">")
          .append("<fo:inline color=\"#9E392C\" font-weight=\"bold\">").append(xml(chapter.number())).append("</fo:inline>")
          .append("　").append(xml(chapter.title())).append("</fo:block>");
    }
    return page("""
        <fo:block font-size="9pt" color="#9E392C" letter-spacing="2pt" space-after="5mm">READING GUIDE</fo:block>
        <fo:block font-size="24pt" font-weight="bold" space-after="5mm">阅读说明与目录</fo:block>
        <fo:block color="#6F6252" font-size="10pt" line-height="18pt" space-after="9mm">%s</fo:block>
        <fo:block background-color="#F5EEDF" border-left="2pt solid #A43D30" padding="5mm" font-size="9pt" line-height="16pt" space-after="9mm">本报告先列证据，再给解释与反思问题。专业版额外展示方法注；通俗版保留关键证据并减少术语密度。</fo:block>
        %s
        """.formatted(xml(document.disclaimer()), toc));
  }

  private String chapterOpener(ReportChapter chapter) {
    return page("""
        <fo:block-container height="185mm" display-align="center" padding="16mm" background-color="#202C2B" color="#F6EEDC">
          <fo:block color="#C79A50" font-size="44pt" font-weight="bold" space-after="7mm">%s</fo:block>
          <fo:block border-top="0.8pt solid #B43E31" padding-top="8mm" font-size="25pt" font-weight="bold" letter-spacing="2pt" space-after="9mm">%s</fo:block>
          <fo:block color="#D8C9AC" font-size="11pt" line-height="20pt">%s</fo:block>
        </fo:block-container>
        """.formatted(xml(chapter.number()), xml(chapter.title()), xml(chapter.lead())));
  }

  private String plainChapterPage(ReportChapter chapter, List<ReportPoint> points) {
    StringBuilder content = new StringBuilder();
    for (int index = 0; index < points.size(); index++) {
      ReportPoint point = points.get(index);
      content.append("<fo:block space-after=\"7mm\" keep-together.within-page=\"always\">")
          .append("<fo:block color=\"#A43D30\" font-size=\"8pt\" letter-spacing=\"1pt\" space-after=\"2mm\">观察 ").append(index + 1).append("</fo:block>")
          .append("<fo:block font-size=\"14pt\" font-weight=\"bold\" line-height=\"20pt\" space-after=\"3mm\">").append(xml(point.conclusion())).append("</fo:block>")
          .append(evidenceBox(point.evidence(), false))
          .append("<fo:block font-size=\"10pt\" line-height=\"18pt\" space-before=\"3mm\">").append(xml(point.interpretation())).append("</fo:block>")
          .append("<fo:block color=\"#786A58\" font-size=\"9pt\" line-height=\"16pt\" space-before=\"2mm\">自问：").append(xml(point.prompt())).append("</fo:block>")
          .append("</fo:block>");
    }
    return page(sectionHeader(chapter, "通俗研读") + content);
  }

  private String professionalPairPage(ReportChapter chapter, List<ReportPoint> points, int total) {
    StringBuilder content = new StringBuilder(sectionHeader(chapter, "专业研读 · 1/2"));
    for (int index = 0; index < points.size(); index++) {
      ReportPoint point = points.get(index);
      content.append("<fo:block space-after=\"7mm\" keep-together.within-page=\"always\">")
          .append("<fo:block color=\"#A43D30\" font-size=\"8pt\" letter-spacing=\"1pt\" space-after=\"2mm\">规则 ")
          .append(index + 1).append("/").append(total).append("　").append(xml(point.ruleKey())).append("</fo:block>")
          .append("<fo:block font-size=\"14pt\" font-weight=\"bold\" line-height=\"20pt\" space-after=\"3mm\">").append(xml(point.conclusion())).append("</fo:block>")
          .append("<fo:block color=\"#8A6B36\" font-size=\"8pt\" font-weight=\"bold\" space-after=\"2mm\">证据链</fo:block>")
          .append(evidenceBox(point.evidence(), true))
          .append("<fo:block font-size=\"9pt\" line-height=\"16pt\" space-before=\"3mm\">").append(xml(point.interpretation())).append("</fo:block>")
          .append("<fo:block color=\"#765F3D\" font-size=\"8pt\" line-height=\"14pt\" space-before=\"2mm\">方法注：").append(xml(point.methodNote())).append("</fo:block>")
          .append("</fo:block>");
    }
    return page(content.toString());
  }

  private String professionalPointPage(ReportChapter chapter, List<ReportPoint> chapterPoints, int index) {
    ReportPoint point = chapterPoints.get(index);
    return page(sectionHeader(chapter, "专业研读 · 2/2") + """
        <fo:block color="#A43D30" font-size="8pt" letter-spacing="1pt" space-after="3mm">规则 %s/%s　%s</fo:block>
        <fo:block font-size="19pt" font-weight="bold" line-height="27pt" space-after="7mm">%s</fo:block>
        <fo:block color="#8A6B36" font-size="9pt" font-weight="bold" space-after="3mm">证据链</fo:block>
        %s
        <fo:block color="#8A6B36" font-size="9pt" font-weight="bold" space-before="7mm" space-after="3mm">结构解释</fo:block>
        <fo:block font-size="10pt" line-height="19pt">%s</fo:block>
        <fo:block background-color="#F3EBDD" border-left="2pt solid #A43D30" padding="5mm" space-before="8mm">
          <fo:block color="#8A6B36" font-size="9pt" font-weight="bold" space-after="2mm">方法注</fo:block>
          <fo:block font-size="9pt" line-height="17pt">%s</fo:block>
        </fo:block>
        <fo:block border-top="0.5pt solid #C8AA72" color="#6F6252" font-size="9pt" line-height="17pt" padding-top="4mm" space-before="9mm">复核问题：%s</fo:block>
        <fo:block background-color="#202C2B" color="#EFE4CF" padding="5mm" space-before="8mm">
          <fo:block color="#CFA45C" font-size="8pt" letter-spacing="1pt" space-after="2mm">章节复核</fo:block>
          <fo:block font-size="8.5pt" line-height="15pt">%s</fo:block>
          <fo:block color="#CBBDA6" font-size="7.5pt" line-height="14pt" space-before="2mm">规则索引：%s</fo:block>
        </fo:block>
        """.formatted(index + 1, chapterPoints.size(), xml(point.ruleKey()), xml(point.conclusion()), evidenceBox(point.evidence(), true),
        xml(point.interpretation()), xml(point.methodNote()), xml(point.prompt()), xml(chapter.lead()),
        xml(chapterPoints.stream().map(ReportPoint::ruleKey).reduce((a, b) -> a + " · " + b).orElse(""))));
  }

  private String sectionHeader(ReportChapter chapter, String mode) {
    return """
        <fo:block color="#9E392C" font-size="8pt" letter-spacing="1.5pt" space-after="3mm">%s · %s</fo:block>
        <fo:block font-size="21pt" font-weight="bold" space-after="9mm">%s、%s</fo:block>
        """.formatted(xml(mode), xml(chapter.number()), xml(chapter.number()), xml(chapter.title()));
  }

  private String evidenceBox(List<String> evidence, boolean full) {
    StringBuilder rows = new StringBuilder();
    int limit = full ? evidence.size() : Math.min(2, evidence.size());
    for (int i = 0; i < limit; i++) {
      rows.append("<fo:block font-size=\"8.5pt\" line-height=\"15pt\" space-after=\"1mm\">· ")
          .append(xml(evidence.get(i))).append("</fo:block>");
    }
    return "<fo:block background-color=\"#F7F1E5\" border=\"0.4pt solid #D8C7A5\" padding=\"4mm\">" + rows + "</fo:block>";
  }

  private String page(String content) {
    return "<fo:block min-height=\"220mm\" break-after=\"page\">" + content + "</fo:block>";
  }

  private static String resourceText(String path) throws IOException {
    try (InputStream input = ReportPdfRenderer.class.getResourceAsStream(path)) {
      if (input == null) throw new IOException("Missing resource: " + path);
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static String xml(String value) {
    if (value == null) return "";
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;");
  }

  private record FontSource(URI uri, String subFont) {}
}
