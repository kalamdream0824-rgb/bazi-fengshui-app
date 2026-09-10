package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.EvidenceFamily;
import com.bazi.app.report.wealth.v3.WealthAssessment.Decision;
import com.bazi.app.report.wealth.v3.WealthAssessment.Evidence;
import com.bazi.app.report.wealth.v3.WealthAssessment.Fact;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resolves the concrete object and evidence angle behind one annual money decision. */
public final class WealthSemanticClaimResolver {

  public WealthSemanticClaim resolve(WealthAssessment assessment, String path) {
    Decision decision = assessment.decisions().stream()
        .filter(item -> item.path().equals(path)).findFirst().orElseThrow(() ->
            new IllegalArgumentException("wealth claim decision is not traceable: " + path));
    Map<String, Evidence> evidenceById = new LinkedHashMap<>();
    assessment.evidence().forEach(evidence -> evidenceById.put(evidence.id(), evidence));
    Map<String, Fact> factById = new LinkedHashMap<>();
    assessment.facts().forEach(fact -> factById.put(fact.id(), fact));
    List<String> evidenceIds = new ArrayList<>(decision.supportingEvidenceIds());
    evidenceIds.addAll(decision.limitingEvidenceIds());
    List<Evidence> evidence = evidenceIds.stream().distinct().map(evidenceById::get).map(item -> {
      if (item == null || !factById.keySet().containsAll(item.rootFactIds())) {
        throw new IllegalArgumentException("wealth claim evidence is not traceable");
      }
      return item;
    }).toList();
    Evidence dominant = evidence.stream().sorted(DOMINANT_ORDER).findFirst().orElseThrow(() ->
        new IllegalArgumentException("wealth claim requires evidence"));
    String angle = evidenceAngle(dominant);
    String detailKey = evidenceDetailKey(dominant, factById);
    var entries = WealthHeadlineVocabulary.forDecision(decision.path(), decision.stance());
    if (entries.isEmpty()) {
      throw new IllegalArgumentException("wealth claim has no concrete object vocabulary");
    }
    var entry = entries.get(objectVariant(angle));
    List<String> semanticEvidenceKeys = evidence.stream()
        .map(item -> semanticEvidenceKey(item, factById))
        .sorted()
        .toList();
    return new WealthSemanticClaim(
        assessment.year(), entry.themeKey(), decision.path(), decision.stance(), decision.strength(),
        entry.objectKey(), entry.objectText(), angle, detailKey, decision.id(),
        decision.supportingEvidenceIds(), decision.limitingEvidenceIds(), List.of(dominant.id()),
        semanticEvidenceKeys);
  }

  private static final Comparator<Evidence> DOMINANT_ORDER = Comparator
      .comparingLong((Evidence evidence) -> Math.abs((long) evidence.weight())).reversed()
      .thenComparing(Comparator.comparingInt((Evidence evidence) ->
          specificity(evidence.family())).reversed())
      .thenComparing(Evidence::id);

  private static int specificity(EvidenceFamily family) {
    return switch (family) {
      case ANNUAL_TRIGGER -> 4;
      case DAYUN_CONTEXT -> 3;
      case NATAL_COMBINATION -> 2;
      case NATAL_STRUCTURE -> 1;
    };
  }

  private static String evidenceAngle(Evidence evidence) {
    String key = evidence.factKey();
    return switch (evidence.family()) {
      case ANNUAL_TRIGGER -> key.startsWith("annual.branch.")
          ? "annual_relation" : "annual_direct";
      case DAYUN_CONTEXT -> "long_term_context";
      case NATAL_COMBINATION -> switch (key) {
        case "natal.combination.output_wealth" -> "input_to_income";
        case "natal.combination.wealth_capacity" -> "income_to_retention";
        case "natal.combination.peer_wealth" -> "shared_money";
        default -> "combined_pattern";
      };
      case NATAL_STRUCTURE -> key.equals("natal.balance")
          ? "income_capacity" : "usual_pattern";
    };
  }

  private static int objectVariant(String angle) {
    return switch (angle) {
      case "annual_direct", "annual_relation" -> 0;
      case "long_term_context" -> 1;
      default -> 2;
    };
  }

  private static String evidenceDetailKey(Evidence evidence, Map<String, Fact> factById) {
    String factKey = evidence.factKey();
    if (factKey.endsWith("annual.stem.ten_god") || factKey.endsWith("dayun.stem.ten_god")) {
      String value = evidence.rootFactIds().stream().map(factById::get)
          .filter(java.util.Objects::nonNull)
          .filter(root -> root.code().endsWith("stem.ten_god"))
          .map(Fact::value).findFirst().orElse("");
      return switch (value) {
        case "正财", "偏财" -> "direct_income";
        case "食神", "伤官" -> "input_return";
        case "正官", "七杀" -> "responsibility_limit";
        case "比肩", "劫财" -> "shared_competition";
        case "正印", "偏印" -> "preparation_support";
        default -> "generic";
      };
    }
    if (factKey.contains(".harmony.")) return "relation_coordination";
    if (factKey.contains(".clash.") || factKey.contains(".punishment.")
        || factKey.contains(".harm.") || factKey.contains(".break.")) {
      return "relation_disruption";
    }
    return switch (factKey) {
      case "natal.combination.output_wealth" -> "input_return";
      case "natal.combination.wealth_capacity" -> "retention_capacity";
      case "natal.combination.peer_wealth" -> "shared_competition";
      case "natal.balance" -> "capacity";
      default -> "generic";
    };
  }

  private static String semanticEvidenceKey(Evidence evidence, Map<String, Fact> factById) {
    String roots = evidence.rootFactIds().stream().map(factById::get)
        .filter(java.util.Objects::nonNull)
        .map(fact -> fact.code() + "=" + fact.value())
        .sorted().collect(java.util.stream.Collectors.joining("|"));
    return evidence.family().name() + ":" + evidence.factKey() + ":" + evidence.weight() + ":" + roots;
  }
}
