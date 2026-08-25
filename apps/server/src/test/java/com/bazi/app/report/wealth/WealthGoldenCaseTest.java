package com.bazi.app.report.wealth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class WealthGoldenCaseTest {

  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void reviewedCasesKeepThreeYearsFivePathsAndEvidenceBoundaries() throws Exception {
    InputStream input = getClass().getResourceAsStream("/report/wealth-v2-golden-cases.json");
    assertNotNull(input, "wealth v2 golden cases must exist");
    List<GoldenCase> cases = objectMapper.readValue(input, new TypeReference<>() {});
    assertFalse(cases.isEmpty());

    for (GoldenCase golden : cases) {
      PaipanRequest request = requestFor(golden.fixtureId());
      PaipanResultDto chart = new BaziService().paipan(request);
      Clock clock = Clock.fixed(
          LocalDate.parse(golden.asOf()).atStartOfDay(ZONE).toInstant(), ZONE);
      WealthPeriodEvaluation evaluation = new WealthPathEvaluator().evaluate(
          new WealthFactExtractor().extract(
              chart,
              new AnnualContextFactory(clock)
                  .create(request, chart, ReportHorizon.of(golden.horizonYears()))));
      WealthNarrativePlan plan = new WealthNarrativePlanner().plan(evaluation);

      assertEquals(golden.requiredYears(),
          evaluation.years().stream().map(WealthYearEvaluation::year).toList(),
          golden.fixtureId());
      assertEquals(Set.copyOf(golden.requiredPaths()),
          evaluation.years().get(0).paths().keySet().stream()
              .map(WealthPath::code)
              .collect(Collectors.toUnmodifiableSet()),
          golden.fixtureId());
      assertEquals(golden.horizonYears(), plan.horizonYears(), golden.fixtureId());
      assertEquals(golden.horizonYears(), plan.years().size(), golden.fixtureId());
      assertEquals(3, plan.route().size(), golden.fixtureId());

      Set<String> evidenceKeys = evaluation.years().stream()
          .flatMap(year -> year.paths().values().stream())
          .flatMap(path -> path.scoredEvidence().stream())
          .map(item -> item.evidence().key())
          .collect(Collectors.toUnmodifiableSet());
      for (String forbidden : golden.forbiddenEvidenceKeys()) {
        assertFalse(evidenceKeys.contains(forbidden),
            golden.fixtureId() + " contains forbidden evidence " + forbidden);
      }
    }
  }

  private PaipanRequest requestFor(String fixtureId) {
    if ("lin-1995-10-08-1430".equals(fixtureId)) {
      return new PaipanRequest(
          "林先生", "male", "1995-10-08T14:30:00", "上海", false);
    }
    throw new IllegalArgumentException("unknown wealth golden fixture: " + fixtureId);
  }

  private record GoldenCase(
      String fixtureId,
      String asOf,
      int horizonYears,
      List<Integer> requiredYears,
      List<String> requiredPaths,
      List<String> forbiddenEvidenceKeys) {}
}
