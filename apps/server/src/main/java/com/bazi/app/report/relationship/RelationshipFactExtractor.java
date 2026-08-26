package com.bazi.app.report.relationship;

import com.bazi.app.dto.HideGanDto;
import com.bazi.app.dto.PaipanRequest;
import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.dto.PillarDto;
import com.bazi.app.report.AnnualContext;
import com.bazi.app.report.AnnualFact;
import com.bazi.app.report.TenGodGroup;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public final class RelationshipFactExtractor {

  private static final List<String> PILLAR_KEYS = List.of("year", "month", "day", "time");
  private static final String RELATION_PREFIX = "annual.branch.";

  public RelationshipNatalProfile extractNatal(
      PaipanRequest request,
      PaipanResultDto chart,
      AnnualContext context) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(chart, "chart");
    Objects.requireNonNull(context, "context");
    validateChart(chart);

    String genderCode = validateGender(request.gender());
    TenGodGroup spouseStarGroup = request.isMale()
        ? TenGodGroup.WEALTH
        : TenGodGroup.AUTHORITY;
    List<RelationshipNatalProfile.TenGodOccurrence> occurrences = new ArrayList<>();
    Map<TenGodGroup, Integer> counts = new EnumMap<>(TenGodGroup.class);
    Map<String, RelationshipEvidence> evidence = new TreeMap<>();

    for (String pillarKey : PILLAR_KEYS) {
      PillarDto pillar = chart.pillars().get(pillarKey);
      if (isTenGod(pillar.shiShen())) {
        addNatalOccurrence(
            occurrences,
            counts,
            evidence,
            pillar.shiShen(),
            pillarKey + ".stem",
            true,
            spouseStarGroup);
      }
      for (int index = 0; index < pillar.hideGan().size(); index++) {
        HideGanDto hidden = pillar.hideGan().get(index);
        if (isTenGod(hidden.shiShen())) {
          addNatalOccurrence(
              occurrences,
              counts,
              evidence,
              hidden.shiShen(),
              pillarKey + ".branch.hidden." + index,
              false,
              spouseStarGroup);
        }
      }
    }

    PillarDto dayPillar = chart.pillars().get("day");
    return new RelationshipNatalProfile(
        genderCode,
        dayPillar.gan(),
        dayPillar.zhi(),
        context.natalAnalysis().wangShuaiLevel(),
        spouseStarGroup,
        occurrences,
        counts,
        List.copyOf(evidence.values()));
  }

  public List<RelationshipYearFacts> extract(
      PaipanRequest request,
      PaipanResultDto chart,
      List<AnnualContext> contexts) {
    Objects.requireNonNull(request, "request");
    Objects.requireNonNull(chart, "chart");
    if (contexts == null || contexts.isEmpty()) {
      throw new IllegalArgumentException("relationship facts require annual contexts");
    }

    RelationshipNatalProfile natal = extractNatal(request, chart, contexts.get(0));
    List<RelationshipYearFacts> years = new ArrayList<>();
    for (int index = 0; index < contexts.size(); index++) {
      AnnualContext context = contexts.get(index);
      if (index > 0 && context.year() != contexts.get(index - 1).year() + 1) {
        throw new IllegalArgumentException("relationship annual contexts must be consecutive");
      }

      Map<String, RelationshipEvidence> evidence = new TreeMap<>();
      addAnnualGroupEvidence(evidence, context, natal.spouseStarGroup());

      String dayunTenGod = context.activeDaYun() == null
          ? null
          : context.activeDaYun().shiShen();
      TenGodGroup dayunGroup = null;
      if (isTenGod(dayunTenGod)) {
        dayunGroup = TenGodGroup.fromTenGod(dayunTenGod);
        addDayunGroupEvidence(evidence, dayunTenGod, dayunGroup, natal.spouseStarGroup());
      }

      for (AnnualFact fact : context.natalRelations()) {
        addNatalRelationEvidence(evidence, fact);
      }
      for (AnnualFact fact : context.dayunRelations()) {
        addDayunRelationEvidence(evidence, fact);
      }

      years.add(new RelationshipYearFacts(
          context.year(),
          context.ganZhi(),
          context.yearStemTenGod(),
          context.yearStemGroup(),
          dayunTenGod,
          dayunGroup,
          natal,
          List.copyOf(evidence.values())));
    }
    return List.copyOf(years);
  }

  private void addNatalOccurrence(
      List<RelationshipNatalProfile.TenGodOccurrence> occurrences,
      Map<TenGodGroup, Integer> counts,
      Map<String, RelationshipEvidence> evidence,
      String tenGod,
      String position,
      boolean visible,
      TenGodGroup spouseStarGroup) {
    TenGodGroup group = TenGodGroup.fromTenGod(tenGod);
    occurrences.add(new RelationshipNatalProfile.TenGodOccurrence(
        tenGod, group, position, visible));
    counts.merge(group, 1, Integer::sum);
    String sourceKey = "natal.ten_god." + position + "." + tenGod;
    String label = position + "出现" + tenGod;

    if (group == spouseStarGroup) {
      add(evidence, sourceKey, RelationshipEvidenceFamily.NATAL_STRUCTURE,
          label, RelationshipDimension.CONNECTION, visible ? 3 : 1);
      add(evidence, sourceKey, RelationshipEvidenceFamily.NATAL_STRUCTURE,
          label, RelationshipDimension.STABILITY, 1);
    }
    if (group == TenGodGroup.OUTPUT) {
      add(evidence, sourceKey, RelationshipEvidenceFamily.NATAL_STRUCTURE,
          label, RelationshipDimension.RESPONSE, visible ? 2 : 1);
    }
    if (group == TenGodGroup.RESOURCE) {
      add(evidence, sourceKey, RelationshipEvidenceFamily.NATAL_STRUCTURE,
          label, RelationshipDimension.RESPONSE, 1);
      add(evidence, sourceKey, RelationshipEvidenceFamily.NATAL_STRUCTURE,
          label, RelationshipDimension.DAILY_COOPERATION, 1);
    }
  }

  private void addAnnualGroupEvidence(
      Map<String, RelationshipEvidence> evidence,
      AnnualContext context,
      TenGodGroup spouseStarGroup) {
    TenGodGroup group = context.yearStemGroup();
    String sourceKey = "annual.stem.group." + group.code();
    String label = "流年天干为" + context.yearStemTenGod();
    if (group == spouseStarGroup) {
      add(evidence, sourceKey, RelationshipEvidenceFamily.ANNUAL_TRIGGER,
          label, RelationshipDimension.CONNECTION, 4);
      add(evidence, sourceKey, RelationshipEvidenceFamily.ANNUAL_TRIGGER,
          label, RelationshipDimension.RESPONSE, 1);
      return;
    }
    switch (group) {
      case OUTPUT -> {
        add(evidence, sourceKey, RelationshipEvidenceFamily.ANNUAL_TRIGGER,
            label, RelationshipDimension.RESPONSE, 4);
        add(evidence, sourceKey, RelationshipEvidenceFamily.ANNUAL_TRIGGER,
            label, RelationshipDimension.CONNECTION, 1);
      }
      case RESOURCE -> {
        add(evidence, sourceKey, RelationshipEvidenceFamily.ANNUAL_TRIGGER,
            label, RelationshipDimension.RESPONSE, 2);
        add(evidence, sourceKey, RelationshipEvidenceFamily.ANNUAL_TRIGGER,
            label, RelationshipDimension.DAILY_COOPERATION, 2);
      }
      case PEER -> add(evidence, sourceKey, RelationshipEvidenceFamily.ANNUAL_TRIGGER,
          label, RelationshipDimension.BOUNDARIES, 2);
      case WEALTH, AUTHORITY -> add(
          evidence,
          sourceKey,
          RelationshipEvidenceFamily.ANNUAL_TRIGGER,
          label,
          RelationshipDimension.STABILITY,
          2);
    }
  }

  private void addDayunGroupEvidence(
      Map<String, RelationshipEvidence> evidence,
      String tenGod,
      TenGodGroup group,
      TenGodGroup spouseStarGroup) {
    String sourceKey = "dayun.stem.group." + group.code();
    String label = "大运天干为" + tenGod;
    if (group == spouseStarGroup) {
      add(evidence, sourceKey, RelationshipEvidenceFamily.DAYUN_CONTEXT,
          label, RelationshipDimension.CONNECTION, 2);
      add(evidence, sourceKey, RelationshipEvidenceFamily.DAYUN_CONTEXT,
          label, RelationshipDimension.STABILITY, 1);
    } else if (group == TenGodGroup.OUTPUT) {
      add(evidence, sourceKey, RelationshipEvidenceFamily.DAYUN_CONTEXT,
          label, RelationshipDimension.RESPONSE, 1);
    } else if (group == TenGodGroup.RESOURCE) {
      add(evidence, sourceKey, RelationshipEvidenceFamily.DAYUN_CONTEXT,
          label, RelationshipDimension.DAILY_COOPERATION, 1);
    }
  }

  private void addNatalRelationEvidence(
      Map<String, RelationshipEvidence> evidence,
      AnnualFact fact) {
    RelationKey relation = RelationKey.parse(fact.key());
    boolean spousePalace = "day".equals(relation.target());
    RelationshipEvidenceFamily family = spousePalace
        ? RelationshipEvidenceFamily.SPOUSE_PALACE
        : RelationshipEvidenceFamily.ANNUAL_TRIGGER;

    if ("harmony".equals(relation.code())) {
      if (spousePalace) {
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.DAILY_COOPERATION, 4);
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.STABILITY, 2);
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.RESPONSE, 1);
      } else {
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.DAILY_COOPERATION, 2);
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.CONNECTION, 1);
      }
      return;
    }

    if (!isDisruptive(relation.code())) {
      return;
    }
    if (!spousePalace) {
      add(evidence, fact.key(), family, fact.value(), RelationshipDimension.BOUNDARIES, -2);
      add(evidence, fact.key(), family, fact.value(), RelationshipDimension.DAILY_COOPERATION, -1);
      return;
    }

    switch (relation.code()) {
      case "clash" -> {
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.BOUNDARIES, -4);
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.STABILITY, -3);
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.DAILY_COOPERATION, -1);
      }
      case "harm" -> {
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.BOUNDARIES, -3);
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.STABILITY, -2);
      }
      case "punishment" -> {
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.BOUNDARIES, -3);
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.RESPONSE, -1);
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.STABILITY, -2);
      }
      case "break" -> {
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.BOUNDARIES, -2);
        add(evidence, fact.key(), family, fact.value(), RelationshipDimension.STABILITY, -1);
      }
      default -> throw new IllegalStateException("unsupported disruptive relation: " + relation.code());
    }
  }

  private void addDayunRelationEvidence(
      Map<String, RelationshipEvidence> evidence,
      AnnualFact fact) {
    RelationKey relation = RelationKey.parse(fact.key());
    if (!isDisruptive(relation.code())) {
      return;
    }
    add(evidence, fact.key(), RelationshipEvidenceFamily.DAYUN_CONTEXT,
        fact.value(), RelationshipDimension.BOUNDARIES, -2);
    add(evidence, fact.key(), RelationshipEvidenceFamily.DAYUN_CONTEXT,
        fact.value(), RelationshipDimension.STABILITY, -2);
  }

  private void add(
      Map<String, RelationshipEvidence> evidence,
      String sourceKey,
      RelationshipEvidenceFamily family,
      String label,
      RelationshipDimension dimension,
      int weight) {
    String key = sourceKey + "." + dimension.code();
    RelationshipEvidence item = new RelationshipEvidence(
        key, family, label, dimension, weight, List.of(sourceKey));
    if (evidence.putIfAbsent(key, item) != null) {
      throw new IllegalArgumentException("duplicate relationship evidence key: " + key);
    }
  }

  private void validateChart(PaipanResultDto chart) {
    if (chart.pillars() == null) {
      throw new IllegalArgumentException("relationship facts require four pillars");
    }
    for (String key : PILLAR_KEYS) {
      PillarDto pillar = chart.pillars().get(key);
      if (pillar == null
          || pillar.gan() == null
          || pillar.gan().isBlank()
          || pillar.zhi() == null
          || pillar.zhi().isBlank()
          || pillar.hideGan() == null) {
        throw new IllegalArgumentException("invalid relationship pillar: " + key);
      }
    }
  }

  private String validateGender(String gender) {
    if (!"male".equals(gender) && !"female".equals(gender)) {
      throw new IllegalArgumentException("relationship facts require male or female gender");
    }
    return gender;
  }

  private boolean isTenGod(String value) {
    return value != null && !value.isBlank() && !"日主".equals(value);
  }

  private boolean isDisruptive(String relation) {
    return "clash".equals(relation)
        || "harm".equals(relation)
        || "break".equals(relation)
        || "punishment".equals(relation);
  }

  private record RelationKey(String code, String target) {

    private static RelationKey parse(String key) {
      if (key == null || !key.startsWith(RELATION_PREFIX)) {
        throw new IllegalArgumentException("invalid relationship relation key: " + key);
      }
      String[] parts = key.substring(RELATION_PREFIX.length()).split("\\.");
      if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
        throw new IllegalArgumentException("invalid relationship relation key: " + key);
      }
      return new RelationKey(parts[0], parts[1]);
    }
  }
}
