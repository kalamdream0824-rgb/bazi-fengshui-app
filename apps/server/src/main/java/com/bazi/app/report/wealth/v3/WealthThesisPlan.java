package com.bazi.app.report.wealth.v3;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

/** Evidence-bearing reading plan for the report-level wealth thesis. */
public record WealthThesisPlan(List<YearFocus> years) {
  public static final String VERSION = "wealth-thesis-v1";
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final Set<String> STANCES = Set.of("supportive", "mixed", "restricted");
  private static final Set<String> STRENGTHS = Set.of("limited", "supported", "pronounced");

  public WealthThesisPlan {
    years = years == null ? List.of() : List.copyOf(years);
    if (years.isEmpty() || years.stream().map(YearFocus::year).distinct().count() != years.size()) {
      throw new IllegalArgumentException("wealth thesis requires distinct report years");
    }
  }

  public String plannerVersion() {
    return VERSION;
  }

  public List<String> decisionIds() {
    return years.stream().flatMap(year -> year.focusDecisionIds().stream()).distinct().toList();
  }

  /** Selected evidence values define meaning; the digest is never used to choose copy. */
  public String evidenceSignature() {
    return digest(json(List.of(VERSION, years.stream().map(YearFocus::semanticKey).toList())));
  }

  public record YearFocus(WealthSemanticClaim claim, String focusState, List<String> primaryPaths,
      List<String> focusDecisionIds,
      String secondaryPath, String secondaryObjectText,
      List<Citation> citations, List<String> dominantEvidenceIds) {
    public YearFocus {
      if (claim == null || !STANCES.contains(required(claim.stance()))
          || !STRENGTHS.contains(required(claim.strength()))) {
        throw new IllegalArgumentException("invalid wealth thesis focus");
      }
      required(focusState);
      citations = citations == null ? List.of() : citations.stream().distinct()
          .sorted(Comparator.comparing(citation -> citation.evidence().id())).toList();
      primaryPaths = primaryPaths == null ? List.of() : primaryPaths.stream().distinct().toList();
      focusDecisionIds = focusDecisionIds == null ? List.of()
          : focusDecisionIds.stream().distinct().sorted().toList();
      dominantEvidenceIds = dominantEvidenceIds == null ? List.of()
          : dominantEvidenceIds.stream().distinct().sorted().toList();
      Set<String> citedIds = citations.stream().map(citation -> citation.evidence().id())
          .collect(java.util.stream.Collectors.toUnmodifiableSet());
      Set<String> allowedPaths = secondaryPath == null
          ? Set.of(claim.path()) : Set.of(claim.path(), secondaryPath);
      if (secondaryPath != null) required(secondaryObjectText);
      if (citations.isEmpty() || dominantEvidenceIds.isEmpty() || focusDecisionIds.isEmpty()
          || !focusDecisionIds.contains(claim.decisionId())
          || !citedIds.containsAll(dominantEvidenceIds)
          || citations.stream().anyMatch(citation -> !allowedPaths.contains(citation.evidence().path()))) {
        throw new IllegalArgumentException("untraceable wealth thesis focus");
      }
    }

    public int year() { return claim.year(); }
    public String themeKey() { return claim.themeKey(); }
    public String path() { return claim.path(); }
    public String stance() { return claim.stance(); }
    public String strength() { return claim.strength(); }
    public String objectKey() { return claim.objectKey(); }
    public String objectText() { return claim.objectText(); }
    public String evidenceAngle() { return claim.evidenceAngle(); }
    public String evidenceDetailKey() { return claim.evidenceDetailKey(); }
    public String decisionId() { return claim.decisionId(); }

    private String semanticKey() {
      return json(List.of(claim.semanticKey(), year(), focusState, primaryPaths, focusDecisionIds,
          secondaryPath == null ? "" : secondaryPath,
          secondaryObjectText == null ? "" : secondaryObjectText,
          citations.stream().map(Citation::semanticKey).toList(), dominantEvidenceIds));
    }
  }

  public record Citation(WealthAssessment.Evidence evidence, List<WealthAssessment.Fact> roots) {
    public Citation {
      if (evidence == null || evidence.weight() == 0) {
        throw new IllegalArgumentException("invalid wealth thesis citation");
      }
      roots = roots == null ? List.of() : roots.stream().distinct()
          .sorted(Comparator.comparing(WealthAssessment.Fact::id)).toList();
      Set<String> rootIds = roots.stream().map(WealthAssessment.Fact::id)
          .collect(java.util.stream.Collectors.toUnmodifiableSet());
      if (roots.isEmpty() || !rootIds.equals(Set.copyOf(evidence.rootFactIds()))) {
        throw new IllegalArgumentException("wealth thesis citation has unresolved roots");
      }
    }

    private String semanticKey() {
      return json(List.of(evidence.path(), evidence.ruleKey(), evidence.factKey(),
          evidence.family().name(), evidence.weight(), roots.stream()
              .map(root -> List.of(root.kind(), root.code(), root.value())).toList()));
    }
  }

  private static String required(String value) {
    if (value == null || value.isBlank()) throw new IllegalArgumentException("blank wealth thesis field");
    return value;
  }

  private static String json(Object value) {
    try {
      return JSON.writeValueAsString(value);
    } catch (JsonProcessingException error) {
      throw new IllegalStateException("cannot encode wealth thesis plan", error);
    }
  }

  private static String digest(String value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
          .digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }
}
