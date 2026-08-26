package com.bazi.app.report.relationship;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.AnnualFact;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.TenGodGroup;
import com.bazi.app.service.BaziService;
import java.lang.reflect.Executable;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RelationshipFactExtractorTest {

  private static final Clock CLOCK = Clock.fixed(
      Instant.parse("2026-08-23T00:00:00Z"), ZoneId.of("Asia/Shanghai"));

  private PaipanRequest maleRequest;
  private PaipanResultDto maleChart;
  private List<AnnualContext> maleContexts;

  @BeforeEach
  void setUp() {
    maleRequest = request("male");
    maleChart = new BaziService().paipan(maleRequest);
    maleContexts = new AnnualContextFactory(CLOCK)
        .create(maleRequest, maleChart, ReportHorizon.RELATIONSHIP_PRODUCT);
  }

  @Test
  void selectsTheSpouseStarGroupFromChartGender() {
    RelationshipFactExtractor extractor = new RelationshipFactExtractor();
    RelationshipNatalProfile male = extractor.extractNatal(
        maleRequest, maleChart, maleContexts.get(0));

    PaipanRequest femaleRequest = request("female");
    PaipanResultDto femaleChart = new BaziService().paipan(femaleRequest);
    AnnualContext femaleContext = new AnnualContextFactory(CLOCK)
        .create(femaleRequest, femaleChart, ReportHorizon.RELATIONSHIP_PRODUCT)
        .get(0);
    RelationshipNatalProfile female = extractor.extractNatal(
        femaleRequest, femaleChart, femaleContext);

    assertEquals(TenGodGroup.WEALTH, male.spouseStarGroup());
    assertEquals(TenGodGroup.AUTHORITY, female.spouseStarGroup());
    assertEquals("male", male.genderCode());
    assertEquals("female", female.genderCode());
  }

  @Test
  void keepsVisibleAndHiddenSpouseStarOccurrencesAsDifferentEvidence() {
    RelationshipNatalProfile profile = new RelationshipFactExtractor().extractNatal(
        maleRequest, maleChart, maleContexts.get(0));

    assertTrue(profile.tenGodOccurrences().stream().anyMatch(item ->
        item.group() == TenGodGroup.WEALTH
            && item.position().equals("time.stem")
            && item.visible()));
    assertTrue(profile.tenGodOccurrences().stream().anyMatch(item ->
        item.group() == TenGodGroup.WEALTH
            && item.position().startsWith("time.branch.hidden")
            && !item.visible()));
    assertEvidence(profile.evidence(), "natal.ten_god.time.stem.正财",
        RelationshipDimension.CONNECTION, 3, RelationshipEvidenceFamily.NATAL_STRUCTURE);
    assertTrue(profile.evidence().stream().anyMatch(item ->
        item.sourceFactKeys().stream().anyMatch(key ->
            key.startsWith("natal.ten_god.time.branch.hidden") && key.endsWith(".正财"))
            && item.dimension() == RelationshipDimension.CONNECTION
            && item.weight() == 1));
  }

  @Test
  void extractsThreeConsecutiveYearsAndMapsAnnualAndDayunSignals() {
    List<RelationshipYearFacts> years = new RelationshipFactExtractor().extract(
        maleRequest, maleChart, maleContexts);

    assertEquals(3, ReportHorizon.RELATIONSHIP_PRODUCT.years());
    assertEquals(List.of(2026, 2027, 2028),
        years.stream().map(RelationshipYearFacts::year).toList());
    assertEvidence(years.get(0).evidence(), "annual.stem.group.wealth",
        RelationshipDimension.CONNECTION, 4, RelationshipEvidenceFamily.ANNUAL_TRIGGER);
    assertEvidence(years.get(0).evidence(), "annual.branch.harmony.time",
        RelationshipDimension.DAILY_COOPERATION, 2,
        RelationshipEvidenceFamily.ANNUAL_TRIGGER);
    assertEvidence(years.get(0).evidence(), "annual.branch.punishment.dayun",
        RelationshipDimension.BOUNDARIES, -2,
        RelationshipEvidenceFamily.DAYUN_CONTEXT);
    assertEvidence(years.get(2).evidence(), "annual.branch.harm.year",
        RelationshipDimension.DAILY_COOPERATION, -1,
        RelationshipEvidenceFamily.ANNUAL_TRIGGER);
    assertNotNull(years.get(0).activeDayunGroup());
  }

  @Test
  void givesDayBranchRelationsSpousePalaceWeights() {
    AnnualContext base = maleContexts.get(0);
    AnnualContext withDayRelations = new AnnualContext(
        base.year(),
        base.ganZhi(),
        base.yearStemTenGod(),
        base.yearStemGroup(),
        base.activeDaYun(),
        base.natalFacts(),
        List.of(
            fact("annual.branch.harmony.day", "午与未合（日支）"),
            fact("annual.branch.clash.day", "午与子冲（日支）"),
            fact("annual.branch.harm.day", "午与丑害（日支）"),
            fact("annual.branch.punishment.day", "午与午刑（日支）")),
        base.dayunRelations(),
        base.natalAnalysis());

    RelationshipYearFacts year = new RelationshipFactExtractor()
        .extract(maleRequest, maleChart, List.of(withDayRelations))
        .get(0);

    assertEvidence(year.evidence(), "annual.branch.harmony.day",
        RelationshipDimension.DAILY_COOPERATION, 4,
        RelationshipEvidenceFamily.SPOUSE_PALACE);
    assertEvidence(year.evidence(), "annual.branch.clash.day",
        RelationshipDimension.BOUNDARIES, -4,
        RelationshipEvidenceFamily.SPOUSE_PALACE);
    assertEvidence(year.evidence(), "annual.branch.harm.day",
        RelationshipDimension.STABILITY, -2,
        RelationshipEvidenceFamily.SPOUSE_PALACE);
    assertEvidence(year.evidence(), "annual.branch.punishment.day",
        RelationshipDimension.RESPONSE, -1,
        RelationshipEvidenceFamily.SPOUSE_PALACE);
  }

  @Test
  void returnsUniqueEvidenceInStableKeyOrder() {
    RelationshipFactExtractor extractor = new RelationshipFactExtractor();
    RelationshipNatalProfile natal = extractor.extractNatal(
        maleRequest, maleChart, maleContexts.get(0));
    List<RelationshipYearFacts> years = extractor.extract(maleRequest, maleChart, maleContexts);

    assertUniqueAndSorted(natal.evidence());
    years.forEach(year -> assertUniqueAndSorted(year.evidence()));
    assertFalse(natal.evidence().isEmpty());
  }

  @Test
  void rejectsNonConsecutiveAnnualContexts() {
    List<AnnualContext> nonConsecutive = List.of(maleContexts.get(0), maleContexts.get(2));

    assertThrows(IllegalArgumentException.class,
        () -> new RelationshipFactExtractor().extract(maleRequest, maleChart, nonConsecutive));
  }

  @Test
  void calculationApiCannotReceiveRelationshipStatus() {
    List<Executable> api = new ArrayList<>(
        Arrays.asList(RelationshipFactExtractor.class.getDeclaredConstructors()));
    api.addAll(Arrays.asList(RelationshipFactExtractor.class.getDeclaredMethods()));

    assertTrue(api.stream()
        .flatMap(item -> Arrays.stream(item.getParameterTypes()))
        .noneMatch(RelationshipStatus.class::equals));
  }

  private PaipanRequest request(String gender) {
    return new PaipanRequest("林先生", gender, "1995-10-08T14:30:00", "上海", false);
  }

  private AnnualFact fact(String key, String value) {
    return new AnnualFact(key, "流年地支关系", value);
  }

  private void assertEvidence(
      List<RelationshipEvidence> evidence,
      String sourceKey,
      RelationshipDimension dimension,
      int weight,
      RelationshipEvidenceFamily family) {
    assertTrue(evidence.stream().anyMatch(item ->
        item.sourceFactKeys().contains(sourceKey)
            && item.dimension() == dimension
            && item.weight() == weight
            && item.family() == family),
        () -> "missing evidence for " + sourceKey + " / " + dimension + " / " + weight);
  }

  private void assertUniqueAndSorted(List<RelationshipEvidence> evidence) {
    List<String> keys = evidence.stream().map(RelationshipEvidence::key).toList();
    assertEquals(keys.size(), new HashSet<>(keys).size());
    assertEquals(keys.stream().sorted().toList(), keys);
    assertTrue(evidence.stream().allMatch(item -> !item.label().isBlank()));
    assertTrue(evidence.stream().allMatch(item -> !item.sourceFactKeys().isEmpty()));
  }
}
