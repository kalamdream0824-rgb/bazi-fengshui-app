package com.bazi.app.report.wealth.v3;

import com.bazi.app.report.NarrativeTimeline;
import com.bazi.app.report.NarrativeTimelineValidator;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    NarrativeTimeline.PastReview past = new WealthRetrospectiveWriter()
        .write(new WealthRetrospectiveArbitrator().plan(previous));
    WealthNarrativeV3.Year presentYear = content.years().get(0);
    String presentPath = presentPath(presentYear);
    WealthAssessment presentAssessment = assessment(presentYear);
    WealthAssessment.Decision presentDecision = decision(presentAssessment, presentPath);
    WealthAssessment.Decision retention = decision(presentAssessment, "retention");
    WealthSemanticClaim presentClaim = new WealthSemanticClaimResolver()
        .resolve(presentAssessment, presentPath);
    String styleSeed = content.thesis().text();

    List<NarrativeTimeline.FutureStep> future = new ArrayList<>();
    String previousActionPath = null;
    for (int index = 1; index < content.years().size(); index++) {
      WealthNarrativeV3.Year year = content.years().get(index);
      WealthNarrativeV3.Block selected = selectAction(year.actions(), previousActionPath, year);
      String path = actionPath(selected);
      List<WealthAssessment.Decision> actionDecisions = decisionsFor(selected, year);
      WealthAssessment.Decision actionDecision = actionDecisions.get(0);
      WealthSemanticClaim actionClaim = new WealthSemanticClaimResolver()
          .resolve(assessment(year), actionDecision.path());
      future.add(new NarrativeTimeline.FutureStep(
          year.year(),
          new WealthSemanticClaimWriter().futureHeadline(actionClaim, styleSeed),
          new WealthSemanticClaimWriter().action(actionClaim, index - 1, styleSeed),
          evidenceKeys(assessment(year), actionDecisions)));
      previousActionPath = path;
    }

    NarrativeTimeline timeline = new NarrativeTimeline(
        past,
        new NarrativeTimeline.PresentReading(
            presentYear.year(),
            new WealthSemanticClaimWriter().presentHeadline(presentClaim, styleSeed),
            presentJudgment(presentClaim, styleSeed),
            presentPriority(presentClaim, retention, styleSeed),
            evidenceKeys(presentAssessment, distinctDecisions(presentDecision, retention))),
        future);
    return new NarrativeTimelineValidator(CORE_PHRASES).validate(timeline);
  }

  private WealthAssessment.Decision strongestDecision(WealthAssessment assessment) {
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
    WealthAssessment.Decision selected = strongestDecision(assessment(year));
    return selected.path();
  }

  private String presentJudgment(WealthSemanticClaim claim, String styleSeed) {
    var writer = new WealthSemanticClaimWriter();
    return "今年，" + writer.conclusion(claim, styleSeed + "\u0000present") + "；"
        + writer.check(claim, styleSeed + "\u0000present") + "。";
  }

  private String presentPriority(WealthSemanticClaim claim, WealthAssessment.Decision retention,
      String styleSeed) {
    String first = new WealthSemanticClaimWriter()
        .check(claim, styleSeed + "\u0000priority");
    String lead = priorityLead(priorityVariant(claim, styleSeed, 16));
    int tailVariant = priorityVariant(claim, styleSeed + "\u0000tail", 4);
    return retention.limitationWeight() > 0
        ? lead + first + limitedPriorityTail(tailVariant)
        : lead + first + openPriorityTail(tailVariant);
  }

  private String priorityLead(int variant) {
    return switch (variant) {
      case 0 -> "先";
      case 1 -> "当前先";
      case 2 -> "安排支出前，先";
      case 3 -> "这一阶段先";
      case 4 -> "眼下先";
      case 5 -> "决定新增花费前，先";
      case 6 -> "处理今年收支时，先";
      case 7 -> "开始安排资金前，先";
      case 8 -> "今年首先";
      case 9 -> "先把基础情况弄清：";
      case 10 -> "先从实际记录入手：";
      case 11 -> "现在要先";
      case 12 -> "目前先";
      case 13 -> "眼下要先";
      case 14 -> "做判断前，先";
      default -> "决定下一步前，先";
    };
  }

  private String limitedPriorityTail(int variant) {
    return switch (variant) {
      case 0 -> "，然后从已到账的钱里预留日常开销，避免临时支出压低实际结余。";
      case 1 -> "；核对完成后，再从实收金额中留出日常开销，避免临时支出减少结余。";
      case 2 -> "，随后按实际到账金额先留出日常开销，防止临时花费压低结余。";
      default -> "；确认真实进账后，先预留日常开销，再决定其余的钱如何使用。";
    };
  }

  private String openPriorityTail(int variant) {
    return switch (variant) {
      case 0 -> "，月底再核对实际留下多少钱。";
      case 1 -> "；到月底再查看真正留下了多少钱。";
      case 2 -> "，月末再按真实记录计算结余。";
      default -> "；月底要再对照进账和支出，确认实际结余。";
    };
  }

  private int priorityVariant(WealthSemanticClaim claim, String styleSeed, int bound) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256").digest(
          (claim.semanticKey() + "\u0000" + styleSeed + "\u0000priority-copy")
              .getBytes(StandardCharsets.UTF_8));
      return Math.floorMod(ByteBuffer.wrap(digest, 8, Integer.BYTES).getInt(), bound);
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  private WealthNarrativeV3.Block selectAction(
      List<WealthNarrativeV3.Block> actions,
      String previousPath,
      WealthNarrativeV3.Year year) {
    if (actions.isEmpty()) throw new IllegalArgumentException("wealth future year requires an action");
    List<WealthNarrativeV3.Block> traceable = actions.stream()
        .filter(action -> actionHasEvidence(action, year))
        .toList();
    if (traceable.isEmpty()) {
      throw new IllegalArgumentException("wealth future year requires an evidence-backed action");
    }
    if (previousPath != null) {
      for (WealthNarrativeV3.Block action : traceable) {
        if (!previousPath.equals(actionPath(action))) return action;
      }
    }
    return traceable.get(0);
  }

  private boolean actionHasEvidence(WealthNarrativeV3.Block action, WealthNarrativeV3.Year year) {
    return year.decisions().stream()
        .filter(decision -> action.decisionIds().contains(decision.id()))
        .anyMatch(decision -> !decision.supportingEvidenceIds().isEmpty()
            || !decision.limitingEvidenceIds().isEmpty());
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
