package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.domain.User;
import com.bazi.app.mapper.BaziReportMapper;
import com.bazi.app.mapper.OrderMapper;
import com.bazi.app.mapper.ReportOrderLinkMapper;
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
class ReportCheckoutIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private BaziReportMapper reportMapper;
  @Autowired private OrderMapper orderMapper;
  @Autowired private ReportOrderLinkMapper linkMapper;
  @Autowired private UserMapper userMapper;

  @Test
  void checkoutCreatesServerPricedLockedReportThatCannotBeReadBeforePayment() throws Exception {
    String owner = register("report-checkout-owner");
    String other = register("report-checkout-other");

    MvcResult checkout = mvc.perform(post("/api/v1/reports/checkout")
            .header("Authorization", "Bearer " + owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayload()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.topic").value("wealth"))
        .andExpect(jsonPath("$.edition").value("plain"))
        .andExpect(jsonPath("$.amountCents").value(690))
        .andExpect(jsonPath("$.status").value("pending"))
        .andReturn();

    JsonNode body = objectMapper.readTree(checkout.getResponse().getContentAsString());
    long orderId = body.get("orderId").asLong();
    long reportId = body.get("reportId").asLong();
    assertEquals("locked", reportMapper.selectById(reportId).getStatus());
    assertEquals("pending", orderMapper.selectById(orderId).getStatus());
    assertEquals(1, orderMapper.selectCount(null));
    assertEquals(1, linkMapper.selectCount(null));

    mvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
    mvc.perform(get("/api/v1/reports/" + reportId).header("Authorization", "Bearer " + owner))
        .andExpect(status().isNotFound());
    mvc.perform(get("/api/v1/reports/" + reportId).header("Authorization", "Bearer " + other))
        .andExpect(status().isNotFound());
    assertEquals("pending", orderMapper.selectById(orderId).getStatus());
    assertEquals("locked", reportMapper.selectById(reportId).getStatus());
  }

  @Test
  void mockPaymentUnlocksExactlyOneReportAndIsIdempotent() throws Exception {
    String owner = register("report-checkout-pay-owner");
    String other = register("report-checkout-pay-other");
    JsonNode checkout = checkout(owner);
    long orderId = checkout.get("orderId").asLong();
    long reportId = checkout.get("reportId").asLong();

    mvc.perform(post("/api/v1/reports/checkout/" + orderId + "/mock-pay")
            .header("Authorization", "Bearer " + other))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));

    for (int attempt = 0; attempt < 2; attempt++) {
      mvc.perform(post("/api/v1/reports/checkout/" + orderId + "/mock-pay")
              .header("Authorization", "Bearer " + owner))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(reportId))
          .andExpect(jsonPath("$.status").value("ready"));
    }

    assertEquals("paid", orderMapper.selectById(orderId).getStatus());
    assertEquals("ready", reportMapper.selectById(reportId).getStatus());
    assertEquals(1, reportMapper.selectCount(null));
    assertEquals(1, orderMapper.selectCount(null));
    assertEquals(1, linkMapper.selectCount(null));
    mvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void individuallyPurchasedReportDoesNotConsumeMembersThreeDailyReports() throws Exception {
    String username = "member-with-purchased-report";
    String token = register(username);
    activateMembership(username);
    JsonNode checkout = checkout(token);

    mvc.perform(post("/api/v1/reports/checkout/" + checkout.get("orderId").asLong() + "/mock-pay")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());

    for (int index = 0; index < 3; index++) {
      mvc.perform(post("/api/v1/reports")
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .content(wealthPayload()))
          .andExpect(status().isOk());
    }
    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayload()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MEMBER_DAILY_REPORT_LIMIT"));
    mvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(4));
  }

  private JsonNode checkout(String token) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/reports/checkout")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayload()))
        .andExpect(status().isOk())
        .andReturn();
    return objectMapper.readTree(result.getResponse().getContentAsString());
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
