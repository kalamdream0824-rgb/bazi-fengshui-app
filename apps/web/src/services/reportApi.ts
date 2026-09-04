import type { PaipanRequest } from '@/types/bazi'
import { authFetch } from './http'
import { apiUrl } from './apiConfig'

export class ReportApiError extends Error {
  constructor(public readonly code: string, message: string) {
    super(message)
    this.name = 'ReportApiError'
  }
}

export type ReportTopicCode = 'overall' | 'career' | 'wealth' | 'relationship'
export type ReportEditionCode = 'plain' | 'professional'
export type CareerStatusCode = 'employed' | 'self_employed' | 'job_seeking' | 'studying'
export type CareerGoalCode = 'promotion' | 'job_change' | 'stability' | 'transition'
export type CareerPaceCode = 'smooth' | 'stalled' | 'high_pressure' | 'preparing_change'
export type WealthIncomeSourceCode = 'salary' | 'self_employed' | 'mixed' | 'unstable'
export type WealthGoalCode = 'increase_income' | 'stabilize_cashflow' | 'reduce_pressure' | 'new_income_source'
export type WealthPaceCode = 'stable' | 'income_fluctuating' | 'spending_pressure' | 'preparing_adjustment'
export type RelationshipStatus = 'single' | 'dating' | 'married'

export interface CareerContextInput {
  status: CareerStatusCode
  goal: CareerGoalCode
  pace: CareerPaceCode
}

export interface WealthContextInput {
  incomeSource: WealthIncomeSourceCode
  goal: WealthGoalCode
  pace: WealthPaceCode
}

export interface RelationshipContextInput {
  status: RelationshipStatus
}

export interface CreateReportOptions {
  careerContext?: CareerContextInput
  relationshipContext?: RelationshipContextInput
}

export interface ReportEvidence {
  key: string
  label: string
  value: string
}

export interface NarrativeTimelinePastReview {
  year: number
  headline: string
  checkpoints: string[]
  bridge: string
  evidenceKeys: string[]
}

export interface NarrativeTimelinePresentReading {
  year: number
  headline: string
  judgment: string
  priority: string
  evidenceKeys: string[]
}

export interface NarrativeTimelineFutureStep {
  year: number
  headline: string
  action: string
  evidenceKeys: string[]
}

export interface NarrativeTimeline {
  past: NarrativeTimelinePastReview
  present: NarrativeTimelinePresentReading
  future: NarrativeTimelineFutureStep[]
}

export interface CareerYearNarrative {
  year: number
  ganZhi: string
  stage: string
  headline: string
  verdict: string
  reasons: string[]
  obstacle: string
  actions: string[]
  changeCondition: string
  evidenceKeys: string[]
  evidence: ReportEvidence[]
  counterEvidence: ReportEvidence[]
  confidence: string
}

export interface CareerNarrativePlan {
  thesis: string
  contextSummary: string
  years: CareerYearNarrative[]
  route: string[]
  timeline?: NarrativeTimeline
}

export interface WealthPathSummary {
  code: string
  label: string
  status: string
  judgment: string
  supportingEvidenceKeys: string[]
  limitingEvidenceKeys: string[]
}

export interface WealthRiskSummary {
  pathCode: string
  label: string
  judgment: string
  evidenceKeys: string[]
}

export interface WealthYearNarrative {
  year: number
  ganZhi: string
  focus: string
  incomeSource: string
  retention: string
  mainLimit: string
  realitySignals: string[]
  actions: string[]
  transition: string
  evidenceKeys: string[]
}

export interface WealthNarrativePlan {
  horizonYears: number
  thesis: string
  summary: string
  paths: WealthPathSummary[]
  primaryPathCode: string
  secondaryPathCode: string
  mainRisk: WealthRiskSummary
  years: WealthYearNarrative[]
  route: string[]
}

export type WealthPathV3 =
  | 'stable_income'
  | 'skill_income'
  | 'project_income'
  | 'cooperation_income'
  | 'retention'
export type WealthIncomePathV3 = Exclude<WealthPathV3, 'retention'>

export interface WealthBlockV3 {
  id: string
  kind: 'interpretation' | 'observation' | 'general_advice' | 'method_note'
  text: string
  templateId: string
  years: number[]
  decisionIds: string[]
}

export type WealthFocusV3 =
  | { state: 'none'; primaryCandidates: []; secondaryCandidates: [] }
  | { state: 'leading'; primaryCandidates: [WealthIncomePathV3]; secondaryCandidates: WealthIncomePathV3[] }
  | { state: 'tied'; primaryCandidates: [WealthIncomePathV3, WealthIncomePathV3, ...WealthIncomePathV3[]]; secondaryCandidates: [] }

export interface WealthFactV3 {
  id: string
  kind: 'natal' | 'annual' | 'dayun'
  code: string
  value: string
}

export interface WealthEvidenceV3 {
  id: string
  path: WealthPathV3
  ruleKey: string
  factKey: string
  family: 'NATAL_STRUCTURE' | 'NATAL_COMBINATION' | 'DAYUN_CONTEXT' | 'ANNUAL_TRIGGER'
  rootFactIds: string[]
  weight: number
}

export interface WealthDecisionV3 {
  id: string
  year: number
  path: WealthPathV3
  supportWeight: number
  limitationWeight: number
  netWeight: number
  stance: 'quiet' | 'supportive' | 'restricted' | 'mixed'
  strength: 'none' | 'limited' | 'supported' | 'pronounced'
  supportingEvidenceIds: string[]
  limitingEvidenceIds: string[]
  reasonCodes: Array<
    | 'no_evidence'
    | 'limited_net_support'
    | 'single_origin'
    | 'has_limitations'
    | 'legacy_provenance_unresolved'
  >
}

export interface WealthRiskV3 {
  path: WealthPathV3
  limitingEvidenceIds: string[]
  reading: WealthBlockV3
}

export interface WealthComparisonV3 {
  toYear: number
  direction: 'unchanged' | 'changed'
  changes: Array<{
    path: WealthPathV3
    supportDelta: number
    limitationDelta: number
    addedEvidenceIds: string[]
    removedEvidenceIds: string[]
  }>
  reading: WealthBlockV3
}

export interface WealthHeadlineMetaV3 {
  plannerVersion: 'wealth-headline-v1'
  themeKey: string
  pathKey: WealthPathV3
  subjectKey: string
  angleKey: string
  objectKey: string
  corePhraseKeys: string[]
  selectionReasonCodes: string[]
}

export interface WealthYearV3 {
  year: number
  ganZhi: string
  facts: WealthFactV3[]
  evidence: WealthEvidenceV3[]
  decisions: WealthDecisionV3[]
  focus: WealthFocusV3
  headlineMeta?: WealthHeadlineMetaV3
  overview: WealthBlockV3
  income: WealthBlockV3[]
  retention: WealthBlockV3
  risk: WealthRiskV3 | null
  observations: WealthBlockV3[]
  actions: WealthBlockV3[]
  comparison: WealthComparisonV3 | null
}

export interface WealthContentV3 {
  asOf: string
  zoneId: 'Asia/Shanghai'
  horizonYears: 3
  calculationVersion: 'wealth-path-v2'
  policyVersion: 'wealth-expression-v1'
  copyVersion: 'wealth-plain-v3' | 'wealth-plain-v3.1' | 'wealth-plain-v3.2' | 'wealth-plain-v3.3' | 'wealth-plain-v3.4'
  headlinePlannerVersion?: 'wealth-headline-v1'
  thesis: WealthBlockV3
  summary: WealthBlockV3
  pathSummaries: Array<{
    path: WealthPathV3
    reading: WealthBlockV3
    yearDecisionIds: string[]
  }>
  riskSummary: WealthBlockV3 | null
  years: WealthYearV3[]
  route: WealthBlockV3[]
  readingNote: WealthBlockV3
  timeline?: NarrativeTimeline
}

export interface RelationshipDimensionSummary {
  code: string
  label: string
  status: string
  tone: string
  judgment: string
  supportingEvidenceKeys: string[]
  limitingEvidenceKeys: string[]
}

export interface RelationshipRiskSummary {
  dimensionCode: string
  label: string
  judgment: string
  evidenceKeys: string[]
}

export interface RelationshipYearNarrative {
  year: number
  ganZhi: string
  focus: string
  judgment: string
  mainLimit: string | null
  realitySignals: string[]
  actions: string[]
  transition: string
  primaryDimensionCode: string
  secondaryDimensionCode: string
  riskDimensionCode: string | null
  evidenceKeys: string[]
}

export interface RelationshipNarrativePlan {
  relationshipStatus: RelationshipStatus
  relationshipStatusLabel: string
  horizonYears: number
  thesis: string
  summary: string
  dimensions: RelationshipDimensionSummary[]
  primaryDimensionCode: string
  secondaryDimensionCode: string
  focusTied: boolean
  mainRisk: RelationshipRiskSummary | null
  years: RelationshipYearNarrative[]
  evidenceKeys: string[]
  timeline?: NarrativeTimeline
}

export interface OverallNarrativePlan {
  horizonYears: 3
  thesis: string
  summary: string
  years: Array<{
    year: number
    ganZhi: string
    primaryCode: 'rhythm' | 'career' | 'wealth' | 'relationship'
    primaryLabel: string
    secondaryCode: 'rhythm' | 'career' | 'wealth' | 'relationship'
    secondaryLabel: string
    headline: string
    verdict: string
    linkage?: string
    dimensions: Array<{
      code: 'rhythm' | 'career' | 'wealth' | 'relationship'
      label: string
      stance: string
      judgment: string
      evidenceKeys: string[]
    }>
    priorityIssue: string
    actions: string[]
    changeCondition: string
    transition: string
    evidenceKeys: string[]
  }>
  route: string[]
  readingNote: string
  evidenceKeys: string[]
  timeline?: NarrativeTimeline
}

interface SavedReportBase {
  id: number
  subject: string
  topic: ReportTopicCode
  edition: ReportEditionCode
  status: string
  createdAt: string
  generatedAt: string
}

export interface LegacySavedReport extends SavedReportBase {
  contentVersion:
    | 'career-narrative-v1'
    | 'career-narrative-v2'
    | 'career-narrative-v3'
    | 'wealth-narrative-v1'
  content: CareerNarrativePlan
}

export interface CareerV4SavedReport extends SavedReportBase {
  topic: 'career'
  contentVersion: 'career-narrative-v4'
  content: CareerNarrativePlan
}

export interface WealthV2SavedReport extends SavedReportBase {
  topic: 'wealth'
  edition: 'plain'
  contentVersion: 'wealth-narrative-v2'
  content: WealthNarrativePlan
}

export interface OverallSavedReport extends SavedReportBase {
  topic: 'overall'
  edition: 'plain'
  contentVersion: 'overall-narrative-v1' | 'overall-narrative-v1.1' | 'overall-narrative-v2'
  content: OverallNarrativePlan
}

export interface WealthV3SavedReport extends SavedReportBase {
  topic: 'wealth'
  edition: 'plain'
  status: 'ready'
  contentVersion: 'wealth-narrative-v3' | 'wealth-narrative-v4'
  content: WealthContentV3
}

export interface RelationshipV1SavedReport extends SavedReportBase {
  topic: 'relationship'
  edition: 'plain'
  contentVersion: 'relationship-narrative-v1' | 'relationship-narrative-v2'
  content: RelationshipNarrativePlan
}

export interface RelationshipSingleNarrativePlan {
  relationshipStatus: 'single'
  horizonYears: 2
  thesis: string
  summary: string
  currentYear: number
  outlookYear: number
  sections: Array<{
    id: string
    title: string
    paragraphs: string[]
    signals: string[]
    evidenceKeys: string[]
  }>
  outlook: string[]
  readingNote: string
  evidenceKeys: string[]
  timeline?: NarrativeTimeline
}

export interface RelationshipSingleSavedReport extends SavedReportBase {
  topic: 'relationship'
  edition: 'plain'
  contentVersion: 'relationship-single-v1' | 'relationship-single-v2'
  content: RelationshipSingleNarrativePlan
}

export type SavedReport =
  | OverallSavedReport
  | LegacySavedReport
  | CareerV4SavedReport
  | WealthV2SavedReport
  | WealthV3SavedReport
  | RelationshipV1SavedReport
  | RelationshipSingleSavedReport

export function isRelationshipSingleReport(report: SavedReport): report is RelationshipSingleSavedReport {
  return report.contentVersion === 'relationship-single-v1'
    || report.contentVersion === 'relationship-single-v2'
}

export function isOverallReport(report: SavedReport): report is OverallSavedReport {
  return report.contentVersion === 'overall-narrative-v1'
    || report.contentVersion === 'overall-narrative-v1.1'
    || report.contentVersion === 'overall-narrative-v2'
}

export function isWealthV2Report(report: SavedReport): report is WealthV2SavedReport {
  return report.contentVersion === 'wealth-narrative-v2'
}

export function isWealthDetailedReport(report: SavedReport): report is WealthV3SavedReport {
  return report.contentVersion === 'wealth-narrative-v3'
    || report.contentVersion === 'wealth-narrative-v4'
}

export function isRelationshipReport(report: SavedReport): report is RelationshipV1SavedReport {
  return report.contentVersion === 'relationship-narrative-v1'
    || report.contentVersion === 'relationship-narrative-v2'
}

export function isCareerV4Report(report: SavedReport): report is CareerV4SavedReport {
  return report.contentVersion === 'career-narrative-v4'
}

export function isLegacyReport(report: SavedReport): report is LegacySavedReport {
  return [
    'career-narrative-v1',
    'career-narrative-v2',
    'career-narrative-v3',
    'wealth-narrative-v1',
  ].includes(report.contentVersion)
}

export function reportHeadline(report: SavedReport): string {
  const thesis: unknown = report.content.thesis
  if (typeof thesis === 'string') return thesis
  if (thesis && typeof thesis === 'object' && 'text' in thesis && typeof thesis.text === 'string') {
    return thesis.text
  }
  return '这份命书需要新版页面读取'
}

export async function createReport(
  request: PaipanRequest,
  topic: ReportTopicCode,
  edition: ReportEditionCode,
  options: CreateReportOptions = {},
): Promise<SavedReport> {
  const payload = topic === 'career' && options.careerContext
    ? { request, topic, edition, careerContext: options.careerContext }
    : topic === 'relationship' && options.relationshipContext
      ? { request, topic, edition, relationshipContext: options.relationshipContext }
      : { request, topic, edition }
  return reportJson(apiUrl('/v1/reports'), {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function listReports(): Promise<SavedReport[]> {
  return reportJson(apiUrl('/v1/reports'))
}

export async function getReport(id: number): Promise<SavedReport> {
  return reportJson(apiUrl(`/v1/reports/${id}`))
}

export interface ReportPreviewFile {
  blob: Blob
  fileName: string
}

export async function fetchReportPreview(
  request: PaipanRequest,
  topic: ReportTopicCode,
  edition: ReportEditionCode,
  careerContext?: CareerContextInput,
): Promise<ReportPreviewFile> {
  const payload = careerContext
    ? { request, topic, edition, careerContext }
    : { request, topic, edition }
  const response = await authFetch(apiUrl('/v1/reports/preview'), {
    method: 'POST',
    body: JSON.stringify(payload),
  })
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { message?: string } | null
    throw new Error(body?.message || `命书生成失败（HTTP ${response.status}）`)
  }
  return {
    blob: await response.blob(),
    fileName: responseFileName(response.headers.get('Content-Disposition')) ?? fallbackName(topic, edition),
  }
}

export async function downloadReportPreview(
  request: PaipanRequest,
  topic: ReportTopicCode,
  edition: ReportEditionCode,
  careerContext?: CareerContextInput,
): Promise<void> {
  const file = await fetchReportPreview(request, topic, edition, careerContext)
  const url = URL.createObjectURL(file.blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = file.fileName
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 0)
}

function responseFileName(disposition: string | null): string | null {
  if (!disposition) return null
  const encoded = disposition.match(/filename\*=UTF-8''([^;]+)/i)?.[1]
  if (encoded) {
    try {
      return decodeURIComponent(encoded)
    } catch {
      return null
    }
  }
  return disposition.match(/filename="?([^";]+)"?/i)?.[1] ?? null
}

function fallbackName(topic: ReportTopicCode, edition: ReportEditionCode): string {
  const topicLabel = {
    overall: '综合运势',
    career: '事业运势',
    wealth: '财富运势',
    relationship: '感情运势',
  }[topic]
  return `命书-${topicLabel}-${edition === 'plain' ? '通俗版' : '专业版'}.pdf`
}

async function reportJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await authFetch(path, init)
  if (!response.ok) {
    const body = (await response.json().catch(() => null)) as { code?: string; message?: string } | null
    throw new ReportApiError(
      body?.code || `HTTP_${response.status}`,
      body?.message || `命书读取失败（HTTP ${response.status}）`,
    )
  }
  return response.json() as Promise<T>
}
