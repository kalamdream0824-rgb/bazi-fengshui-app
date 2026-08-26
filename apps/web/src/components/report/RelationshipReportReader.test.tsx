import { render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import type { RelationshipV1SavedReport } from '@/services/reportApi'
import { RelationshipReportReader } from './RelationshipReportReader'

export const relationshipReport: RelationshipV1SavedReport = {
  id: 38,
  subject: '林先生',
  topic: 'relationship',
  edition: 'plain',
  status: 'ready',
  contentVersion: 'relationship-narrative-v1',
  createdAt: '2026-08-27T08:00:00',
  generatedAt: '2026-08-27T08:00:00',
  content: {
    relationshipStatus: 'dating',
    relationshipStatusLabel: '已确认交往关系',
    horizonYears: 3,
    thesis: '先看两个人能不能稳定回应，再决定是否继续走下去。',
    summary: '主要看关系连接，其次看回应与表达，并留意矛盾与边界。',
    primaryDimensionCode: 'connection',
    secondaryDimensionCode: 'response',
    focusTied: false,
    mainRisk: {
      dimensionCode: 'boundaries',
      label: '矛盾与边界',
      judgment: '分歧出现时，两个人要先把问题说具体。',
      evidenceKeys: ['annual.branch.clash.day.boundaries'],
    },
    dimensions: [
      ['connection', '关系连接', '主要内容', '两个人愿意持续靠近。'],
      ['response', '回应与表达', '还要看看', '彼此能否把真实想法说清楚。'],
      ['daily_cooperation', '日常配合', '不是主要内容', '见面和时间安排能否互相照顾。'],
      ['boundaries', '矛盾与边界', '需要留意', '分歧出现时不要只靠一方退让。'],
      ['stability', '长期稳定', '不是主要内容', '重要决定要看两个人能否落实。'],
    ].map(([code, label, status, judgment]) => ({
      code,
      label,
      status,
      tone: 'mixed',
      judgment,
      supportingEvidenceKeys: [`${code}.support`],
      limitingEvidenceKeys: code === 'boundaries' ? ['annual.branch.clash.day.boundaries'] : [],
    })),
    years: [
      {
        year: 2026,
        ganZhi: '丙午',
        focus: '第一年先看两个人能否稳定回应。',
        judgment: '今年两个人更容易注意彼此是否认真投入。',
        mainLimit: '争执时不要只说情绪，要说清具体事情。',
        realitySignals: ['你主动联系后，对方愿意认真回答。', '两个人愿意提前安排下一次见面。'],
        actions: ['你先说清自己希望怎样继续。', '两个人约定一个能做到的见面安排。'],
        transition: '下一年继续看两个人能否把安排变成习惯。',
        primaryDimensionCode: 'connection',
        secondaryDimensionCode: 'response',
        riskDimensionCode: 'boundaries',
        evidenceKeys: ['annual.branch.clash.day.boundaries'],
      },
      {
        year: 2027,
        ganZhi: '丁未',
        focus: '第二年看两个人能否安排好相处节奏。',
        judgment: '今年更适合把联系和见面安排说清楚。',
        mainLimit: null,
        realitySignals: ['你提出安排后，对方愿意一起商量。', '两个人的见面不再总靠临时决定。'],
        actions: ['你把不能接受的安排直接说清。', '两个人保留各自需要的时间。'],
        transition: '第三年再看这些安排能否长期保持。',
        primaryDimensionCode: 'daily_cooperation',
        secondaryDimensionCode: 'stability',
        riskDimensionCode: null,
        evidenceKeys: ['annual.branch.harmony.day.daily_cooperation'],
      },
      {
        year: 2028,
        ganZhi: '戊申',
        focus: '第三年确认两个人是否适合继续走下去。',
        judgment: '今年要看重要决定能否由两个人一起完成。',
        mainLimit: null,
        realitySignals: ['两个人愿意谈未来一年的具体安排。', '你和对方都愿意为约定留出时间。'],
        actions: ['两个人先确定一件共同要完成的事。', '你根据实际行动决定是否继续投入。'],
        transition: '已经能够稳定做到的安排，可以继续保留。',
        primaryDimensionCode: 'stability',
        secondaryDimensionCode: 'connection',
        riskDimensionCode: null,
        evidenceKeys: ['annual.stem.group.wealth.stability'],
      },
    ],
    evidenceKeys: [
      'annual.branch.clash.day.boundaries',
      'annual.branch.harmony.day.daily_cooperation',
      'annual.stem.group.wealth.stability',
    ],
  },
}

describe('RelationshipReportReader', () => {
  it('展示状态、三年总断、五个维度和三年行动', () => {
    render(<MemoryRouter><RelationshipReportReader report={relationshipReport} /></MemoryRouter>)

    expect(screen.getByText('感情命书 · 通俗版')).toBeInTheDocument()
    expect(screen.getByText(/林先生.*已确认交往关系/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: relationshipReport.content.thesis })).toBeInTheDocument()
    expect(screen.getByText('主要关注')).toBeInTheDocument()
    expect(screen.getByText('次要关注')).toBeInTheDocument()
    expect(within(screen.getByLabelText('三年关系重点')).getByText('需要留意')).toBeInTheDocument()
    for (const label of ['关系连接', '回应与表达', '日常配合', '矛盾与边界', '长期稳定']) {
      expect(screen.getAllByText(label).length).toBeGreaterThan(0)
    }
    expect(screen.getByRole('heading', { name: '2028 · 戊申' })).toBeInTheDocument()
    expect(screen.getByText('两个人先确定一件共同要完成的事。')).toBeInTheDocument()
  })

  it('计算依据默认折叠，无风险时不显示问题卡', () => {
    const noRisk = {
      ...relationshipReport,
      content: {
        ...relationshipReport.content,
        mainRisk: null,
        dimensions: relationshipReport.content.dimensions.map((dimension) => (
          dimension.code === 'boundaries'
            ? { ...dimension, status: '不是主要内容', limitingEvidenceKeys: [] }
            : dimension
        )),
      },
    }
    render(<MemoryRouter><RelationshipReportReader report={noRisk} /></MemoryRouter>)

    expect(within(screen.getByLabelText('三年关系重点')).queryByText('需要留意')).not.toBeInTheDocument()
    expect(screen.getByText('查看计算依据')).toBeInTheDocument()
    expect(screen.getByText('annual.branch.clash.day.boundaries')).not.toBeVisible()
  })
})
