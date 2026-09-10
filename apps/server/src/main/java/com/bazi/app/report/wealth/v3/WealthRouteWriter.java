package com.bazi.app.report.wealth.v3;

/** Turns the selected report paths into one ordered, concrete money plan. */
final class WealthRouteWriter {

  String write(String path, int index, int total) {
    String step = index == 0 ? "先" : index == total - 1 ? "最后" : "接着";
    return step + switch (path) {
      case "stable_income" -> "把每次进账的日期和金额记下来，确认能否连续后再增加固定支出。";
      case "skill_income" -> "记录新增投入的时间和花费，再核对实际进账有没有增加。";
      case "project_income" -> "分开记录预计到账和实际到账，只用已经到账的钱安排支出。";
      case "cooperation_income" -> "写清共同用钱的金额、用途和各自承担的部分。";
      case "retention" -> "按月核对进账、固定开销和临时花费，只以月底实际结余为准。";
      default -> throw new IllegalArgumentException("unknown wealth route");
    };
  }
}
