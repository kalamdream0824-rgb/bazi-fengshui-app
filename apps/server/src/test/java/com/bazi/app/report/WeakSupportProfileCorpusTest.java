package com.bazi.app.report;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WeakSupportProfileCorpusTest {

  private static final Path CORPUS = Path.of(
      "src/test/resources/report-golden/weak-support-profile-corpus-v1.json");
  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  void preservesSixIndependentWeakBandCasesAcrossThreeProfiles() throws Exception {
    JsonNode root = JSON.readTree(Files.readString(CORPUS));
    Map<WeakSupportProfile, Integer> counts = new EnumMap<>(WeakSupportProfile.class);
    Set<String> pillars = new LinkedHashSet<>();
    BaziService service = new BaziService();

    for (JsonNode fixture : root.path("cases")) {
      PaipanRequest request = JSON.treeToValue(fixture.path("request"), PaipanRequest.class);
      var analysis = ReportAnalysis.from(request, service.paipan(request));
      WeakSupportProfile expected = WeakSupportProfile.valueOf(
          fixture.path("expectedProfile").asText());

      assertEquals("偏弱", analysis.wangShuaiLevel(), fixture.path("id").asText());
      assertEquals(expected, analysis.weakSupportProfile(), fixture.path("id").asText());
      assertEquals(fixture.path("expectedScore").asDouble(), analysis.wangShuaiScore(),
          fixture.path("id").asText());
      assertEquals(fixture.path("expectedShadowScore").asDouble(),
          analysis.rootedSupportScore(), fixture.path("id").asText());
      counts.merge(expected, 1, Integer::sum);
      pillars.add(analysis.fact("pillars").value());
    }

    assertEquals(6, root.path("cases").size());
    assertEquals(6, pillars.size());
    assertEquals(2, counts.get(WeakSupportProfile.ROOTLESS));
    assertEquals(2, counts.get(WeakSupportProfile.ROOTED));
    assertEquals(2, counts.get(WeakSupportProfile.ROOTED_WITH_VISIBLE_RESOURCE));
  }
}
