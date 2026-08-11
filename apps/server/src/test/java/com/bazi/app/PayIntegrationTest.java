package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
class PayIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper om;

  private String register(String username) throws Exception {
    MvcResult register = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return om.readTree(register.getResponse().getContentAsString()).get("token").asText();
  }

  private long createOrder(String token, String plan) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/orders")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"plan\":\"" + plan + "\"}"))
        .andExpect(status().isOk())
        .andReturn();
    return om.readTree(result.getResponse().getContentAsString()).get("id").asLong();
  }

  @Test
  void createOrderThenMockPayActivatesMembership() throws Exception {
    String token = register("payflow1");
    long orderId = createOrder(token, "member_1m");

    mvc.perform(post("/api/v1/orders")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"plan\":\"member_1m\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("pending"))
        .andExpect(jsonPath("$.amountCents").value(2990));

    mvc.perform(post("/api/v1/pay/mock-success/" + orderId)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isMember").value(true))
        .andExpect(jsonPath("$.plan").value("member_1m"));

    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isMember").value(true));
  }

  @Test
  void mockPayIsIdempotentAndNonOwnerRejected() throws Exception {
    String owner = register("payflow2");
    String other = register("payflow3");
    long orderId = createOrder(owner, "member_3m");

    MvcResult first = mvc.perform(post("/api/v1/pay/mock-success/" + orderId)
            .header("Authorization", "Bearer " + owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isMember").value(true))
        .andReturn();
    String expireFirst = om.readTree(first.getResponse().getContentAsString()).get("memberExpireAt").asText();

    MvcResult second = mvc.perform(post("/api/v1/pay/mock-success/" + orderId)
            .header("Authorization", "Bearer " + owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isMember").value(true))
        .andReturn();
    String expireSecond = om.readTree(second.getResponse().getContentAsString()).get("memberExpireAt").asText();
    assertEquals(expireFirst, expireSecond, "重复回调不得重复顺延到期");

    mvc.perform(post("/api/v1/pay/mock-success/" + orderId)
            .header("Authorization", "Bearer " + other))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
  }

  @Test
  void ordersRequireAuthAndCallbackPlaceholderRejects() throws Exception {
    mvc.perform(post("/api/v1/orders")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"plan\":\"member_1m\"}"))
        .andExpect(status().isUnauthorized());

    mvc.perform(post("/api/v1/pay/callback")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderId\":1,\"provider\":\"wechat\",\"providerTradeNo\":\"WX123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PAY_CALLBACK_NOT_READY"));
  }

  @Test
  void invalidPlanRejected() throws Exception {
    String token = register("payflow4");
    mvc.perform(post("/api/v1/orders")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"plan\":\"nope\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("PLAN_INVALID"));
  }
}
