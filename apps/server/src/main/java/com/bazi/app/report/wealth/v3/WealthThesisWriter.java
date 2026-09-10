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
    Map<String, List<Integer>> primaryYears = new LinkedHashMap<>();
    plan.years().forEach(focus -> addYear(primaryYears, line(focus, styleSeed), focus.year()));
    String primary = primaryYears.entrySet().stream()
        .map(entry -> yearLabel(entry.getValue()) + entry.getKey())
        .collect(Collectors.joining());
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
    return primary + (secondaryItems.isBlank() ? "" : "另需核对：" + secondaryItems + "。");
  }

  private String line(YearFocus focus, String styleSeed) {
    String tied = focus.focusState().equals("tied")
        ? focus.primaryPaths().stream().map(WealthPlainCopyV3::label)
            .collect(Collectors.joining("、")) + "要一起看："
        : "";
    var writer = new WealthSemanticClaimWriter();
    return tied + writer.conclusion(focus.claim(), styleSeed)
        + "；" + writer.check(focus.claim(), styleSeed) + "。";
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
