import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getReport } from '@/services/reportApi'
import { ReportReaderPage } from './ReportReaderPage'

vi.mock('@/services/reportApi', () => ({ getReport: vi.fn() }))

const report = {
  id: 18,
  subject: '林先生',
  topic: 'career' as const,
  edition: 'professional' as const,
  status: 'ready',
  contentVersion: 'career-narrative-v1',
  createdAt: '2026-08-23T16:00:00',
  generatedAt: '2026-08-23T16:00:00',
  content: {
    thesis: '今年先把成果和责任边界做实，明年再争取正式授权。',
    contextSummary: '在职 · 晋升增责 · 推进顺利',
    route: ['整理成果', '争取授权'],
    years: [{
      year: 2026,
      ganZhi: '丙午',
      stage: '转折',
      headline: '工作角色调整',
      verdict: '岗位边界正在变化，先确认权责。',
      reasons: ['职责与协作关系发生变化。', '当前推进顺利。'],
      obstacle: '进展尚未沉淀为正式回报',
      actions: ['整理可量化成果', '确认职责边界'],
      changeCondition: '两个月没有转成回报时，重新谈边界。',
      evidenceKeys: ['career.role_transition'],
      evidence: [{ key: 'career.role_transition', label: '角色调整', value: '流年作用于事业关系' }],
      counterEvidence: [],
      confidence: '高',
    }],
  },
}

describe('ReportReaderPage', () => {
  beforeEach(() => vi.mocked(getReport).mockResolvedValue(report))

  it('首屏先给两年总判断，逐年只展示重点、理由和动作', async () => {
    render(
      <MemoryRouter initialEntries={['/reports/18']}>
        <Routes><Route path="/reports/:id" element={<ReportReaderPage />} /></Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: /今年先把成果和责任边界做实/ })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '2026 · 丙午' })).toBeInTheDocument()
    expect(screen.getByText('岗位边界正在变化，先确认权责。')).toBeInTheDocument()
    expect(screen.getByText('整理可量化成果')).toBeInTheDocument()
    expect(screen.getByText('进展尚未沉淀为正式回报')).toBeInTheDocument()
  })

  it('专业版保留可折叠的规则证据，不污染通俗主叙事', async () => {
    render(
      <MemoryRouter initialEntries={['/reports/18']}>
        <Routes><Route path="/reports/:id" element={<ReportReaderPage />} /></Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText('专业依据')).toBeInTheDocument()
    expect(screen.queryByText('流年作用于事业关系')).not.toBeVisible()
  })

  it('使用自然中文说明眼下的问题和何时调整', async () => {
    render(
      <MemoryRouter initialEntries={['/reports/18']}>
        <Routes><Route path="/reports/:id" element={<ReportReaderPage />} /></Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: '眼下最需要解决的问题' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '这一年，建议先做两件事' })).toBeInTheDocument()
    expect(screen.getByText('出现下面的情况，就要调整：', { exact: true })).toBeInTheDocument()
    expect(screen.getByText('接下来两年，可以先做这三件事', { exact: true })).toBeInTheDocument()
    expect(screen.queryByText('当前卡点')).not.toBeInTheDocument()
    expect(screen.queryByText('需要改路时：')).not.toBeInTheDocument()
  })

  it('财富报告显示财富标题与财印，而不是事业文案', async () => {
    vi.mocked(getReport).mockResolvedValue({
      ...report,
      topic: 'wealth',
      contentVersion: 'wealth-narrative-v1',
      content: {
        ...report.content,
        thesis: '先稳住工资，再用一个小副业尝试增加收入。',
        contextSummary: '工资和副业都有 · 希望增加收入 · 近期收入有波动',
      },
    })
    render(
      <MemoryRouter initialEntries={['/reports/18']}>
        <Routes><Route path="/reports/:id" element={<ReportReaderPage />} /></Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText(/财富命书 · 专业版/)).toBeInTheDocument()
    expect(screen.getByText('财')).toBeInTheDocument()
    expect(screen.queryByText(/事业命书/)).not.toBeInTheDocument()
  })
})
