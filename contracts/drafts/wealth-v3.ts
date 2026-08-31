/** Wealth v3 contract reference. The running web application keeps matching runtime-facing types in reportApi.ts. */
export type WealthPathV3 =
  | 'stable_income' | 'skill_income' | 'project_income' | 'cooperation_income' | 'retention'
export type IncomePathV3 = Exclude<WealthPathV3, 'retention'>

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
  | { state: 'leading'; primaryCandidates: [IncomePathV3]; secondaryCandidates: IncomePathV3[] }
  | { state: 'tied'; primaryCandidates: [IncomePathV3, IncomePathV3, ...IncomePathV3[]]; secondaryCandidates: [] }

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
  reasonCodes: Array<'no_evidence' | 'limited_net_support' | 'single_origin'
    | 'has_limitations' | 'legacy_provenance_unresolved'>
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
  copyVersion: 'wealth-plain-v3' | 'wealth-plain-v3.1' | 'wealth-plain-v3.2' | 'wealth-plain-v3.3'
  headlinePlannerVersion?: 'wealth-headline-v1'
  thesis: WealthBlockV3
  summary: WealthBlockV3
  pathSummaries: Array<{ path: WealthPathV3; reading: WealthBlockV3; yearDecisionIds: string[] }>
  riskSummary: WealthBlockV3 | null
  years: WealthYearV3[]
  route: WealthBlockV3[]
  readingNote: WealthBlockV3
}

export interface WealthReportV3 {
  id: number
  subject: string
  topic: 'wealth'
  edition: 'plain'
  status: 'ready'
  contentVersion: 'wealth-narrative-v3'
  content: WealthContentV3
  createdAt: string
  generatedAt: string
}
