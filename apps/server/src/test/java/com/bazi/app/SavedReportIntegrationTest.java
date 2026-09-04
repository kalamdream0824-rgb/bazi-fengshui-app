package com.bazi.app;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.bazi.app.domain.BaziReport;
import com.bazi.app.domain.User;
import com.bazi.app.mapper.BaziReportMapper;
import com.bazi.app.mapper.UserMapper;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.report.AnnualContextFactory;
import com.bazi.app.report.ReportHorizon;
import com.bazi.app.report.relationship.*;
import com.bazi.app.service.BaziService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
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
class SavedReportIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private ObjectMapper objectMapper;

  @Autowired
  private BaziReportMapper reportMapper;

  @Autowired
  private UserMapper userMapper;

  @Test
  void createsAndReadsAStoredCareerReportSnapshot() throws Exception {
    String token = register("saved-report-owner");

    MvcResult created = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload("plain")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").isNumber())
        .andExpect(jsonPath("$.subject").value("林先生"))
        .andExpect(jsonPath("$.topic").value("career"))
        .andExpect(jsonPath("$.edition").value("plain"))
        .andExpect(jsonPath("$.contentVersion").value("career-narrative-v4"))
        .andExpect(jsonPath("$.content.thesis").isNotEmpty())
        .andExpect(jsonPath("$.content.years.length()").value(2))
        .andExpect(jsonPath("$.content.timeline.past.year").value(2025))
        .andExpect(jsonPath("$.content.timeline.present.year").value(2026))
        .andExpect(jsonPath("$.content.timeline.future.length()").value(1))
        .andExpect(jsonPath("$.content.years[0].reasons.length()").value(2))
        .andExpect(jsonPath("$.content.years[0].actions.length()").value(2))
        .andReturn();

    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mvc.perform(get("/api/v1/reports")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id))
        .andExpect(jsonPath("$[0].content.thesis").isNotEmpty());

    mvc.perform(get("/api/v1/reports/{id}", id)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.content.years[1].year").value(2027));

    assertLegacyCloneReadable(token, id, "career-narrative-v3");
  }

  @Test
  void reportUsesTheSameServerResolvedTimeWhilePreservingOriginalRequest() throws Exception {
    String token = register("saved-true-solar-report-owner");

    MvcResult trueSolar = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(trueSolarCareerPayload("1995-10-08T13:05:00", true)))
        .andExpect(status().isOk())
        .andReturn();
    MvcResult explicitEffectiveTime = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(trueSolarCareerPayload("1995-10-08T12:53:51", false)))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode trueSolarBody = objectMapper.readTree(trueSolar.getResponse().getContentAsString());
    JsonNode effectiveBody = objectMapper.readTree(
        explicitEffectiveTime.getResponse().getContentAsString());
    assertEquals(effectiveBody.get("content"), trueSolarBody.get("content"));

    BaziReport stored = reportMapper.selectById(trueSolarBody.get("id").asLong());
    JsonNode storedRequest = objectMapper.readTree(stored.getRequestJson());
    assertEquals("1995-10-08T13:05:00", storedRequest.get("solarDateTime").asText());
    assertEquals("广东省 深圳市", storedRequest.get("birthPlace").asText());
    assertEquals(true, storedRequest.get("trueSolarTime").asBoolean());
  }

  @Test
  void createsAndReadsAStoredOverallPlainReportSnapshot() throws Exception {
    String token = register("saved-overall-report-owner");

    MvcResult created = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(overallPayload("plain")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subject").value("林先生"))
        .andExpect(jsonPath("$.topic").value("overall"))
        .andExpect(jsonPath("$.edition").value("plain"))
        .andExpect(jsonPath("$.contentVersion").value("overall-narrative-v2"))
        .andExpect(jsonPath("$.content.horizonYears").value(3))
        .andExpect(jsonPath("$.content.timeline.past.year").value(2025))
        .andExpect(jsonPath("$.content.timeline.present.year").value(2026))
        .andExpect(jsonPath("$.content.timeline.future.length()").value(2))
        .andExpect(jsonPath("$.content.years.length()").value(3))
        .andExpect(jsonPath("$.content.years[0].dimensions.length()").value(4))
        .andExpect(jsonPath("$.content.years[0].actions.length()").value(2))
        .andExpect(jsonPath("$.content.years[0].primaryCode").isString())
        .andExpect(jsonPath("$.content.years[0].linkage").isNotEmpty())
        .andExpect(jsonPath("$.content.years[0].evidenceKeys").isNotEmpty())
        .andReturn();

    JsonNode snapshot = objectMapper.readTree(created.getResponse().getContentAsString());
    long id = snapshot.get("id").asLong();
    BaziReport stored = reportMapper.selectById(id);
    assertEquals(objectMapper.createObjectNode().put("source", "system").put("horizonYears", 3),
        objectMapper.readTree(stored.getContextJson()));
    assertEquals(snapshot.get("content"), objectMapper.readTree(stored.getContentJson()));

    MvcResult read = mvc.perform(get("/api/v1/reports/{id}", id)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value("overall-narrative-v2"))
        .andReturn();
    assertEquals(snapshot, objectMapper.readTree(read.getResponse().getContentAsString()));

    assertLegacyCloneReadable(token, id, "overall-narrative-v1.1");
  }

  @Test
  void readsAStoredOverallV1SnapshotWithoutLinkage() throws Exception {
    String username = "saved-overall-v1-fixture";
    String token = register(username);
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    MvcResult current = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(overallPayload("plain")))
        .andExpect(status().isOk())
        .andReturn();
    ObjectNode legacyContent = (ObjectNode) objectMapper.readTree(
        current.getResponse().getContentAsString()).get("content");
    legacyContent.withArray("years").forEach(year -> ((ObjectNode) year).remove("linkage"));
    BaziReport legacy = storedReport(
        user.getId(), "旧版综合命主", "overall", "overall-narrative-v1",
        objectMapper.writeValueAsString(legacyContent));
    reportMapper.insert(legacy);

    mvc.perform(get("/api/v1/reports/{id}", legacy.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value("overall-narrative-v1"))
        .andExpect(jsonPath("$.content.years[0].linkage").value(""));
  }

  @Test
  void rejectsOverallProfessionalWithoutSavingAReport() throws Exception {
    String token = register("saved-overall-professional");

    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(overallPayload("professional")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("OVERALL_PROFESSIONAL_NOT_AVAILABLE"));

    mvc.perform(get("/api/v1/reports")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void reportOwnershipIsolatedBetweenUsers() throws Exception {
    String owner = register("saved-report-private-owner");
    String stranger = register("saved-report-stranger");
    MvcResult created = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content(payload("professional")))
        .andExpect(status().isOk())
        .andReturn();
    long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

    mvc.perform(get("/api/v1/reports/{id}", id)
            .header("Authorization", "Bearer " + stranger))
        .andExpect(status().isNotFound());

    mvc.perform(get("/api/v1/reports")
            .header("Authorization", "Bearer " + stranger))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void savedCareerReportStillRequiresRealityContext() throws Exception {
    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + register("saved-report-no-context"))
            .contentType(MediaType.APPLICATION_JSON)
            .content(payloadWithoutContext()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("CAREER_CONTEXT_REQUIRED"));
  }

  @Test
  void createsAndReadsAStoredWealthReportSnapshot() throws Exception {
    String token = register("saved-wealth-report-owner");
    ZoneId wealthZone = ZoneId.of("Asia/Shanghai");
    LocalDate beforeCreate = LocalDate.now(wealthZone);

    MvcResult created = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayloadWithoutContext("plain")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subject").value("林先生"))
        .andExpect(jsonPath("$.topic").value("wealth"))
        .andExpect(jsonPath("$.contentVersion").value("wealth-narrative-v4"))
        .andExpect(jsonPath("$.content.zoneId").value("Asia/Shanghai"))
        .andExpect(jsonPath("$.content.timeline.past.year").value(2025))
        .andExpect(jsonPath("$.content.timeline.present.year").value(2026))
        .andExpect(jsonPath("$.content.timeline.future.length()").value(2))
        .andExpect(jsonPath("$.content.calculationVersion").value("wealth-path-v2"))
        .andExpect(jsonPath("$.content.policyVersion").value("wealth-expression-v1"))
        .andExpect(jsonPath("$.content.copyVersion").value("wealth-plain-v3.4"))
        .andExpect(jsonPath("$.content.headlinePlannerVersion").value("wealth-headline-v1"))
        .andExpect(jsonPath("$.content.pathSummaries.length()").value(5))
        .andExpect(jsonPath("$.content.years.length()").value(3))
        .andExpect(jsonPath("$.content.years[0].facts").isArray())
        .andExpect(jsonPath("$.content.years[0].evidence").isArray())
        .andExpect(jsonPath("$.content.years[0].decisions.length()").value(5))
        .andExpect(jsonPath("$.content.years[0].headlineMeta.themeKey").isString())
        .andExpect(jsonPath("$.content.years[1].headlineMeta.themeKey").isString())
        .andExpect(jsonPath("$.content.years[2].headlineMeta.themeKey").isString())
        .andReturn();

    JsonNode createdSnapshot = objectMapper.readTree(created.getResponse().getContentAsString());
    LocalDate afterCreate = LocalDate.now(wealthZone);
    LocalDate asOf = LocalDate.parse(createdSnapshot.at("/content/asOf").asText());
    assertFalse(asOf.isBefore(beforeCreate));
    assertFalse(asOf.isAfter(afterCreate));
    int startYear = asOf.getYear();
    assertEquals(startYear, createdSnapshot.at("/content/years/0/year").asInt());
    assertEquals(startYear + 1, createdSnapshot.at("/content/years/1/year").asInt());
    assertEquals(startYear + 2, createdSnapshot.at("/content/years/2/year").asInt());
    assertEquals(startYear + 1, createdSnapshot.at("/content/years/0/comparison/toYear").asInt());
    long id = createdSnapshot.get("id").asLong();
    BaziReport stored = reportMapper.selectById(id);
    assertEquals(createdSnapshot.get("content"), objectMapper.readTree(stored.getContentJson()));
    ObjectNode expectedContext = objectMapper.createObjectNode()
        .put("source", "system")
        .put("horizonYears", 3)
        .put("asOf", asOf.toString())
        .put("calculationVersion", "wealth-path-v2")
        .put("policyVersion", "wealth-expression-v1")
        .put("copyVersion", "wealth-plain-v3.4");
    assertEquals(expectedContext, objectMapper.readTree(stored.getContextJson()));

    MvcResult read = mvc.perform(get("/api/v1/reports/{id}", id)
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.topic").value("wealth"))
        .andExpect(jsonPath("$.contentVersion").value("wealth-narrative-v4"))
        .andExpect(jsonPath("$.content.years[2].year").value(startYear + 2))
        .andReturn();
    assertEquals(createdSnapshot, objectMapper.readTree(read.getResponse().getContentAsString()));

    assertLegacyCloneReadable(token, id, "wealth-narrative-v3");
  }

  @Test
  void legacyWealthContextIsAcceptedButCannotChangeV3Content() throws Exception {
    String token = register("saved-wealth-legacy-context");
    MvcResult withoutContext = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayloadWithoutContext("plain")))
        .andExpect(status().isOk())
        .andReturn();
    MvcResult withContext = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayload("plain")))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode first = objectMapper.readTree(withoutContext.getResponse().getContentAsString());
    JsonNode second = objectMapper.readTree(withContext.getResponse().getContentAsString());
    assertEquals(first.get("content"), second.get("content"));
  }

  @Test
  void readsAStoredWealthV2SnapshotWithoutRecalculatingOrRewritingIt() throws Exception {
    String username = "saved-wealth-v2-fixture";
    String token = register(username);
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    JsonNode baseline;
    try (var input = getClass().getResourceAsStream("/report/wealth-v2-baseline-20260829.json")) {
      baseline = objectMapper.readTree(input).get("cases").get(0).get("content");
    }
    BaziReport report = storedReport(user.getId(), "旧版财富命主", "wealth", "wealth-narrative-v2",
        objectMapper.writeValueAsString(baseline));
    reportMapper.insert(report);

    MvcResult read = mvc.perform(get("/api/v1/reports/{id}", report.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value("wealth-narrative-v2"))
        .andReturn();
    assertEquals(baseline, objectMapper.readTree(read.getResponse().getContentAsString()).get("content"));
    assertEquals(baseline, objectMapper.readTree(reportMapper.selectById(report.getId()).getContentJson()));
  }

  @Test
  void unknownContentVersionIsNotSilentlyReadAsACareerReport() throws Exception {
    String username = "saved-unknown-version";
    String token = register(username);
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    BaziReport report = storedReport(user.getId(), "未知版本", "wealth", "wealth-narrative-v99", "{}");
    reportMapper.insert(report);

    mvc.perform(get("/api/v1/reports/{id}", report.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("REPORT_CONTENT_VERSION_UNSUPPORTED"));
  }

  @Test
  void rejectsUnavailableWealthProfessionalWithoutSavingAReport() throws Exception {
    String token = register("saved-wealth-professional");
    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(wealthPayloadWithoutContext("professional")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("WEALTH_PROFESSIONAL_NOT_AVAILABLE"));

    mvc.perform(get("/api/v1/reports")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void readsAStableLegacyWealthV1Snapshot() throws Exception {
    String username = "saved-wealth-v1-fixture";
    String token = register(username);
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    LocalDateTime now = LocalDateTime.now();
    BaziReport report = new BaziReport();
    report.setUserId(user.getId());
    report.setSubject("旧版命主");
    report.setTopic("wealth");
    report.setEdition("plain");
    report.setStatus("ready");
    report.setContentVersion("wealth-narrative-v1");
    report.setRequestJson("{}");
    report.setContextJson("{}");
    report.setContentJson(legacyWealthV1Fixture());
    report.setCreatedAt(now);
    report.setGeneratedAt(now);
    reportMapper.insert(report);

    mvc.perform(get("/api/v1/reports/{id}", report.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value("wealth-narrative-v1"))
        .andExpect(jsonPath("$.content.contextSummary").value("旧版财富问卷摘要"))
        .andExpect(jsonPath("$.content.years[0].headline").value("旧版内容仍可阅读"));
  }

  @Test
  void createsReadsAndKeepsCalculationStableAcrossPartneredRelationshipStatuses() throws Exception {
    String token = register("saved-relationship-statuses");
    List<JsonNode> reports = new ArrayList<>();

    for (String statusCode : List.of("dating", "married")) {
      MvcResult created = mvc.perform(post("/api/v1/reports")
              .header("Authorization", "Bearer " + token)
              .contentType(MediaType.APPLICATION_JSON)
              .content(relationshipPayload("plain", statusCode)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.topic").value("relationship"))
          .andExpect(jsonPath("$.edition").value("plain"))
          .andExpect(jsonPath("$.contentVersion").value("relationship-narrative-v2"))
          .andExpect(jsonPath("$.content.relationshipStatus").value(statusCode))
          .andExpect(jsonPath("$.content.timeline.past.year").value(2025))
          .andExpect(jsonPath("$.content.timeline.present.year").value(2026))
          .andExpect(jsonPath("$.content.timeline.future.length()").value(2))
          .andExpect(jsonPath("$.content.dimensions.length()").value(5))
          .andExpect(jsonPath("$.content.years.length()").value(3))
          .andExpect(jsonPath("$.content.years[0].realitySignals.length()").value(2))
          .andExpect(jsonPath("$.content.years[0].actions.length()").value(2))
          .andReturn();
      JsonNode report = objectMapper.readTree(created.getResponse().getContentAsString());
      reports.add(report);

      BaziReport stored = reportMapper.selectById(report.get("id").asLong());
      assertEquals(objectMapper.readTree("{\"status\":\"" + statusCode + "\"}"),
          objectMapper.readTree(stored.getContextJson()));

      mvc.perform(get("/api/v1/reports/{id}", report.get("id").asLong())
              .header("Authorization", "Bearer " + token))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content.relationshipStatus").value(statusCode));

      if ("dating".equals(statusCode)) {
        assertLegacyCloneReadable(
            token, report.get("id").asLong(), "relationship-narrative-v1");
      }
    }

    JsonNode baseline = relationshipCalculation(reports.get(0).get("content"));
    assertEquals(baseline, relationshipCalculation(reports.get(1).get("content")));
    assertEquals(2, reports.stream().map(item -> item.at("/content/thesis").asText()).distinct().count());
    assertEquals(2, reports.stream()
        .map(item -> item.at("/content/years/0/judgment").asText()).distinct().count());
  }

  @Test
  void singleReportStoresCurrentYearReadingAndBriefNextYearAsANewSnapshot() throws Exception {
    String token = register("single-current-year-owner");
    MvcResult created = mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(relationshipPayload("plain", "single")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value("relationship-single-v2"))
        .andExpect(jsonPath("$.content.relationshipStatus").value("single"))
        .andExpect(jsonPath("$.content.timeline.past.year").value(2025))
        .andExpect(jsonPath("$.content.timeline.present.year").value(2026))
        .andExpect(jsonPath("$.content.timeline.future.length()").value(1))
        .andExpect(jsonPath("$.content.horizonYears").value(2))
        .andExpect(jsonPath("$.content.currentYear").value(2026))
        .andExpect(jsonPath("$.content.outlookYear").value(2027))
        .andExpect(jsonPath("$.content.sections.length()").value(3))
        .andExpect(jsonPath("$.content.sections[0].title").value("今年有没有认识人的机会？"))
        .andExpect(jsonPath("$.content.evaluations.length()").value(2))
        .andExpect(jsonPath("$.content.years").doesNotExist())
        .andReturn();
    JsonNode snapshot = objectMapper.readTree(created.getResponse().getContentAsString());
    MvcResult read = mvc.perform(get("/api/v1/reports/{id}", snapshot.get("id").asLong())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk()).andReturn();
    assertEquals(snapshot, objectMapper.readTree(read.getResponse().getContentAsString()));
    mvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].contentVersion").value("relationship-single-v2"));

    assertLegacyCloneReadable(
        token, snapshot.get("id").asLong(), "relationship-single-v1");
  }

  @Test
  void oldSingleSnapshotRemainsThreeYearsAndDoesNotChangeWhenNewReportIsGenerated() throws Exception {
    String username = "single-legacy-snapshot";
    String token = register(username);
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    var request = new PaipanRequest("旧命主", "male", "1995-10-08T14:30:00", "上海", false);
    var chart = new BaziService().paipan(request);
    var clock = Clock.fixed(Instant.parse("2026-08-27T00:00:00Z"), ZoneId.of("Asia/Shanghai"));
    var legacy = new RelationshipNarrativePlanner().plan(new RelationshipPeriodArbitrator().arbitrate(
        new RelationshipDimensionEvaluator().evaluate(new RelationshipFactExtractor().extract(request, chart,
            new AnnualContextFactory(clock).create(request, chart, ReportHorizon.RELATIONSHIP_PRODUCT)))),
        RelationshipStatus.SINGLE);
    BaziReport old = new BaziReport();
    old.setUserId(user.getId());
    old.setSubject("旧命主");
    old.setTopic("relationship");
    old.setEdition("plain");
    old.setStatus("ready");
    old.setContentVersion("relationship-narrative-v1");
    old.setRequestJson("{}");
    old.setContextJson("{\"status\":\"single\"}");
    old.setContentJson(objectMapper.writeValueAsString(legacy));
    old.setCreatedAt(LocalDateTime.now());
    old.setGeneratedAt(LocalDateTime.now());
    reportMapper.insert(old);

    mvc.perform(post("/api/v1/reports").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(relationshipPayload("plain", "single")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value("relationship-single-v2"));
    MvcResult stored = mvc.perform(get("/api/v1/reports/{id}", old.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value("relationship-narrative-v1"))
        .andExpect(jsonPath("$.content.years.length()").value(3))
        .andReturn();
    assertEquals(objectMapper.readTree(old.getContentJson()),
        objectMapper.readTree(stored.getResponse().getContentAsString()).get("content"));
    mvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void rejectsMissingAndUnknownRelationshipStatusWithoutSaving() throws Exception {
    String token = register("saved-relationship-invalid-context");

    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(relationshipPayloadWithoutContext("plain")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("RELATIONSHIP_CONTEXT_REQUIRED"));

    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(relationshipPayload("plain", "ambiguous")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("RELATIONSHIP_STATUS_INVALID"));

    mvc.perform(get("/api/v1/reports")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  void rejectsUnavailableRelationshipProfessionalWithoutSavingAReport() throws Exception {
    String token = register("saved-relationship-professional");

    mvc.perform(post("/api/v1/reports")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .content(relationshipPayload("professional", "dating")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("RELATIONSHIP_PROFESSIONAL_NOT_AVAILABLE"));

    mvc.perform(get("/api/v1/reports")
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  private String register(String username) throws Exception {
    MvcResult result = mvc.perform(post("/api/v1/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"" + username + "\",\"password\":\"pass123\"}"))
        .andExpect(status().isOk())
        .andReturn();
    User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
    user.setPlan("member_1m");
    user.setMemberExpireAt(LocalDateTime.now().plusDays(1));
    userMapper.updateById(user);
    return objectMapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
  }

  private String payload(String edition) {
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "career",
          "edition": "%s",
          "careerContext": {
            "status": "employed",
            "goal": "promotion",
            "pace": "smooth"
          }
        }
        """.formatted(edition);
  }

  private String trueSolarCareerPayload(String solarDateTime, boolean trueSolarTime) {
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "%s",
            "birthPlace": "广东省 深圳市",
            "trueSolarTime": %s
          },
          "topic": "career",
          "edition": "plain",
          "careerContext": {
            "status": "employed",
            "goal": "promotion",
            "pace": "smooth"
          }
        }
        """.formatted(solarDateTime, trueSolarTime);
  }

  private String overallPayload(String edition) {
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "overall",
          "edition": "%s"
        }
        """.formatted(edition);
  }

  private String payloadWithoutContext() {
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "career",
          "edition": "plain"
        }
        """;
  }

  private String wealthPayload(String edition) {
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
          "edition": "%s",
          "wealthContext": {
            "incomeSource": "mixed",
            "goal": "increase_income",
            "pace": "income_fluctuating"
          }
        }
        """.formatted(edition);
  }

  private String wealthPayloadWithoutContext(String edition) {
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
          "edition": "%s"
        }
        """.formatted(edition);
  }

  private String relationshipPayload(String edition, String status) {
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "relationship",
          "edition": "%s",
          "relationshipContext": { "status": "%s" }
        }
        """.formatted(edition, status);
  }

  private String relationshipPayloadWithoutContext(String edition) {
    return """
        {
          "request": {
            "name": "林先生",
            "gender": "male",
            "solarDateTime": "1995-10-08T14:30:00",
            "birthPlace": "上海",
            "trueSolarTime": false
          },
          "topic": "relationship",
          "edition": "%s"
        }
        """.formatted(edition);
  }

  private JsonNode relationshipCalculation(JsonNode content) {
    ObjectNode result = objectMapper.createObjectNode();
    result.set("focus", objectMapper.createArrayNode()
        .add(content.get("primaryDimensionCode"))
        .add(content.get("secondaryDimensionCode"))
        .add(content.get("focusTied")));
    if (content.get("mainRisk").isNull()) {
      result.set("risk", objectMapper.nullNode());
    } else {
      ObjectNode risk = objectMapper.createObjectNode();
      risk.set("dimensionCode", content.at("/mainRisk/dimensionCode"));
      risk.set("evidenceKeys", content.at("/mainRisk/evidenceKeys"));
      result.set("risk", risk);
    }
    ArrayNode dimensions = objectMapper.createArrayNode();
    content.get("dimensions").forEach(node -> {
      ObjectNode dimension = objectMapper.createObjectNode();
      dimension.set("code", node.get("code"));
      dimension.set("status", node.get("status"));
      dimension.set("tone", node.get("tone"));
      dimension.set("support", node.get("supportingEvidenceKeys"));
      dimension.set("limit", node.get("limitingEvidenceKeys"));
      dimensions.add(dimension);
    });
    result.set("dimensions", dimensions);
    ArrayNode years = objectMapper.createArrayNode();
    content.get("years").forEach(node -> {
      ObjectNode year = objectMapper.createObjectNode();
      year.set("year", node.get("year"));
      year.set("primary", node.get("primaryDimensionCode"));
      year.set("secondary", node.get("secondaryDimensionCode"));
      year.set("risk", node.get("riskDimensionCode"));
      year.set("evidenceKeys", node.get("evidenceKeys"));
      years.add(year);
    });
    result.set("years", years);
    result.set("evidenceKeys", content.get("evidenceKeys"));
    return result;
  }

  private String legacyWealthV1Fixture() {
    return """
        {
          "thesis": "旧版财富主题",
          "contextSummary": "旧版财富问卷摘要",
          "years": [{
            "year": 2026,
            "ganZhi": "丙午",
            "stage": "第一年",
            "headline": "旧版内容仍可阅读",
            "verdict": "这是固定的旧版快照。",
            "reasons": ["旧版理由一", "旧版理由二"],
            "obstacle": "旧版限制",
            "actions": ["旧版行动一", "旧版行动二"],
            "changeCondition": "旧版变化条件",
            "evidenceKeys": ["legacy.fixture"],
            "evidence": [],
            "counterEvidence": [],
            "confidence": "中"
          }],
          "route": ["旧版路线"]
        }
        """;
  }

  private BaziReport storedReport(Long userId, String subject, String topic, String version, String content) {
    LocalDateTime now = LocalDateTime.now();
    BaziReport report = new BaziReport();
    report.setUserId(userId);
    report.setSubject(subject);
    report.setTopic(topic);
    report.setEdition("plain");
    report.setStatus("ready");
    report.setContentVersion(version);
    report.setRequestJson("{}");
    report.setContextJson("{}");
    report.setContentJson(content);
    report.setCreatedAt(now);
    report.setGeneratedAt(now);
    return report;
  }

  private void assertLegacyCloneReadable(
      String token,
      long sourceId,
      String legacyVersion) throws Exception {
    BaziReport source = reportMapper.selectById(sourceId);
    ObjectNode legacyContent = (ObjectNode) objectMapper.readTree(source.getContentJson());
    legacyContent.remove("timeline");
    BaziReport legacy = storedReport(
        source.getUserId(),
        source.getSubject(),
        source.getTopic(),
        legacyVersion,
        objectMapper.writeValueAsString(legacyContent));
    reportMapper.insert(legacy);

    mvc.perform(get("/api/v1/reports/{id}", legacy.getId())
            .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.contentVersion").value(legacyVersion))
        .andExpect(jsonPath("$.content.timeline").doesNotExist());
  }
}
