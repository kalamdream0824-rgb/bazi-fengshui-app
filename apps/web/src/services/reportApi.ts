import type { PaipanRequest } from '@/types/bazi'
import { authFetch } from './http'

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

export interface WealthV2SavedReport extends SavedReportBase {
  topic: 'wealth'
  edition: 'plain'
  contentVersion: 'wealth-narrative-v2'
  content: WealthNarrativePlan
}

export interface RelationshipV1SavedReport extends SavedReportBase {
  topic: 'relationship'
  edition: 'plain'
  contentVersion: 'relationship-narrative-v1'
  content: RelationshipNarrativePlan
}

export type SavedReport = LegacySavedReport | WealthV2SavedReport | RelationshipV1SavedReport

export function isWealthV2Report(report: SavedReport): report is WealthV2SavedReport {
  return report.contentVersion === 'wealth-narrative-v2'
}

export function isRelationshipV1Report(report: SavedReport): report is RelationshipV1SavedReport {
  return report.contentVersion === 'relationship-narrative-v1'
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
  return reportJson('/api/v1/reports', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function listReports(): Promise<SavedReport[]> {
  return reportJson('/api/v1/reports')
}

export async function getReport(id: number): Promise<SavedReport> {
  return reportJson(`/api/v1/reports/${id}`)
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
  const response = await authFetch('/api/v1/reports/preview', {
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
    const body = (await response.json().catch(() => null)) as { message?: string } | null
    throw new Error(body?.message || `命书读取失败（HTTP ${response.status}）`)
  }
  return response.json() as Promise<T>
}
