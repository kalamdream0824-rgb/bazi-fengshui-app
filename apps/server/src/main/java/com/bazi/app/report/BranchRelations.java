package com.bazi.app.report;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BranchRelations {

  private static final Set<String> BRANCHES = Set.of(
      "子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥");
  private static final Set<String> CLASHES = symmetric(
      "子午", "丑未", "寅申", "卯酉", "辰戌", "巳亥");
  private static final Set<String> HARMONIES = symmetric(
      "子丑", "寅亥", "卯戌", "辰酉", "巳申", "午未");
  private static final Set<String> HARMS = symmetric(
      "子未", "丑午", "寅巳", "卯辰", "申亥", "酉戌");
  private static final Set<String> BREAKS = symmetric(
      "子酉", "丑辰", "寅亥", "卯午", "巳申", "未戌");
  private static final Set<String> PUNISHMENTS = symmetric(
      "子卯", "寅巳", "巳申", "申寅", "丑戌", "戌未", "未丑");
  private static final Set<String> SELF_PUNISHMENTS = Set.of("辰", "午", "酉", "亥");

  private BranchRelations() {}

  public static Set<BranchRelation> between(String left, String right) {
    validate(left);
    validate(right);
    String pair = left + right;
    EnumSet<BranchRelation> relations = EnumSet.noneOf(BranchRelation.class);
    if (CLASHES.contains(pair)) relations.add(BranchRelation.CLASH);
    if (HARMONIES.contains(pair)) relations.add(BranchRelation.HARMONY);
    if (HARMS.contains(pair)) relations.add(BranchRelation.HARM);
    if (BREAKS.contains(pair)) relations.add(BranchRelation.BREAK);
    if (PUNISHMENTS.contains(pair) || left.equals(right) && SELF_PUNISHMENTS.contains(left)) {
      relations.add(BranchRelation.PUNISHMENT);
    }
    return Set.copyOf(relations);
  }

  private static void validate(String branch) {
    if (!BRANCHES.contains(branch)) {
      throw new IllegalArgumentException("unknown earthly branch: " + branch);
    }
  }

  private static Set<String> symmetric(String... pairs) {
    Set<String> result = new LinkedHashSet<>();
    for (String pair : pairs) {
      if (pair.codePointCount(0, pair.length()) != 2) {
        throw new IllegalArgumentException("branch relation pair must contain two branches: " + pair);
      }
      int split = pair.offsetByCodePoints(0, 1);
      String left = pair.substring(0, split);
      String right = pair.substring(split);
      result.add(left + right);
      result.add(right + left);
    }
    return Set.copyOf(result);
  }
}
