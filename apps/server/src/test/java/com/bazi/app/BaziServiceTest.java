package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
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
      assertEquals(expected.get("taiYuan").asText(), result.taiYuan(), c.get("id").asText() + " taiYuan");
      assertEquals(expected.get("mingGong").asText(), result.mingGong(), c.get("id").asText() + " mingGong");
      assertEquals(expected.get("shenGong").asText(), result.shenGong(), c.get("id").asText() + " shenGong");
      assertEquals(
          expected.get("daYunFirst").get("ganZhi").asText(),
          result.daYun().get(0).ganZhi(),
          c.get("id").asText() + " 首步大运");
      assertEquals(
          expected.get("daYunFirst").get("naYin").asText(),
          result.daYun().get(0).naYin(),
          c.get("id").asText() + " 首步大运纳音");
      assertEquals(
          expected.get("daYunFirst").get("xunKong").asText(),
          result.daYun().get(0).xunKong(),
          c.get("id").asText() + " 首步大运旬空");
      assertEquals(
          expected.get("daYunFirst").get("shiShen").asText(),
          result.daYun().get(0).shiShen(),
          c.get("id").asText() + " 首步大运十神");
      assertEquals(
          expected.get("daYunFirst").get("starFortune").asText(),
          result.daYun().get(0).starFortune(),
          c.get("id").asText() + " 首步大运星运");
      assertEquals(
          expected.get("yunStart").get("year").asInt(),
          result.yunStart().year(),
          c.get("id").asText() + " 起运年份");
      assertEquals(
          expected.get("yunStart").get("forward").asBoolean(),
          result.yunStart().forward(),
          c.get("id").asText() + " 起运顺逆");
      assertEquals(
          expected.get("liuNianFirst").get("ganZhi").asText(),
          result.liuNianList().get(0).ganZhi(),
          c.get("id").asText() + " 首年流年");
      assertEquals(
          expected.get("liuNianFirst").get("naYin").asText(),
          result.liuNianList().get(0).naYin(),
          c.get("id").asText() + " 首年流年纳音");
      assertEquals(
          expected.get("liuNianFirst").get("xunKong").asText(),
          result.liuNianList().get(0).xunKong(),
          c.get("id").asText() + " 首年流年旬空");
      assertEquals(
          expected.get("liuNianFirst").get("starFortune").asText(),
          result.liuNianList().get(0).starFortune(),
          c.get("id").asText() + " 首年流年星运");

      for (String key : new String[] {"year", "month", "day", "time"}) {
        String ganZhi = result.pillars().get(key).gan() + result.pillars().get(key).zhi();
        assertEquals(expected.get("pillars").get(key).get("ganZhi").asText(), ganZhi, c.get("id").asText() + " " + key);
        assertEquals(
            expected.get("pillars").get(key).get("shiShen").asText(),
            result.pillars().get(key).shiShen(),
            c.get("id").asText() + " " + key + " 十神");
        assertEquals(
            om.convertValue(
                expected.get("pillars").get(key).get("hideGanShiShen"),
                new TypeReference<List<String>>() {}),
            result.pillars().get(key).hideGan().stream().map(h -> h.shiShen()).toList(),
            c.get("id").asText() + " " + key + " 副星");
        assertEquals(
            expected.get("pillars").get(key).get("ziZuo").asText(),
            result.pillars().get(key).ziZuo(),
            c.get("id").asText() + " " + key + " 自坐");
      }

      int wuXingTotal = result.wuXing().values().stream().mapToInt(Integer::intValue).sum();
      assertEquals(8, wuXingTotal, c.get("id").asText() + " 五行合计");
      assertNotNull(result.daYun());
      assertTrue(!result.daYun().isEmpty(), c.get("id").asText() + " 大运非空");
    }
  }
}
