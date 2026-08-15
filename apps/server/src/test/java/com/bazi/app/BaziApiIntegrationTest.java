package com.bazi.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
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
class BaziApiIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper om;

  private String register(String username) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return om.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private String payload() {
    return "{\"gender\":\"male\",\"solarDateTime\":\"1995-10-08T14:30:00\",\"trueSolarTime\":false}";
  }

  @Test
  void recordsRequireAuth() throws Exception {
    mvc.perform(post("/api/v1/records").contentType(MediaType.APPLICATION_JSON).content(payload()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void fullFlowCreateListDedupeIsolationDelete() throws Exception {
    String token1 = register("flow1");
    String token2 = register("flow2");

    mvc.perform(post("/api/v1/records")
            .header("Authorization", "Bearer " + token1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lunarText").value("一九九五年闰八月十四"))
        .andExpect(jsonPath("$.taiYuan").value("丙子"))
        .andExpect(jsonPath("$.pillars.year.hideGan[0].shiShen").value("比肩"))
        .andExpect(jsonPath("$.pillars.day.ziZuo").value("长生"))
        .andExpect(jsonPath("$.yunStart.year").value(2005))
        .andExpect(jsonPath("$.yunStart.forward").value(false))
        .andExpect(jsonPath("$.daYun[0].naYin").value("泉中水"))
        .andExpect(jsonPath("$.liuNianList[0].ganZhi").value("乙酉"));

    // 重复提交：去重后列表仍 1 条
    mvc.perform(post("/api/v1/records")
            .header("Authorization", "Bearer " + token1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload()))
        .andExpect(status().isOk());

    MvcResult list1 = mvc.perform(get("/api/v1/records").header("Authorization", "Bearer " + token1))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andReturn();
    long id = om.readTree(list1.getResponse().getContentAsString()).get(0).get("id").asLong();

    // 用户隔离
    mvc.perform(get("/api/v1/records").header("Authorization", "Bearer " + token2))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/v1/records/" + id).header("Authorization", "Bearer " + token2))
        .andExpect(status().isNotFound());

    // 删除
    mvc.perform(delete("/api/v1/records/" + id).header("Authorization", "Bearer " + token1))
        .andExpect(status().isNoContent());
    mvc.perform(get("/api/v1/records/" + id).header("Authorization", "Bearer " + token1))
        .andExpect(status().isNotFound());
  }
}
