package com.bazi.app.report.wealth.v3;

/** Meaning-first data used to write one complete annual headline. */
public record WealthHeadlineSentenceSpec(
    String subject,
    String tone,
    String judgment,
    String limitation,
    String verification) {
}
