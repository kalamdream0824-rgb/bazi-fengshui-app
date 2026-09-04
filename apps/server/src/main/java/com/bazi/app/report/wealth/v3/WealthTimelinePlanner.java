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
      "进账持续变化",
      "投入回报变化",
      "到账节奏变化",
      "资金责任变化",
      "日常支出压力",
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
            reviewHeadline(previous.year(), pastDecision.path()),
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
          "次判断：回看" + year + "年进账是否比此前更稳定，有没有出现明显中断",
          "隐性影响：如果" + year + "年进账金额变化不大，到账间隔是否变得不规律");
      case "skill_income" -> List.of(
          "次判断：回看" + year + "年增加投入以后，实际进账是否同步变化",
          "隐性影响：如果" + year + "年比以前更忙，最后留下的钱是否反而没有增加");
      case "project_income" -> List.of(
          "次判断：回看" + year + "年预计进账是否按时到账，有没有明显延后",
          "隐性影响：如果" + year + "年到账节奏改变，日常支出安排是否受到影响");
      case "cooperation_income" -> List.of(
          "次判断：回看" + year + "年与他人有关的钱是否增加，责任和用途是否清楚",
          "隐性影响：如果" + year + "年有共同开销，最后承担的部分是否超出原先预期");
      case "retention" -> List.of(
          "次判断：回看" + year + "年支出压力是否增加，固定开销有没有变多",
          "隐性影响：如果" + year + "年进账提高，最后留下的钱是否也跟着增加");
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String reviewHeadline(int year, String path) {
    return switch (path) {
      case "stable_income" -> "主判断：" + year + "年最值得回看的是进账能否持续";
      case "skill_income" -> "主判断：" + year + "年最值得回看的是投入与进账是否相称";
      case "project_income" -> "主判断：" + year + "年最值得回看的是预计与实际到账是否错开";
      case "cooperation_income" -> "主判断：" + year + "年最值得回看的是与他人有关的钱是否增加责任";
      case "retention" -> "主判断：" + year + "年最值得回看的是进账最终能否留下";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String pastBridge(String path) {
    return switch (path) {
      case "stable_income" -> "去年只用来核对进账是否稳定，再看今年怎么安排。";
      case "skill_income" -> "去年只用来核对投入与进账是否相称，再看今年怎么调整。";
      case "project_income" -> "去年只用来核对预计和实际到账的差距，再看今年怎么安排。";
      case "cooperation_income" -> "去年只用来核对资金责任和开销，再看今年怎么分配。";
      case "retention" -> "去年只用来核对钱最后留下多少，再看今年怎么调整。";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String presentHeadline(String path) {
    return switch (path) {
      case "stable_income" -> "今年先看持续进账能否稳定到账";
      case "skill_income" -> "今年先看增加投入后进账是否同步变化";
      case "project_income" -> "今年先看预计进账能否按时到账";
      case "cooperation_income" -> "今年先看与他人有关的钱能否分清责任";
      case "retention" -> "今年先看每月结余有没有真正增加";
      default -> throw new IllegalArgumentException("unknown wealth path: " + path);
    };
  }

  private String presentJudgment(WealthAssessment.Decision decision) {
    String object = switch (decision.path()) {
      case "stable_income" -> "持续进账";
      case "skill_income" -> "投入回报";
      case "project_income" -> "到账节奏";
      case "cooperation_income" -> "资金责任";
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
      case "skill_income" -> "先比较投入增加前后的实际进账";
      case "project_income" -> "先核对预计到账和实际到账的差距";
      case "cooperation_income" -> "先核对共同用钱时的用途和各自责任";
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
          ? year + "年先比较投入与实际进账"
          : year + "年再减少回报偏低的投入";
      case "project_income" -> index == 0
          ? year + "年先核对预计到账时间"
          : year + "年再检查到账延后的影响";
      case "cooperation_income" -> index == 0
          ? year + "年先分清共同用钱的责任"
          : year + "年再核对额外责任是否增加";
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
          ? "把增加投入前后的进账放在一起比较，看实际回报有没有提高。"
          : "减少长期占用时间、实际进账却没有增加的投入。";
      case "project_income" -> index == 0
          ? "把预计到账日期单独记下，并与实际到账日期进行核对。"
          : "到账延后时，先减少非必要支出，避免打乱原有安排。";
      case "cooperation_income" -> index == 0
          ? "涉及共同用钱时，先写清金额、用途和各自承担的部分。"
          : "共同开销发生后及时记录，避免责任一直说不清。";
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
