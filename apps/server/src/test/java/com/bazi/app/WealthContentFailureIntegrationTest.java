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
import com.bazi.app.report.wealth.v3.WealthReportGenerator;
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
class WealthContentFailureIntegrationTest {
  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private UserMapper userMapper;
  @MockitoBean private WealthReportGenerator wealthReportGenerator;

  @Test
  void invalidWealthContentReturnsStableErrorAndDoesNotSaveAReport() throws Exception {
    when(wealthReportGenerator.generate(any(), any(), any()))
        .thenThrow(new IllegalArgumentException("unresolved wealth content"));
    String username = "member-wealth-content-invalid";
    String token = register(username);
    activateMembership(username);

    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayload()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("REPORT_GENERATION_UNAVAILABLE"))
        .andExpect(jsonPath("$.message").value("本次命书暂未生成成功，未扣除费用或使用次数，请稍后再试"));

    mvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  private void activateMembership(String username) {
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    user.setPlan("member_1m");
    user.setMemberExpireAt(LocalDateTime.now().plusDays(1));
    userMapper.updateById(user);
  }

  private String register(String username) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk()).andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private String wealthPayload() {
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
