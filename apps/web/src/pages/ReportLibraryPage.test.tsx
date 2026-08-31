import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listReports, type SavedReport } from '@/services/reportApi'
import { ReportLibraryPage } from './ReportLibraryPage'

vi.mock('@/services/reportApi', async (importOriginal) => ({
  ...await importOriginal<typeof import('@/services/reportApi')>(),
  listReports: vi.fn(),
}))

describe('ReportLibraryPage', () => {
  beforeEach(() => vi.mocked(listReports).mockResolvedValue([
    {
      id: 18,
      subject: '林先生',
      topic: 'career',
      edition: 'plain',
      status: 'ready',
      contentVersion: 'career-narrative-v1',
      content: { thesis: '今年先做实成果，明年再争取授权。', contextSummary: '在职', years: [], route: [] },
      createdAt: '2026-08-23T16:00:00',
      generatedAt: '2026-08-23T16:00:00',
    },
    {
      id: 19,
      subject: '林先生',
      topic: 'wealth',
      edition: 'professional',
      status: 'ready',
      contentVersion: 'wealth-narrative-v1',
      content: { thesis: '先稳住工资，再尝试增加收入。', contextSummary: '工资和副业都有', years: [], route: [] },
      createdAt: '2026-08-23T17:00:00',
      generatedAt: '2026-08-23T17:00:00',
    },
    {
      id: 20,
      subject: '林先生',
      topic: 'wealth',
      edition: 'plain',
      status: 'ready',
      contentVersion: 'wealth-narrative-v2',
      content: {
        horizonYears: 3,
        thesis: '先找到真正带来钱的事情，再把可靠收入稳定下来。',
        summary: '主要看项目收入。',
        paths: [],
        primaryPathCode: 'project_income',
        secondaryPathCode: 'skill_income',
        mainRisk: { pathCode: 'retention', label: '把钱留下', judgment: '管住支出。', evidenceKeys: [] },
        years: [],
        route: [],
      },
      createdAt: '2026-08-24T08:00:00',
      generatedAt: '2026-08-24T08:00:00',
    },
    {
      id: 38,
      subject: '林先生',
      topic: 'relationship',
      edition: 'plain',
      status: 'ready',
      contentVersion: 'relationship-narrative-v1',
      content: {
        relationshipStatus: 'dating',
        relationshipStatusLabel: '已确认交往关系',
        horizonYears: 3,
        thesis: '先看两个人能不能稳定回应，再决定是否继续走下去。',
        summary: '主要看关系连接。',
        dimensions: [],
        primaryDimensionCode: 'connection',
        secondaryDimensionCode: 'response',
        focusTied: false,
        mainRisk: null,
        years: [],
        evidenceKeys: [],
      },
      createdAt: '2026-08-27T08:00:00',
      generatedAt: '2026-08-27T08:00:00',
    },
    {
      id: 48,
      subject: '林先生',
      topic: 'wealth',
      edition: 'plain',
      status: 'ready',
      contentVersion: 'wealth-narrative-v3',
      content: {
        thesis: { text: '三年里每年的收入重点不同，先逐年看清楚。' },
      },
      createdAt: '2026-08-29T08:00:00',
      generatedAt: '2026-08-29T08:00:00',
    } as unknown as SavedReport,
  ] satisfies SavedReport[]))

  it('把已经生成的命书作为可重复打开的报告展示', async () => {
    render(<MemoryRouter><ReportLibraryPage /></MemoryRouter>)

    expect(await screen.findByRole('link', { name: /林先生.*事业运势.*通俗版/ })).toHaveAttribute('href', '/reports/18')
    expect(screen.getByRole('link', { name: /林先生.*财富运势.*专业版/ })).toHaveAttribute('href', '/reports/19')
    expect(screen.getByText('今年先做实成果，明年再争取授权。')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /林先生.*财富运势.*通俗版.*先找到真正带来钱/ })).toHaveAttribute(
      'href',
      '/reports/20',
    )
    expect(screen.getByRole('link', { name: /林先生.*感情运势.*通俗版.*先看两个人/ })).toHaveAttribute(
      'href',
      '/reports/38',
    )
    expect(screen.getByRole('link', { name: /林先生.*财富运势.*通俗版.*三年里每年的收入重点不同/ })).toHaveAttribute(
      'href',
      '/reports/48',
    )
  })
})
