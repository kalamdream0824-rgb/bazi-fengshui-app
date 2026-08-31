package com.bazi.app.report.wealth.v3;

import com.bazi.app.dto.PaipanResultDto;
import com.bazi.app.report.AnnualContext;
import java.time.LocalDate;
import java.util.List;

/** Generates one validated wealth v3 content snapshot from server-owned calculations. */
public interface WealthReportGenerator {
  WealthNarrativeV3 generate(PaipanResultDto chart, List<AnnualContext> contexts, LocalDate asOf);
}
