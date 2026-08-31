package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.domain.User;
import com.bazi.app.mapper.UserMapper;
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
class ReportEntitlementIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserMapper userMapper;

  @Test
  void nonMemberCannotBypassSingleReportPurchaseWithDirectGeneration() throws Exception {
    String token = register("non-member-direct-report");

    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayload("非会员命主")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("REPORT_PAYMENT_REQUIRED"))
        .andExpect(jsonPath("$.message").value("请先购买这份命书，本次未扣费"));

    mvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void memberConsumesOnlyThreeSuccessfullySavedReportsPerDay() throws Exception {
    String username = "member-report-daily-limit";
    String token = register(username);
    activateMembership(username);

    for (int index = 0; index < 3; index++) {
      mvc.perform(post("/api/v1/reports")
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .content(wealthPayload("会员命主" + index)))
          .andExpect(status().isOk());
    }

    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayload("会员命主4")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MEMBER_DAILY_REPORT_LIMIT"))
        .andExpect(jsonPath("$.message").value("今天的3份会员命书已全部生成；继续生成需单独购买，本次未扣费"));

    MvcResult list = mvc.perform(get("/api/v1/reports")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andReturn();
    assertEquals(3, objectMapper.readTree(list.getResponse().getContentAsString()).size());
  }

  private String register(String username) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private void activateMembership(String username) {
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    user.setPlan("member_1m");
    user.setMemberExpireAt(LocalDateTime.now().plusDays(1));
    userMapper.updateById(user);
  }

  private String wealthPayload(String name) {
    return """
        {
          "request": {
            "name": "%s",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "wealth",
          "edition": "plain"
        }
        """.formatted(name);
  }
}
