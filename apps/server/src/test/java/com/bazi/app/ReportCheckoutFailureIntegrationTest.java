package com.bazi.app;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bazi.app.mapper.BaziReportMapper;
import com.bazi.app.mapper.OrderMapper;
import com.bazi.app.mapper.ReportOrderLinkMapper;
import com.bazi.app.report.wealth.v3.WealthReportGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class ReportCheckoutFailureIntegrationTest {

  @Autowired private MockMvc mvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private BaziReportMapper reportMapper;
  @Autowired private OrderMapper orderMapper;
  @Autowired private ReportOrderLinkMapper linkMapper;
  @MockitoBean private WealthReportGenerator wealthReportGenerator;

  @Test
  void generationFailureCreatesNeitherReportNorOrder() throws Exception {
    when(wealthReportGenerator.generate(any(), any(), any(), any()))
        .thenThrow(new IllegalArgumentException("internal planning failure"));
    String token = register("report-checkout-failure");

    mvc.perform(post("/api/v1/reports/checkout")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayload()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("REPORT_GENERATION_UNAVAILABLE"));

    org.junit.jupiter.api.Assertions.assertEquals(0, reportMapper.selectCount(null));
    org.junit.jupiter.api.Assertions.assertEquals(0, orderMapper.selectCount(null));
    org.junit.jupiter.api.Assertions.assertEquals(0, linkMapper.selectCount(null));
  }

  @Test
  void unresolvedTrueSolarPlaceCreatesNeitherReportNorOrder() throws Exception {
    String token = register("report-checkout-unresolved-place");

    mvc.perform(post("/api/v1/reports/checkout")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "request": {
                    "name": "林先生",
                    "gender": "male",
                    "solarDateTime": "1995-10-08T13:05:00",
                    "birthPlace": "广东省 火星市",
                    "trueSolarTime": true
                  },
                  "topic": "wealth",
                  "edition": "plain"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("BIRTH_PLACE_UNRESOLVED"));

    org.junit.jupiter.api.Assertions.assertEquals(0, reportMapper.selectCount(null));
    org.junit.jupiter.api.Assertions.assertEquals(0, orderMapper.selectCount(null));
    org.junit.jupiter.api.Assertions.assertEquals(0, linkMapper.selectCount(null));
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
