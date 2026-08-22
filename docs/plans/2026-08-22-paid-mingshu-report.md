# Paid Mingshu Report Implementation Plan

> **For Claude:** Use `${SUPERPOWERS_SKILLS_ROOT}/skills/collaboration/executing-plans/SKILL.md` to implement this plan task-by-task.

**Goal:** Build separately priced plain-language and professional, server-generated, text-searchable five-chapter Mingshu PDFs with extensible focus topics and permanent downloads.

**Architecture:** Keep the existing React explainer for the free preview. Add one backend evidence engine that converts an owned `BaziRecord` plus `ReportTopic` into structured rule results, then selects either plain-language or professional copy through `ReportEdition`; never maintain two independent conclusion engines. Render the purchased edition with Apache FOP 2.11, persist its composed content and entitlement source, and allow permanent repeat downloads.

**Tech Stack:** React 19, TypeScript, Spring Boot 3.5/Java 17, MyBatis-Plus, Apache FOP 2.11, Apache PDFBox text extraction in tests, H2/MySQL.

---

## Delivery boundaries

- Free preview remains in `apps/web/src/pages/ReportPage.tsx` and may reuse `apps/web/src/lib/explainer.ts`.
- Paid report content is authoritative on the server; the client sends only `recordId`, `topic` and `edition`.
- Initial topics are data-driven: `overall`, `career`, `wealth`, `relationship`, `family`, `study`, `collaboration`, `life_stage`.
- Editions are separately entitled products: `plain` costs ¥6.9 (`690` cents) and `professional` costs ¥12.9 (`1290` cents).
- Every `recordId + edition + topic` is an independent report. Changing any dimension creates a new report and, without membership quota, requires a new purchase.
- Active members may successfully generate up to three new reports per Asia/Shanghai calendar day. Failed generation and repeat downloads do not consume quota.
- After the member quota is exhausted, generation remains available through single purchase with an explicit confirmation message.
- Every successfully generated report remains permanently downloadable by its owner, including after membership expiry.
- A topic is not considered shippable until it has fixture-backed evidence rules and human content review.
- Rename chapter 2 from “五行与用神” to “五行结构与平衡” until a documented, testable Yongshen method exists.
- Do not integrate a real payment provider in this plan. Reuse the existing mock payment lifecycle, but refactor fulfillment so membership and one-off reports are different products.
- Do not persist client-authored report paragraphs. Persist the server-composed `ReportDocument` JSON and its `contentVersion`.

## Scope estimate

- New files: approximately 18–24.
- Modified existing files: approximately 12–16.
- Main risk is content quality and rule review, not PDF rendering.
- Implement in three reviewable milestones: report engine, report API, payment/UI integration.

---

### Task 1: Define the report contract, edition and topic catalogues

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportTopic.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportEdition.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportDocument.java`
- Create: `apps/server/src/main/java/com/bazi/app/dto/CreateReportRequest.java`
- Create: `apps/server/src/main/java/com/bazi/app/dto/ReportDto.java`
- Test: `apps/server/src/test/java/com/bazi/app/report/ReportTopicTest.java`
- Modify: `contracts/openapi.yaml`

**Step 1: Write the failing topic parsing test**

```java
@Test
void acceptsSupportedTopicsAndRejectsUnknownValues() {
  assertEquals(ReportTopic.CAREER, ReportTopic.fromCode("career"));
  assertThrows(BusinessException.class, () -> ReportTopic.fromCode("fortune_telling"));
}

@Test
void acceptsSupportedEditionsAndRejectsUnknownValues() {
  assertEquals(ReportEdition.PLAIN, ReportEdition.fromCode("plain"));
  assertEquals(ReportEdition.PROFESSIONAL, ReportEdition.fromCode("professional"));
  assertThrows(BusinessException.class, () -> ReportEdition.fromCode("premium"));
}
```

**Step 2: Run the test and verify failure**

Run: `cd apps/server && ./mvnw -Dtest=ReportTopicTest test`

Expected: FAIL because `ReportTopic` does not exist.

**Step 3: Add the minimum contract**

```java
public enum ReportTopic {
  OVERALL("overall", "综合研读"),
  CAREER("career", "事业与职场"),
  WEALTH("wealth", "财富与资源"),
  RELATIONSHIP("relationship", "恋爱与亲密关系"),
  FAMILY("family", "婚姻与家庭"),
  STUDY("study", "学业与能力成长"),
  COLLABORATION("collaboration", "创业与合作"),
  LIFE_STAGE("life_stage", "年度与阶段节奏");
}
```

Define `ReportDocument` with immutable records:

```java
public record ReportDocument(
    String contentVersion,
    String title,
    String subject,
    String topicCode,
    String editionCode,
    List<ReportChapter> chapters,
    String disclaimer) {}

public record ReportChapter(String number, String title, List<ReportSection> sections) {}
public record ReportSection(String title, List<ReportPoint> points) {}
public record ReportPoint(String conclusion, List<String> evidence, String interpretation, String methodNote, String prompt) {}
```

`CreateReportRequest` accepts `@NotNull Long recordId`, `@NotBlank String topic` and `@NotBlank String edition`.

**Step 4: Update `contracts/openapi.yaml`**

Add schemas for `ReportTopic`, `CreateReportRequest`, `ReportDto` and paths for create/list/detail/download.

**Step 5: Run tests and commit**

Run: `cd apps/server && ./mvnw -Dtest=ReportTopicTest test`

Expected: PASS.

```bash
git add apps/server/src/main/java/com/bazi/app/report apps/server/src/main/java/com/bazi/app/dto contracts/openapi.yaml apps/server/src/test/java/com/bazi/app/report
git commit -m "feat(report): define report document contract"
```

---

### Task 2: Build the shared evidence engine and two presentation branches

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportComposer.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportEvidence.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportRule.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportRuleResult.java`
- Create: `apps/server/src/main/java/com/bazi/app/report/TopicRules.java`
- Create: `apps/server/src/main/resources/report/report-copy.yml`
- Test: `apps/server/src/test/java/com/bazi/app/report/ReportComposerTest.java`
- Reference: `apps/web/src/lib/explainerRules.ts`
- Reference: `apps/web/src/lib/explainerDictionary.ts`

**Step 1: Write a failing fixture test**

Use the existing 1995-10-08 fixture and require:

```java
ReportDocument report = composer.compose(request, result, ReportTopic.CAREER, ReportEdition.PROFESSIONAL);
assertEquals(5, report.chapters().size());
assertEquals("五行结构与平衡", report.chapters().get(1).title());
assertTrue(report.chapters().stream()
    .flatMap(chapter -> chapter.sections().stream())
    .flatMap(section -> section.points().stream())
    .allMatch(point -> !point.evidence().isEmpty()));
assertTrue(report.chapters().get(4).title().contains("事业"));
assertTrue(report.chapters().stream()
    .flatMap(chapter -> chapter.sections().stream())
    .flatMap(section -> section.points().stream())
    .anyMatch(point -> point.methodNote() != null && !point.methodNote().isBlank()));
```

Also assert the forbidden certainty terms are absent: `一定`, `注定`, `必发财`, `必离婚`, `改运`, `消灾`.

**Step 2: Implement deterministic composition**

- Chapter 1: identity, four pillars, Wangshuai evidence, Taiyuan/Minggong/Shengong.
- Chapter 2: five-element counts and relations; explicitly state that Yongshen is not concluded.
- Chapter 3: current Dayun, current Liunian and the next Dayun boundary.
- Chapter 4: Shishen combinations and Shensha placements actually present in the chart.
- Chapter 5: topic rules chosen by `ReportTopic`; each point must contain evidence, interpretation and a non-prescriptive reflection prompt.

Rules return `ReportRuleResult` with one evidence set plus `plainText`, `professionalText` and `methodNote`. `ReportComposer` selects only the purchased edition into `ReportDocument`; it must not persist or expose the other edition's paid copy. Copy lives in `report-copy.yml` so content review does not require editing layout code.

Plain-language acceptance: terminology is introduced with a direct explanation, only key evidence is shown, and each point contains a readable reflection prompt. Professional acceptance: full rule path, scores, branch relationships, method notes and school differences remain visible. Professional copy must not be merely a more obscure rewrite.

**Step 3: Add coverage for every topic**

Parameterized test: every `ReportTopic × ReportEdition` generates at least three topic points and every point has chart evidence. Also assert that both editions reach the same rule keys/conclusions for the same chart while their presentation text differs.

**Step 4: Run tests and commit**

Run: `cd apps/server && ./mvnw -Dtest=ReportComposerTest test`

Expected: PASS with all topic cases.

```bash
git add apps/server/src/main/java/com/bazi/app/report apps/server/src/main/resources/report apps/server/src/test/java/com/bazi/app/report
git commit -m "feat(report): compose five chapter narratives"
```

---

### Task 3: Generate a native-text PDF with Apache FOP

**Files:**
- Modify: `apps/server/pom.xml`
- Create: `apps/server/src/main/java/com/bazi/app/report/ReportPdfRenderer.java`
- Create: `apps/server/src/main/resources/report/mingshu.fo.xml`
- Create: `apps/server/src/main/resources/report/fop.xconf`
- Add asset: `apps/server/src/main/resources/report/fonts/NotoSerifSC-Regular.ttf`
- Add asset: `apps/server/src/main/resources/report/fonts/OFL.txt`
- Test: `apps/server/src/test/java/com/bazi/app/report/ReportPdfRendererTest.java`

**Step 1: Add a failing text-extraction test**

```java
byte[] pdf = renderer.render(report);
try (PDDocument document = Loader.loadPDF(pdf)) {
  String text = new PDFTextStripper().getText(document);
  assertTrue(text.contains("壹、命盘概览"));
  assertTrue(text.contains("五行结构与平衡"));
  assertTrue(document.getNumberOfPages() >= 5);
}
```

The test proves the PDF contains text, not only page images.

**Step 2: Add dependencies**

Use Apache FOP 2.11. Do not add iText. Keep PDFBox only for tests if FOP does not expose the needed version transitively.

**Step 3: Build the XSL-FO template**

- A4 pages, embedded Noto Serif SC.
- Cover plus five chapters.
- Plain target: 10–14 pages with terminology explanations and reduced data density.
- Professional target: 16–22 pages with method notes, full evidence tables and rule paths.
- Running footer with report number and `page-number`.
- Tables for pillars and five-element counts.
- No remote assets, JavaScript, or untrusted XML entities.

**Step 4: Verify text and visual output**

Run: `cd apps/server && ./mvnw -Dtest=ReportPdfRendererTest test`

Expected: extracted Chinese chapter text is present and the PDF opens without missing glyphs.

**Step 5: Commit**

```bash
git add apps/server/pom.xml apps/server/src/main/java/com/bazi/app/report/ReportPdfRenderer.java apps/server/src/main/resources/report apps/server/src/test/java/com/bazi/app/report/ReportPdfRendererTest.java
git commit -m "feat(report): render searchable Chinese PDF"
```

---

### Task 4: Persist reports and expose authenticated APIs

**Files:**
- Modify: `apps/server/src/main/resources/schema.sql`
- Create: `apps/server/src/main/java/com/bazi/app/domain/BaziReport.java`
- Create: `apps/server/src/main/java/com/bazi/app/mapper/BaziReportMapper.java`
- Create: `apps/server/src/main/java/com/bazi/app/service/ReportService.java`
- Create: `apps/server/src/main/java/com/bazi/app/controller/ReportController.java`
- Modify: `apps/server/src/main/java/com/bazi/app/config/WebConfig.java`
- Test: `apps/server/src/test/java/com/bazi/app/ReportIntegrationTest.java`

**Step 1: Add the report table**

```sql
CREATE TABLE IF NOT EXISTS bazi_report (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  record_id BIGINT NOT NULL,
  order_id BIGINT,
  topic VARCHAR(32) NOT NULL,
  edition VARCHAR(24) NOT NULL,
  access_source VARCHAR(24) NOT NULL,
  quota_date DATE,
  status VARCHAR(16) NOT NULL,
  content_version VARCHAR(32) NOT NULL,
  content_json LONGTEXT,
  error_message VARCHAR(255),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  generated_at TIMESTAMP
);
```

Do not store the PDF in the database in this MVP. Persist versioned composed content and render it again on download; this avoids introducing object storage before demand exists. The same `content_version` must always resolve to the same renderer so a later template upgrade cannot silently rewrite an already purchased report.

`access_source` is `single_purchase` or `membership`. A ready report is permanently downloadable by its owner; download authorization never re-checks active membership.

**Step 2: Write ownership tests**

- User A cannot create a report from User B's record.
- User A cannot view or download User B's report.
- Unsupported topic returns `400` with a stable error code.
- A ready report download returns `application/pdf` and an attachment filename.
- Member-generated reports remain downloadable after membership expiry.
- Repeat download does not consume daily quota.
- Failed generation does not consume daily quota.
- A fourth concurrent membership generation cannot pass the three-report daily limit.

**Step 3: Implement endpoints**

```text
POST /api/v1/reports                { recordId, topic, edition }
GET  /api/v1/reports
GET  /api/v1/reports/{id}
GET  /api/v1/reports/{id}/download
```

Add `/api/v1/reports/**` to `AuthInterceptor` paths. `ReportService` must resolve the record through `RecordService.get(userId, recordId)`.

For membership quota, use an Asia/Shanghai calendar date and lock the user row inside the create transaction before counting successful/in-progress `access_source=membership` reports. Count at most three reserved slots; mark failed generation so it no longer consumes a slot.

**Step 4: Run tests and commit**

Run: `cd apps/server && ./mvnw -Dtest=ReportIntegrationTest test`

```bash
git add apps/server/src/main/resources/schema.sql apps/server/src/main/java/com/bazi/app/domain/BaziReport.java apps/server/src/main/java/com/bazi/app/mapper/BaziReportMapper.java apps/server/src/main/java/com/bazi/app/service/ReportService.java apps/server/src/main/java/com/bazi/app/controller/ReportController.java apps/server/src/main/java/com/bazi/app/config/WebConfig.java apps/server/src/test/java/com/bazi/app/ReportIntegrationTest.java
git commit -m "feat(report): add owned report APIs"
```

---

### Task 5: Separate order payment from product fulfillment

**Files:**
- Create: `apps/server/src/main/java/com/bazi/app/domain/constants/ReportProducts.java`
- Create: `apps/server/src/main/java/com/bazi/app/service/OrderFulfillmentService.java`
- Modify: `apps/server/src/main/java/com/bazi/app/service/PayService.java`
- Modify: `apps/server/src/main/java/com/bazi/app/service/MembershipService.java`
- Modify: `apps/server/src/main/java/com/bazi/app/controller/PayController.java`
- Modify: `apps/server/src/main/java/com/bazi/app/dto/CreateOrderRequest.java`
- Modify: `apps/server/src/main/java/com/bazi/app/dto/OrderDto.java`
- Test: `apps/server/src/test/java/com/bazi/app/PayIntegrationTest.java`
- Test: `apps/server/src/test/java/com/bazi/app/ReportPaymentIntegrationTest.java`

**Step 1: Write failing fulfillment tests**

- Paying `member_1m` extends membership exactly once.
- Paying `report_plain_single` (690 cents) or `report_professional_single` (1290 cents) marks only the linked edition/topic report as entitled/ready and does not extend membership.
- A repeated callback is idempotent for both products.
- An active member with fewer than three successful reports today generates without an order.
- An active member at 3/3 receives single-purchase terms and can continue after paying.

**Step 2: Refactor product dispatch**

Keep the existing `bazi_order.plan` column for compatibility, but treat it as a product code in service code. Add `subjectId` to `CreateOrderRequest` so a report order links to `reportId`; membership orders require it to be null.

`PayService` owns the atomic `pending -> paid` state transition. `OrderFulfillmentService` dispatches by product code after that transition. `MembershipService` only grants membership and no longer marks arbitrary orders paid.

**Step 3: Make mock payment return `OrderDto`**

The membership page must refetch `/api/v1/me` after payment rather than depending on a membership-shaped payment response. This makes the same payment endpoint usable for reports.

**Step 4: Run payment tests and commit**

Run: `cd apps/server && ./mvnw -Dtest=PayIntegrationTest,ReportPaymentIntegrationTest test`

```bash
git add apps/server/src/main/java/com/bazi/app/domain/constants/ReportProducts.java apps/server/src/main/java/com/bazi/app/service apps/server/src/main/java/com/bazi/app/controller/PayController.java apps/server/src/main/java/com/bazi/app/dto apps/server/src/test/java/com/bazi/app
git commit -m "refactor(pay): fulfill membership and report products"
```

---

### Task 6: Add topic selection and server download to the report page

**Files:**
- Create: `apps/web/src/services/reportApi.ts`
- Create: `apps/web/src/components/ReportOptionsPicker.tsx`
- Test: `apps/web/src/components/ReportOptionsPicker.test.tsx`
- Modify: `apps/web/src/pages/ReportPage.tsx`
- Modify: `apps/web/src/pages/ReportPage.test.tsx`
- Modify: `apps/web/src/services/payApi.ts`
- Modify: `apps/web/src/pages/MembershipPage.tsx`
- Modify: `apps/web/src/types/bazi.ts`
- Modify: `apps/web/src/styles/app.css`

**Step 1: Write failing UI tests**

- “生成命书” first opens the report options picker.
- The user must choose `通俗版` or `专业版` before confirming a topic.
- Eight supported topics are available, with “综合研读” selected by default.
- Confirming without login routes to `/auth`.
- In HTTP mode, successful mock payment triggers report generation and downloads the returned Blob.
- Server failure retains the selected topic and shows a retryable error.
- A member at 3/3 sees the required extra-payment copy and is never auto-charged.

**Step 2: Add the report API**

```ts
export type ReportTopic =
  | 'overall'
  | 'career'
  | 'wealth'
  | 'relationship'
  | 'family'
  | 'study'
  | 'collaboration'
  | 'life_stage'

export type ReportEdition = 'plain' | 'professional'

export async function createReport(recordId: number, topic: ReportTopic, edition: ReportEdition): Promise<ReportInfo>
export async function downloadReport(reportId: number): Promise<Blob>
```

Use `authFetch`; do not send `PaipanResult` or generated paragraphs.

**Step 3: Build the picker interaction**

First choose `通俗版 ¥6.9` or `专业版 ¥12.9`, then choose one topic. Show each edition's audience, expected detail and page range. “综合研读” is the default topic, but both selected edition and topic must be visible on the confirmation action.

**Step 4: Replace the button workflow**

`生成命书` → choose edition → choose topic → request access decision. Existing report downloads directly; member quota generates free; non-member or member at 3/3 sees price confirmation, then creates an order → mock pay in development → fulfill report → download native PDF.

Required exhausted-quota copy:

```text
今日会员免费额度已用完（3/3）。继续生成本份「{版本}命书」需单独支付 {价格}，生成后可永久下载。
```

Actions: `单次付费生成 {价格}` and `明日再生成`. Never auto-charge after receiving the quota result.

Keep the five-chapter web preview available before purchase.

**Step 5: Run tests and commit**

Run: `cd apps/web && npm test -- --run src/components/ReportOptionsPicker.test.tsx src/pages/ReportPage.test.tsx`

```bash
git add apps/web/src/services/reportApi.ts apps/web/src/components/ReportOptionsPicker.tsx apps/web/src/components/ReportOptionsPicker.test.tsx apps/web/src/pages/ReportPage.tsx apps/web/src/pages/ReportPage.test.tsx apps/web/src/services/payApi.ts apps/web/src/pages/MembershipPage.tsx apps/web/src/types/bazi.ts apps/web/src/styles/app.css
git commit -m "feat(report): select topic and download paid report"
```

---

### Task 7: Remove screenshot PDF generation from the paid path

**Files:**
- Delete after migration: `apps/web/src/lib/reportPdf.ts`
- Delete after migration: `apps/web/src/lib/reportPdf.test.ts`
- Modify: `apps/web/package.json`
- Modify: `apps/web/package-lock.json`

**Step 1: Confirm no imports remain**

Run: `rg "reportPdf|html2canvas|jsPDF" apps/web/src`

Expected: only the old implementation and test are found.

**Step 2: Remove old files and dependencies**

Remove `jspdf` and `html2canvas` only after HTTP report download works. Do not remove free report preview components.

**Step 3: Run the frontend suite and commit**

Run: `cd apps/web && npm test -- --run && npm run lint && npm run build`

Expected: all tests pass, lint has zero errors, production build succeeds.

```bash
git add apps/web/package.json apps/web/package-lock.json apps/web/src/lib/reportPdf.ts apps/web/src/lib/reportPdf.test.ts
git commit -m "refactor(report): remove raster PDF exporter"
```

---

### Task 8: Complete end-to-end and content-quality acceptance

**Files:**
- Modify: `e2e/run_e2e.py`
- Create: `apps/server/src/test/java/com/bazi/app/report/ReportComplianceTest.java`
- Modify: `docs/design/commercial-design.md`
- Modify: `docs/design/bazi-frontend-design.md`
- Modify: `docs/design/backend-design.md`

**Step 1: Add E2E flow**

Register → create chart → open report → choose topic → create report order → mock pay → download PDF → verify MIME, filename and non-zero size.

**Step 2: Add PDF quality checks**

- Extracted text contains the subject name, five chapter titles and selected topic.
- No page is image-only.
- No forbidden certainty terms.
- Every topic point in persisted content has evidence.
- Plain and professional reports share rule conclusions but differ in density and wording.
- Plain report price is 690 cents; professional report price is 1290 cents.
- Membership generation 1–3 succeeds without an order; generation 4 requires an order.
- Existing reports remain downloadable after membership expiry.
- Report cannot be downloaded by a second user.

**Step 3: Correct commercial documentation**

Verify that the commercial document continues to state “免费预览已完成；文字型付费报告完成后方可收费”. Document the exact free/paid boundary and report content version; do not restore the obsolete “命书 PDF 功能已完整” claim.

**Step 4: Run full verification**

```bash
cd apps/server && ./mvnw test
cd apps/web && npm test -- --run && npm run lint && npm run build
python3 e2e/run_e2e.py
```

Expected: backend tests pass, frontend tests pass, lint/build pass, report E2E downloads a searchable PDF.

**Step 5: Commit**

```bash
git add e2e apps/server/src/test/java/com/bazi/app/report docs/design
git commit -m "test(report): verify paid report delivery"
```

---

## Recommended review checkpoints

1. After Task 2: review all topic copy and evidence mappings before PDF work.
2. After Task 3: inspect one generated PDF at 100% and 200% zoom and test text selection.
3. After Task 4: approve the report API and ownership rules before payment changes.
4. After Task 6: approve the topic-selection purchase flow before deleting the old exporter.

## Explicitly deferred

- Real WeChat/Alipay integration and callback signature verification.
- Object storage/CDN; add it when regeneration cost or report volume justifies it.
- LLM-generated prose. The MVP remains deterministic and reviewable.
- A definitive Yongshen result until its method, evidence and expert review are documented.
