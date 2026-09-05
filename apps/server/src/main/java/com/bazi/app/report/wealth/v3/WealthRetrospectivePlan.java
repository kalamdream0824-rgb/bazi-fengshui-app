package com.bazi.app.report.wealth.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Internal plan only. Absent slots are explicit; a writer must never invent their evidence. */
public record WealthRetrospectivePlan(int year, Observation primary, Observation secondary,
    Observation hidden) {
  public static final String VERSION = "wealth-retrospective-v1";
  static final Set<String> SUBJECTS = Set.of("stable_income", "skill_income", "project_income",
      "cooperation_income", "retention");
  private static final ObjectMapper JSON = new ObjectMapper();

  public WealthRetrospectivePlan {
    if (year < 1 || primary == null && (secondary != null || hidden != null)) {
      throw new IllegalArgumentException("invalid retrospective plan structure");
    }
    if (secondary != null && (primary.subject().equals(secondary.subject())
        || primary.angle().equals(secondary.angle()))) {
      throw new IllegalArgumentException("secondary review must have a different subject and angle");
    }
    if (hidden != null) {
      for (Observation earlier : secondary == null ? List.of(primary) : List.of(primary, secondary)) {
        if (hidden.angle().equals(earlier.angle()) || hidden.subject().equals(earlier.subject())
            && !java.util.Collections.disjoint(hidden.dominantRootKeys(), earlier.dominantRootKeys())) {
          throw new IllegalArgumentException("hidden review requires a distinct angle and independent same-subject roots");
        }
      }
    }
  }

  public String plannerVersion() { return VERSION; }

  public List<String> missingSlots() {
    List<String> missing = new ArrayList<>();
    if (primary == null) missing.add("primary");
    if (secondary == null) missing.add("secondary");
    if (hidden == null) missing.add("hidden");
    return List.copyOf(missing);
  }

  public String completeness() {
    return primary == null ? "empty" : missingSlots().isEmpty() ? "complete" : "partial";
  }

  public List<Observation> observations() {
    return java.util.stream.Stream.of(primary, secondary, hidden).filter(Objects::nonNull).toList();
  }

  public List<String> evidenceKeys() {
    return observations().stream().flatMap(o -> o.citations().stream())
        .map(c -> c.evidence().factKey()).distinct().sorted().toList();
  }

  /** Audit signature includes evidence values; it is not a random seed or a copy selector. */
  public String evidenceSignature() {
    return digest(json(java.util.Arrays.asList(VERSION, year,
        primary == null ? null : primary.semanticKey(),
        secondary == null ? null : secondary.semanticKey(),
        hidden == null ? null : hidden.semanticKey())));
  }

  public record Observation(String subject, String direction, String strength, String angle,
      List<Citation> citations, List<String> dominantEvidenceIds) {
    public Observation {
      if (!SUBJECTS.contains(required(subject))
          || !Set.of("supportive", "mixed", "restricted").contains(required(direction))
          || !Set.of("limited", "supported", "pronounced").contains(required(strength))) {
        throw new IllegalArgumentException("invalid retrospective observation judgment");
      }
      required(angle);
      citations = citations.stream().distinct().sorted(Comparator.comparing(c -> c.evidence().id())).toList();
      dominantEvidenceIds = dominantEvidenceIds.stream().distinct().sorted().toList();
      if (citations.isEmpty() || dominantEvidenceIds.isEmpty()
          || citations.stream().anyMatch(c -> !c.evidence().path().equals(subject))
          || !citations.stream().map(c -> c.evidence().id()).collect(Collectors.toSet())
              .containsAll(dominantEvidenceIds)
          || citations.stream().map(c -> c.evidence().id()).distinct().count() != citations.size()) {
        throw new IllegalArgumentException("untraceable retrospective observation");
      }
    }

    public Set<String> dominantRootKeys() {
      return citations.stream().filter(c -> dominantEvidenceIds.contains(c.evidence().id()))
          .flatMap(c -> c.roots().stream()).map(f -> json(List.of(f.kind(), f.code())))
          .collect(Collectors.toUnmodifiableSet());
    }

    private String semanticKey() {
      return json(List.of(subject, direction, strength, angle,
          citations.stream().map(Citation::semanticKey).distinct().sorted().toList(),
          citations.stream().filter(c -> dominantEvidenceIds.contains(c.evidence().id()))
              .map(Citation::semanticKey).distinct().sorted().toList()));
    }
  }

  /** A cited rule and its resolved root values, not just names such as annual.stem.ten_god. */
  public record Citation(WealthAssessment.Evidence evidence, List<WealthAssessment.Fact> roots) {
    public Citation {
      Objects.requireNonNull(evidence, "evidence");
      required(evidence.id()); required(evidence.ruleKey()); required(evidence.factKey());
      if (!SUBJECTS.contains(required(evidence.path())) || evidence.family() == null || evidence.weight() == 0) {
        throw new IllegalArgumentException("invalid retrospective citation");
      }
      roots = roots.stream().distinct().sorted(Comparator.comparing(WealthAssessment.Fact::id)).toList();
      if (roots.isEmpty() || roots.stream().map(WealthAssessment.Fact::id).distinct().count() != roots.size()
          || !roots.stream().map(WealthAssessment.Fact::id).collect(Collectors.toSet())
              .equals(Set.copyOf(evidence.rootFactIds()))) {
        throw new IllegalArgumentException("retrospective citation has unresolved roots");
      }
      for (var root : roots) {
        required(root.id()); required(root.kind()); required(root.code()); required(root.value());
      }
      evidence = new WealthAssessment.Evidence(evidence.id(), evidence.path(), evidence.ruleKey(),
          evidence.factKey(), evidence.family(), evidence.rootFactIds().stream().distinct().sorted().toList(),
          evidence.weight());
    }

    String semanticKey() {
      return json(List.of(evidence.path(), evidence.ruleKey(), evidence.factKey(), evidence.family().name(),
          evidence.weight(), roots.stream().map(f -> json(List.of(f.kind(), f.code(), f.value())))
              .distinct().sorted().toList()));
    }
  }

  private static String required(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("blank retrospective field");
    return value;
  }

  private static String json(Object value) {
    try { return JSON.writeValueAsString(value); }
    catch (JsonProcessingException e) { throw new IllegalStateException("cannot encode retrospective plan", e); }
  }

  private static String digest(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
  }
}
