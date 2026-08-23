package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportPreviewIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void previewRequiresAuthentication() throws Exception {
    mvc.perform(post("/api/v1/reports/preview")
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload("career", "plain")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void downloadsARealPlainTwoYearCareerPdf() throws Exception {
    MvcResult response = mvc.perform(post("/api/v1/reports/preview")
            .header("Authorization", "Bearer " + register("report-preview-plain"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload("career", "plain")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
        .andReturn();

    try (PDDocument document = Loader.loadPDF(response.getResponse().getContentAsByteArray())) {
      String text = new PDFTextStripper().getText(document);
      assertTrue(text.contains("未来两年事业运势总览"));
      assertTrue(text.contains("2026年事业运势详解"));
      assertTrue(text.contains("两年事业行动路线"));
    }
  }

  @Test
  void careerPreviewRequiresRealityContext() throws Exception {
    mvc.perform(post("/api/v1/reports/preview")
            .header("Authorization", "Bearer " + register("report-preview-no-career-context"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(payloadWithoutCareerContext("career", "plain")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CAREER_CONTEXT_REQUIRED"));
  }

  @Test
  void careerPreviewRejectsUnknownRealityContextValue() throws Exception {
    mvc.perform(post("/api/v1/reports/preview")
            .header("Authorization", "Bearer " + register("report-preview-bad-career-context"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(payloadWithCareerContext("unknown", "promotion", "smooth")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CAREER_CONTEXT_INVALID"));
  }

  @Test
  void professionalPreviewContainsProfessionalEvidenceFields() throws Exception {
    MvcResult response = mvc.perform(post("/api/v1/reports/preview")
            .header("Authorization", "Bearer " + register("report-preview-pro"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload("wealth", "professional")))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andReturn();

    try (PDDocument document = Loader.loadPDF(response.getResponse().getContentAsByteArray())) {
      String text = new PDFTextStripper().getText(document);
      assertTrue(text.contains("置信等级"));
      assertTrue(text.contains("方法边界"));
      assertTrue(text.contains("wealth."));
    }
  }

  @Test
  void rejectsUnsupportedTopicWithStableErrorCode() throws Exception {
    mvc.perform(post("/api/v1/reports/preview")
            .header("Authorization", "Bearer " + register("report-preview-invalid"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload("health", "plain")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("REPORT_TOPIC_UNSUPPORTED"));
  }

  private String register(String username) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private String payload(String topic, String edition) {
    String careerContext = "career".equals(topic)
        ? """
          ,
          "careerContext": {
            "status": "employed",
            "goal": "promotion",
            "pace": "smooth"
          }
          """
        : "";
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "%s",
          "edition": "%s"%s
        }
        """.formatted(topic, edition, careerContext);
  }

  private String payloadWithoutCareerContext(String topic, String edition) {
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "%s",
          "edition": "%s"
        }
        """.formatted(topic, edition);
  }

  private String payloadWithCareerContext(String status, String goal, String pace) {
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "career",
          "edition": "plain",
          "careerContext": {
            "status": "%s",
            "goal": "%s",
            "pace": "%s"
          }
        }
        """.formatted(status, goal, pace);
  }
}
