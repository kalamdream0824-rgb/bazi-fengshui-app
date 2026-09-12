package com.bazi.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.domain.User;
import com.bazi.app.mapper.UserMapper;
import com.bazi.app.report.overall.v3.OverallV3ReportGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OverallContentFailureIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserMapper userMapper;
  @MockitoBean private OverallV3ReportGenerator generator;

  @Test
  void generationFailureSavesNothingConsumesNoSlotAndAllowsThreeLaterSuccesses()
      throws Exception {
    when(generator.generate(any(), any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("forced overall failure"))
        .thenCallRealMethod();
    String username = "member-overall-content-invalid";
    String token = register(username);
    activateMembership(username);

    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(overallPayload("失败样本")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("REPORT_GENERATION_UNAVAILABLE"))
        .andExpect(jsonPath("$.message")
            .value("本次命书暂未生成成功，未扣除费用或使用次数，请稍后再试"));

    mvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));

    for (int index = 1; index <= 3; index++) {
      mvc.perform(post("/api/v1/reports")
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .content(overallPayload("成功样本" + index)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.contentVersion").value("overall-narrative-v3"));
    }
    mvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3));
  }

  private String register(String username) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private void activateMembership(String username) {
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    user.setPlan("member_1m");
    user.setMemberExpireAt(LocalDateTime.now().plusDays(1));
    userMapper.updateById(user);
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
