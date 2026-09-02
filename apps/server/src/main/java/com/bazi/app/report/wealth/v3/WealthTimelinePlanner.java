package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.NarrativeTimelineValidator;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class WealthTimelinePlanner {

  private static final List<String> CORE_PHRASES = List.of(
      "收入来源变化",
      "固定进账安排",
      "服务重复付费",
      "项目付款条件",
      "合作分账约定",
      "日常开支上限",
      "每月实际结余");

  public NarrativeTimeline plan(WealthAssessment previous, WealthNarrativeV3 content) {
    Objects.requireNonNull(previous, "previous");
    Objects.requireNonNull(content, "content");
    if (content.years().size() != 3 || content.horizonYears() != 3) {
      throw new IllegalArgumentException("wealth timeline requires the three-year product");
    }
    if (previous.year() != content.years().get(0).year() - 1) {
      throw new IllegalArgumentException("wealth previous year must precede product years");
    }

    WealthAssessment.Decision pastDecision = reviewDecision(previous);
    WealthNarrativeV3.Year presentYear = content.years().get(0);
    String presentPath = presentPath(presentYear);
    WealthAssessment presentAssessment = assessment(presentYear);
    WealthAssessment.Decision presentDecision = decision(presentAssessment, presentPath);
    WealthAssessment.Decision retention = decision(presentAssessment, "retention");

    List<NarrativeTimeline.FutureStep> future = new ArrayList<>();
    String previousActionPath = null;
    for (int index = 1; index < content.years().size(); index++) {
      WealthNarrativeV3.Year year = content.years().get(index);
      WealthNarrativeV3.Block selected = selectAction(year.actions(), previousActionPath);
      String path = actionPath(selected);
      future.add(new NarrativeTimeline.FutureStep(
          year.year(),
          futureHeadline(year.year(), path, index - 1),
          futureAction(path, index - 1),
          evidenceKeys(assessment(year), decisionsFor(selected, year))));
      previousActionPath = path;
    }

    NarrativeTimeline timeline = new NarrativeTimeline(
        new NarrativeTimeline.PastReview(
            previous.year(),
            previous.year() + "年" + reviewLabel(pastDecision.path()) + "回看",
            pastCheckpoints(previous.year(), pastDecision.path()),
            pastBridge(pastDecision.path()),
            evidenceKeys(previous, List.of(pastDecision))),
        new NarrativeTimeline.PresentReading(
            presentYear.year(),
            presentHeadline(presentPath),
            presentJudgment(presentDecision),
            presentPriority(presentPath, retention),
            evidenceKeys(presentAssessment, distinctDecisions(presentDecision, retention))),
        future);
    return new NarrativeTimelineValidator(CORE_PHRASES).validate(timeline);
  }

  private WealthAssessment.Decision reviewDecision(WealthAssessment assessment) {
    if (!assessment.focus().primaryCandidates().isEmpty()) {
      return decision(assessment, assessment.focus().primaryCandidates().get(0));
    }
    WealthAssessment.Decision selected = null;
    int selectedWeight = -1;
    for (WealthAssessment.Decision candidate : assessment.decisions()) {
      int weight = candidate.supportWeight() + candidate.limitationWeight();
      if (weight > selectedWeight) {
        selected = candidate;
        selectedWeight = weight;
      }
    }
    if (selected == null) throw new IllegalArgumentException("wealth review requires decisions");
    return selected;
  }

  private String presentPath(WealthNarrativeV3.Year year) {
    if (!year.focus().primaryCandidates().isEmpty()) return year.focus().primaryCandidates().get(0);
    WealthAssessment.Decision selected = reviewDecision(assessment(year));
    return selected.path();
  }

  private List<String> pastCheckpoints(int year, String path) {
    return switch (path) {
      case "stable_income" -> List.of(
          "回看" + year + "年收入来源是否更稳定，工资或长期客户有没有持续",
          "如果" + year + "年固定进账有变化，到账是否仍然准时");
      case "skill_income" -> List.of(
          "回看" + year + "年靠手艺或服务得到的进账是否更稳定",
          "如果" + year + "年同一种服务有人付钱，是否出现再次购买");
      case "project_income" -> List.of(
          "回看" + year + "年按次结算的收入是否增加，回款有没有变慢",
          "如果" + year + "年额外项目变多，扣掉成本后是否真的留下钱");
      case "cooperation_income" -> List.of(
          "回看" + year + "年与别人共同做事的进账是否增加",
          "如果" + year + "年有合作收入，分账和共同开销是否事先说清");
      case "retention" -> List.of(
          "回看" + year + "年支出压力是否增加，固定开销有没有变多",
          "如果" + year + "年进账提高，最后留下的钱是否也跟着增加");
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String reviewLabel(String path) {
    return switch (path) {
      case "stable_income" -> "持续进账";
      case "skill_income" -> "服务收入";
      case "project_income" -> "项目收款";
      case "cooperation_income" -> "合作分账";
      case "retention" -> "收支结余";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String pastBridge(String path) {
    return switch (path) {
      case "stable_income" -> "去年只用来核对进账是否稳定，再看今年怎么安排。";
      case "skill_income" -> "去年只用来核对别人是否愿意付钱，再看今年怎么扩大。";
      case "project_income" -> "去年只用来核对收款和成本，再看今年要先管住什么。";
      case "cooperation_income" -> "去年只用来核对分账和开销，再看今年怎么合作。";
      case "retention" -> "去年只用来核对钱最后留下多少，再看今年怎么调整。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String presentHeadline(String path) {
    return switch (path) {
      case "stable_income" -> "今年先看持续进账能否稳定到账";
      case "skill_income" -> "今年先看哪项服务有人愿意重复付钱";
      case "project_income" -> "今年先看额外进账能否按约定收到";
      case "cooperation_income" -> "今年先看合作带来的钱能否分清";
      case "retention" -> "今年先看每月结余有没有真正增加";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String presentJudgment(WealthAssessment.Decision decision) {
    String object = switch (decision.path()) {
      case "stable_income" -> "持续进账";
      case "skill_income" -> "靠服务赚钱";
      case "project_income" -> "额外进账";
      case "cooperation_income" -> "合作进账";
      case "retention" -> "把钱留下";
      default -> throw new IllegalArgumentException("unknown wealth path: " + decision.path());
    };
    return switch (decision.stance()) {
      case "supportive" -> object + ("pronounced".equals(decision.strength())
          ? "是今年更值得认真经营的方向。"
          : "今年可以尝试，但不要提前高估。");
      case "mixed" -> object + "有机会，同时要把成本和变动算进去。";
      case "restricted" -> object + "今年限制较多，不宜先按理想结果花钱。";
      case "quiet" -> "今年没有哪种进账方式明显领先，不要只押一个方向。";
      default -> throw new IllegalArgumentException("unknown wealth stance: " + decision.stance());
    };
  }

  private String presentPriority(String path, WealthAssessment.Decision retention) {
    String first = switch (path) {
      case "stable_income" -> "先核对每月到账日期和中断可能";
      case "skill_income" -> "先核对哪项服务能带来再次付费";
      case "project_income" -> "先核对报价、收款时间和项目成本";
      case "cooperation_income" -> "先核对分账、共同开销和付款时间";
      case "retention" -> "先把固定开销和新增投入分开记录";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
    return retention.limitationWeight() > 0
        ? first + "，再留出一部分钱应对变化。"
        : first + "，再看每月实际结余。";
  }

  private WealthNarrativeV3.Block selectAction(
      List<WealthNarrativeV3.Block> actions,
      String previousPath) {
    if (actions.isEmpty()) throw new IllegalArgumentException("wealth future year requires an action");
    if (previousPath != null) {
      for (WealthNarrativeV3.Block action : actions) {
        if (!previousPath.equals(actionPath(action))) return action;
      }
    }
    return actions.get(0);
  }

  private String actionPath(WealthNarrativeV3.Block action) {
    if (action.decisionIds().isEmpty()) {
      throw new IllegalArgumentException("wealth action requires a decision");
    }
    String id = action.decisionIds().get(0);
    int marker = id.indexOf(".decision.");
    if (marker < 0) throw new IllegalArgumentException("invalid wealth action decision id: " + id);
    return id.substring(marker + ".decision.".length());
  }

  private String futureHeadline(int year, String path, int index) {
    return switch (path) {
      case "stable_income" -> index == 0
          ? year + "年先确认持续到账"
          : year + "年再准备进账中断时的安排";
      case "skill_income" -> index == 0
          ? year + "年先验证服务是否能再次收费"
          : year + "年再调整服务价格和所需时间";
      case "project_income" -> index == 0
          ? year + "年先说清报价和付款时间"
          : year + "年再核对每笔项目最后留下多少";
      case "cooperation_income" -> index == 0
          ? year + "年先写清合作分账方式"
          : year + "年再约定共同开销怎么承担";
      case "retention" -> index == 0
          ? year + "年先给日常开支设上限"
          : year + "年再核对实际收支记录";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String futureAction(String path, int index) {
    return switch (path) {
      case "stable_income" -> index == 0
          ? "把每月到账日期和可能中断的情况列清楚。"
          : "提前留出进账暂停时能覆盖日常开销的余钱。";
      case "skill_income" -> index == 0
          ? "挑一项有人愿意再次付费的服务，写清价格和内容。"
          : "记录老客户是否再次购买，再决定要不要增加种类。";
      case "project_income" -> index == 0
          ? "接项目前写清成本、付款时间和追加要求的价格。"
          : "项目结束后分开记录报价、实际收款和最后结余。";
      case "cooperation_income" -> index == 0
          ? "合作前写清谁出钱、谁做事和进账怎么分。"
          : "共同开销发生时当天记录，不留到分账时再争论。";
      case "retention" -> index == 0
          ? "给日常必需开支和新增投入分别设一个上限。"
          : "每月结束后核对实际进账和支出，再决定下月花多少。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private List<WealthAssessment.Decision> decisionsFor(
      WealthNarrativeV3.Block block,
      WealthNarrativeV3.Year year) {
    List<WealthAssessment.Decision> decisions = new ArrayList<>();
    for (String id : block.decisionIds()) {
      decisions.add(year.decisions().stream()
          .filter(decision -> decision.id().equals(id))
          .findFirst()
          .orElseThrow(() -> new IllegalArgumentException("missing wealth action decision: " + id)));
    }
    return List.copyOf(decisions);
  }

  private List<WealthAssessment.Decision> distinctDecisions(
      WealthAssessment.Decision first,
      WealthAssessment.Decision second) {
    return first.id().equals(second.id()) ? List.of(first) : List.of(first, second);
  }

  private List<String> evidenceKeys(
      WealthAssessment assessment,
      List<WealthAssessment.Decision> decisions) {
    Set<String> evidenceIds = new LinkedHashSet<>();
    for (WealthAssessment.Decision decision : decisions) {
      evidenceIds.addAll(decision.supportingEvidenceIds());
      evidenceIds.addAll(decision.limitingEvidenceIds());
    }
    LinkedHashSet<String> keys = new LinkedHashSet<>();
    assessment.evidence().stream()
        .filter(evidence -> evidenceIds.contains(evidence.id()))
        .map(WealthAssessment.Evidence::factKey)
        .filter(Objects::nonNull)
        .filter(key -> !key.isBlank())
        .forEach(keys::add);
    if (keys.isEmpty()) {
      assessment.facts().stream()
          .map(WealthAssessment.Fact::id)
          .filter(Objects::nonNull)
          .filter(key -> !key.isBlank())
          .forEach(keys::add);
    }
    if (keys.isEmpty()) throw new IllegalArgumentException("wealth timeline section requires evidence");
    return List.copyOf(keys);
  }

  private WealthAssessment assessment(WealthNarrativeV3.Year year) {
    return new WealthAssessment(
        year.year(),
        year.ganZhi(),
        year.facts(),
        year.evidence(),
        year.decisions(),
        year.focus(),
        year.risk() == null
            ? null
            : new WealthAssessment.Risk(year.risk().path(), year.risk().limitingEvidenceIds()));
  }

  private WealthAssessment.Decision decision(WealthAssessment assessment, String path) {
    return assessment.decisions().stream()
        .filter(candidate -> candidate.path().equals(path))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("missing wealth decision: " + path));
  }
}
