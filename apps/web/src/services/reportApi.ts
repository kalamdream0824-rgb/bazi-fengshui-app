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

export interface SavedReport {
  id: number
  subject: string
  topic: ReportTopicCode
  edition: ReportEditionCode
  status: string
  contentVersion: string
  content: CareerNarrativePlan
  createdAt: string
  generatedAt: string
}

export async function createReport(
  request: PaipanRequest,
  topic: ReportTopicCode,
  edition: ReportEditionCode,
  careerContext?: CareerContextInput,
): Promise<SavedReport> {
  const payload = topic === 'career' && careerContext
    ? { request, topic, edition, careerContext }
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
