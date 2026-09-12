package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.wealth.v3.WealthThesisPlan.YearFocus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Writes a short, plain-language conclusion from the selected objects and evidence context. */
public final class WealthThesisWriter {

  public String write(WealthThesisPlan plan) {
    Objects.requireNonNull(plan, "wealth thesis plan");
    String styleSeed = plan.evidenceSignature();
    String secondary = secondary(plan, styleSeed);
    String detailed = primary(plan, styleSeed, false) + secondary;
    if (detailed.length() <= 240) return detailed;
    return primary(plan, styleSeed, true) + secondary;
  }

  private String primary(WealthThesisPlan plan, String styleSeed, boolean briefCheck) {
    Map<String, List<Integer>> primaryYears = new LinkedHashMap<>();
    plan.years().forEach(focus ->
        addYear(primaryYears, line(focus, styleSeed, briefCheck), focus.year()));
    return primaryYears.entrySet().stream()
        .map(entry -> yearLabel(entry.getValue()) + entry.getKey())
        .collect(Collectors.joining());
  }

  private String secondary(WealthThesisPlan plan, String styleSeed) {
    Map<String, List<Integer>> secondaryYears = new LinkedHashMap<>();
    plan.years().stream().filter(focus -> focus.secondaryPath() != null).forEach(focus -> {
      String key = focus.secondaryPath() + "\u0000" + focus.secondaryObjectText();
      addYear(secondaryYears, key, focus.year());
    });
    String secondaryItems = secondaryYears.entrySet().stream().map(entry -> {
      String object = entry.getKey().substring(entry.getKey().indexOf('\u0000') + 1);
      String years = entry.getValue().stream().map(String::valueOf)
          .collect(Collectors.joining("、"));
      return years + "年" + object;
    }).collect(Collectors.joining("、"));
    if (secondaryItems.isBlank()) return "";
    String lead = switch (Math.floorMod(styleSeed.hashCode(), 8)) {
      case 0 -> "另需核对：";
      case 1 -> "还需核对：";
      case 2 -> "另外核对：";
      case 3 -> "同时核对：";
      case 4 -> "还需看清：";
      case 5 -> "另要记下：";
      case 6 -> "也要核实：";
      default -> "一并核对：";
    };
    return lead + secondaryItems + "。";
  }

  private String line(YearFocus focus, String styleSeed, boolean briefCheck) {
    String tied = focus.focusState().equals("tied")
        ? focus.primaryPaths().stream().map(WealthPlainCopyV3::label)
            .collect(Collectors.joining("、")) + "要一起看："
        : "";
    var writer = new WealthSemanticClaimWriter();
    return tied + writer.conclusion(focus.claim(), styleSeed)
        + "；" + (briefCheck
            ? writer.briefCheck(focus.claim(), styleSeed)
            : writer.check(focus.claim(), styleSeed)) + "。";
  }

  private void addYear(Map<String, List<Integer>> groups, String key, int year) {
    groups.compute(key, (ignored, years) -> {
      var result = years == null ? new java.util.ArrayList<Integer>()
          : new java.util.ArrayList<>(years);
      result.add(year);
      return List.copyOf(result);
    });
  }

  private String yearLabel(List<Integer> years) {
    return years.stream().map(String::valueOf).collect(Collectors.joining("、")) + "年，";
  }

}
