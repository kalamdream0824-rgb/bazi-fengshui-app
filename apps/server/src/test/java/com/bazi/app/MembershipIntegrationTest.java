package com.bazi.app;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bazi.app.domain.RedeemCode;
import com.bazi.app.mapper.RedeemCodeMapper;
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
class MembershipIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper om;

  @Autowired
  private RedeemCodeMapper redeemCodeMapper;

  @Test
  void meAndRedeemFlow() throws Exception {
    MvcResult register = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"mem1\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    String token = om.readTree(register.getResponse().getContentAsString()).get("token").asText();

    RedeemCode code = new RedeemCode();
    code.setCode("VIP-2026");
    code.setPlan("member_1m");
    code.setDurationDays(30);
    code.setCreatedAt(LocalDateTime.now());
    redeemCodeMapper.insert(code);

    mvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isMember").value(false));

    mvc.perform(post("/api/v1/redeem")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"vip-2026\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isMember").value(true))
        .andExpect(jsonPath("$.plan").value("member_1m"));

    mvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
  }
}
