package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.domain.BaziReport;
import com.bazi.app.domain.User;
import com.bazi.app.mapper.BaziReportMapper;
import com.bazi.app.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
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

  @Autowired
  private BaziReportMapper reportMapper;

  @Autowired
  private UserMapper userMapper;

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
            .content(wealthPayloadWithoutContext("plain")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subject").value("林先生"))
        .andExpect(jsonPath("$.topic").value("wealth"))
        .andExpect(jsonPath("$.contentVersion").value("wealth-narrative-v2"))
        .andExpect(jsonPath("$.content.paths.length()").value(5))
        .andExpect(jsonPath("$.content.years.length()").value(3))
        .andExpect(jsonPath("$.content.years[0].year").value(2026))
        .andExpect(jsonPath("$.content.years[1].year").value(2027))
        .andExpect(jsonPath("$.content.years[2].year").value(2028))
        .andExpect(jsonPath("$.content.years[0].evidenceKeys").isArray())
        .andReturn();

    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
    mvc.perform(get("/api/v1/reports/{id}", id)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.topic").value("wealth"))
        .andExpect(jsonPath("$.content.years[2].year").value(2028));
  }

  @Test
  void legacyWealthContextIsAcceptedButCannotChangeV2Content() throws Exception {
    String token = register("saved-wealth-legacy-context");
    MvcResult withoutContext = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayloadWithoutContext("plain")))
        .andExpect(status().isOk())
        .andReturn();
    MvcResult withContext = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayload("plain")))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode first = objectMapper.readTree(withoutContext.getResponse().getContentAsString());
    JsonNode second = objectMapper.readTree(withContext.getResponse().getContentAsString());
    assertEquals(first.get("content"), second.get("content"));
  }

  @Test
  void rejectsUnavailableWealthProfessionalWithoutSavingAReport() throws Exception {
    String token = register("saved-wealth-professional");
    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayloadWithoutContext("professional")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("WEALTH_PROFESSIONAL_NOT_AVAILABLE"));

    mvc.perform(get("/api/v1/reports")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void readsAStableLegacyWealthV1Snapshot() throws Exception {
    String username = "saved-wealth-v1-fixture";
    String token = register(username);
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    LocalDateTime now = LocalDateTime.now();
    BaziReport report = new BaziReport();
    report.setUserId(user.getId());
    report.setSubject("旧版命主");
    report.setTopic("wealth");
    report.setEdition("plain");
    report.setStatus("ready");
    report.setContentVersion("wealth-narrative-v1");
    report.setRequestJson("{}");
    report.setContextJson("{}");
    report.setContentJson(legacyWealthV1Fixture());
    report.setCreatedAt(now);
    report.setGeneratedAt(now);
    reportMapper.insert(report);

    mvc.perform(get("/api/v1/reports/{id}", report.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value("wealth-narrative-v1"))
        .andExpect(jsonPath("$.content.contextSummary").value("旧版财富问卷摘要"))
        .andExpect(jsonPath("$.content.years[0].headline").value("旧版内容仍可阅读"));
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

  private String wealthPayloadWithoutContext(String edition) {
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
          "edition": "%s"
        }
        """.formatted(edition);
  }

  private String legacyWealthV1Fixture() {
    return """
        {
          "thesis": "旧版财富主题",
          "contextSummary": "旧版财富问卷摘要",
          "years": [{
            "year": 2026,
            "ganZhi": "丙午",
            "stage": "第一年",
            "headline": "旧版内容仍可阅读",
            "verdict": "这是固定的旧版快照。",
            "reasons": ["旧版理由一", "旧版理由二"],
            "obstacle": "旧版限制",
            "actions": ["旧版行动一", "旧版行动二"],
            "changeCondition": "旧版变化条件",
            "evidenceKeys": ["legacy.fixture"],
            "evidence": [],
            "counterEvidence": [],
            "confidence": "中"
          }],
          "route": ["旧版路线"]
        }
        """;
  }
}
