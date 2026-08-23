package com.bazi.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
class SavedReportIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Test
  void createsAndReadsAStoredCareerReportSnapshot() throws Exception {
    String token = register("saved-report-owner");

    MvcResult created = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload("plain")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.subject").value("林先生"))
        .andExpect(jsonPath("$.topic").value("career"))
        .andExpect(jsonPath("$.edition").value("plain"))
        .andExpect(jsonPath("$.contentVersion").value("career-narrative-v3"))
        .andExpect(jsonPath("$.content.thesis").isNotEmpty())
        .andExpect(jsonPath("$.content.years.length()").value(2))
        .andExpect(jsonPath("$.content.years[0].reasons.length()").value(2))
        .andExpect(jsonPath("$.content.years[0].actions.length()").value(2))
        .andReturn();

    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mvc.perform(get("/api/v1/reports")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id))
        .andExpect(jsonPath("$[0].content.thesis").isNotEmpty());

    mvc.perform(get("/api/v1/reports/{id}", id)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.content.years[1].year").value(2027));
  }

  @Test
  void reportOwnershipIsolatedBetweenUsers() throws Exception {
    String owner = register("saved-report-private-owner");
    String stranger = register("saved-report-stranger");
    MvcResult created = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload("professional")))
        .andExpect(status().isOk())
        .andReturn();
    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mvc.perform(get("/api/v1/reports/{id}", id)
            .header("Authorization", "Bearer " + stranger))
        .andExpect(status().isNotFound());

    mvc.perform(get("/api/v1/reports")
            .header("Authorization", "Bearer " + stranger))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void savedCareerReportStillRequiresRealityContext() throws Exception {
    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + register("saved-report-no-context"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(payloadWithoutContext()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CAREER_CONTEXT_REQUIRED"));
  }

  @Test
  void createsAndReadsAStoredWealthReportSnapshot() throws Exception {
    String token = register("saved-wealth-report-owner");

    MvcResult created = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayload("plain")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subject").value("林先生"))
        .andExpect(jsonPath("$.topic").value("wealth"))
        .andExpect(jsonPath("$.contentVersion").value("wealth-narrative-v1"))
        .andExpect(jsonPath("$.content.contextSummary").value(
            "工资和副业都有 · 希望增加收入 · 近期收入有波动"))
        .andExpect(jsonPath("$.content.years.length()").value(2))
        .andExpect(jsonPath("$.content.years[0].evidenceKeys").isArray())
        .andReturn();

    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(get("/api/v1/reports/{id}", id)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.topic").value("wealth"))
        .andExpect(jsonPath("$.content.years[1].year").value(2027));
  }

  @Test
  void savedWealthReportRequiresItsOwnRealityContext() throws Exception {
    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + register("saved-wealth-no-context"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayloadWithoutContext()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("WEALTH_CONTEXT_REQUIRED"));
  }

  private String register(String username) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private String payload(String edition) {
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
          "edition": "%s",
          "careerContext": {
            "status": "employed",
            "goal": "promotion",
            "pace": "smooth"
          }
        }
        """.formatted(edition);
  }

  private String payloadWithoutContext() {
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
          "edition": "plain"
        }
        """;
  }

  private String wealthPayload(String edition) {
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "wealth",
          "edition": "%s",
          "wealthContext": {
            "incomeSource": "mixed",
            "goal": "increase_income",
            "pace": "income_fluctuating"
          }
        }
        """.formatted(edition);
  }

  private String wealthPayloadWithoutContext() {
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "wealth",
          "edition": "plain"
        }
        """;
  }
}
