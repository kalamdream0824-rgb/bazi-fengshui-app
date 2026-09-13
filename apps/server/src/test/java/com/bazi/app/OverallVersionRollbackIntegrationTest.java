package com.bazi.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.domain.BaziReport;
import com.bazi.app.domain.User;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.mapper.BaziReportMapper;
import com.bazi.app.mapper.UserMapper;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.overall.v3.OverallV3ReportGenerator;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "app.report.overall-content-version=overall-narrative-v2")
@AutoConfigureMockMvc
@Transactional
class OverallVersionRollbackIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserMapper userMapper;
  @Autowired private BaziReportMapper reportMapper;
  @Autowired private OverallV3ReportGenerator overallV3ReportGenerator;

  @Test
  void rollbackChangesNewGenerationButKeepsStoredV3Readable() throws Exception {
    String username = "overall-version-rollback";
    String token = register(username);
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    user.setPlan("member_1m");
    user.setMemberExpireAt(LocalDateTime.now().plusDays(1));
    userMapper.updateById(user);

    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(overallPayload("回滚后新报告")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value("overall-narrative-v2"))
        .andExpect(jsonPath("$.content.years[0].dimensions.length()").value(4));

    PaipanRequest request = new PaipanRequest(
        "已保存 v3", "male", "1995-10-08T14:30:00", "上海", false);
    var chart = new BaziService().paipan(request);
    Clock clock = Clock.fixed(
        Instant.parse("2026-09-13T00:00:00Z"), ZoneId.of("Asia/Shanghai"));
    AnnualContextFactory factory = new AnnualContextFactory(clock);
    var productYears = factory.create(request, chart, ReportHorizon.of(3));
    var previous = factory.createYear(request, chart, productYears.get(0).year() - 1);
    var content = overallV3ReportGenerator.generate(
        request, chart, previous, productYears, clock);

    BaziReport storedV3 = new BaziReport();
    storedV3.setUserId(user.getId());
    storedV3.setSubject("已保存 v3");
    storedV3.setTopic("overall");
    storedV3.setEdition("plain");
    storedV3.setStatus("ready");
    storedV3.setContentVersion("overall-narrative-v3");
    storedV3.setRequestJson(objectMapper.writeValueAsString(request));
    storedV3.setContextJson("{\"source\":\"system\",\"horizonYears\":3}");
    storedV3.setContentJson(objectMapper.writeValueAsString(content));
    storedV3.setCreatedAt(LocalDateTime.now());
    storedV3.setGeneratedAt(LocalDateTime.now());
    reportMapper.insert(storedV3);

    mvc.perform(get("/api/v1/reports/{id}", storedV3.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value("overall-narrative-v3"))
        .andExpect(jsonPath("$.content.years.length()").value(3))
        .andExpect(jsonPath("$.content.years[0].actionGuide.focusKey").isNotEmpty());
  }

  private String register(String username) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private String overallPayload(String name) {
    return """
        {
          "request": {
            "name": "%s",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "overall",
          "edition": "plain"
        }
        """.formatted(name);
  }
}
