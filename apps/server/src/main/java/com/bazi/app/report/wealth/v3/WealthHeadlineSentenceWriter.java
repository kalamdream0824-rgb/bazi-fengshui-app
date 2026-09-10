package com.bazi.app.report.wealth.v3;

/** Writes deterministic complete sentences from an explicit headline meaning. */
public final class WealthHeadlineSentenceWriter {

  public String write(WealthHeadlineSentenceSpec spec) {
    String judgment = spec.subject() + spec.judgment();
    if (spec.tone().equals("mixed")) {
      judgment += "，但" + spec.limitation();
    }
    String result = judgment + "。先" + spec.verification() + "。";
    new WealthChineseCopyPolicy().validateAnnualHeadline(result);
    return result;
  }
}
