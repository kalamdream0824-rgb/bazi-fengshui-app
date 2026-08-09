package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class BaziServiceTest {

  private final BaziService service = new BaziService();
  private final ObjectMapper om = new ObjectMapper();

  @Test
  void fixturesConsistency() throws Exception {
    JsonNode root = om.readTree(Path.of("..", "..", "contracts", "fixtures", "bazi-cases.json").toFile());
    JsonNode cases = root.get("cases");
    assertTrue(cases.isArray() && cases.size() >= 6);

    for (JsonNode c : cases) {
      JsonNode req = c.get("request");
      PaipanRequest request = new PaipanRequest(
          null,
          req.get("gender").asText(),
          req.get("solarDateTime").asText(),
          null,
          false);
      PaipanResultDto result = service.paipan(request);
      JsonNode expected = c.get("expected");

      assertEquals(expected.get("lunarText").asText(), result.lunarText(), c.get("id").asText() + " lunarText");
      assertEquals(expected.get("shengXiao").asText(), result.shengXiao(), c.get("id").asText() + " shengXiao");

      for (String key : new String[] {"year", "month", "day", "time"}) {
        String ganZhi = result.pillars().get(key).gan() + result.pillars().get(key).zhi();
        assertEquals(expected.get("pillars").get(key).get("ganZhi").asText(), ganZhi, c.get("id").asText() + " " + key);
        assertEquals(
            expected.get("pillars").get(key).get("shiShen").asText(),
            result.pillars().get(key).shiShen(),
            c.get("id").asText() + " " + key + " 十神");
      }

      int wuXingTotal = result.wuXing().values().stream().mapToInt(Integer::intValue).sum();
      assertEquals(8, wuXingTotal, c.get("id").asText() + " 五行合计");
      assertNotNull(result.daYun());
      assertTrue(!result.daYun().isEmpty(), c.get("id").asText() + " 大运非空");
    }
  }
}
