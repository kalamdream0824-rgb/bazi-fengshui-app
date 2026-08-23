import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { getReport, type LegacySavedReport, type WealthV2SavedReport } from '@/services/reportApi'
import { ReportReaderPage } from './ReportReaderPage'

vi.mock('@/services/reportApi', async (importOriginal) => ({
  ...await importOriginal<typeof import('@/services/reportApi')>(),
  getReport: vi.fn(),
}))

const report: LegacySavedReport = {
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

const wealthV2Report: WealthV2SavedReport = {
  id: 28,
  subject: '林先生',
  topic: 'wealth' as const,
  edition: 'plain' as const,
  status: 'ready',
  contentVersion: 'wealth-narrative-v2',
  createdAt: '2026-08-24T08:00:00',
  generatedAt: '2026-08-24T08:00:00',
  content: {
    horizonYears: 3,
    thesis: '先找到真正带来钱的事情，再把可靠收入稳定下来。',
    summary: '主要看项目和额外收入，同时保留靠能力赚钱，并留意把钱留下。',
    primaryPathCode: 'project_income',
    secondaryPathCode: 'skill_income',
    mainRisk: {
      pathCode: 'retention',
      label: '把钱留下',
      judgment: '收入增加不代表结余增加，要管住新增支出。',
      evidenceKeys: ['annual.branch.clash.day'],
    },
    paths: [
      ['stable_income', '稳定收入', '表现一般', '固定工资、长期客户或定期收入更值得保留。'],
      ['skill_income', '靠能力赚钱', '辅助方向', '别人愿意为你能解决的问题付钱。'],
      ['project_income', '项目和额外收入', '主要方向', '奖金、订单和临时项目可以带来额外收入。'],
      ['cooperation_income', '合作带来的收入', '表现一般', '合作可以增收，但收入分配和付款时间要先说清。'],
      ['retention', '把钱留下', '需要留意', '收入增加后，也要防止支出跟着增加。'],
    ].map(([code, label, status, judgment]) => ({
      code,
      label,
      status,
      judgment,
      supportingEvidenceKeys: [`${code}.support`],
      limitingEvidenceKeys: [],
    })),
    years: [
      {
        year: 2026,
        ganZhi: '丙午',
        focus: '第一年：确认什么事情真的能带来收入。',
        incomeSource: '这一年的钱主要来自奖金、订单或临时项目。',
        retention: '先记录收入、成本和支出，再判断能留下多少。',
        mainLimit: '项目看着不少，也要扣掉成本再看实际收入。',
        realitySignals: ['订单或奖金到账，不再只是口头约定。', '连续三个月有实际收款。'],
        actions: ['每笔收入扣除成本后，再看实际剩下的钱。', '记录每月结余。'],
        transition: '把能稳定收钱的事情留下，下一年再增加次数。',
        evidenceKeys: ['wealth.2026'],
      },
      {
        year: 2027,
        ganZhi: '丁未',
        focus: '第二年：把已经能收钱的事情做得更稳定。',
        incomeSource: '这一年的钱主要来自固定工资或长期客户。',
        retention: '今年收入增加时，支出不要按同样速度增加。',
        mainLimit: '固定收入不稳时，先保住必要的生活开支。',
        realitySignals: ['固定收入按时到账。', '同一类收入重复出现。'],
        actions: ['记录到账时间。', '减少低收益安排。'],
        transition: '今年先稳定重复收入，再为第三年留下余地。',
        evidenceKeys: ['wealth.2027'],
      },
      {
        year: 2028,
        ganZhi: '戊申',
        focus: '第三年：减少低收益安排，增加实际结余。',
        incomeSource: '这一年的钱主要来自别人愿意付费的能力。',
        retention: '固定支出要与稳定收入匹配。',
        mainLimit: '收入增加不代表结余增加，要管住新增支出。',
        realitySignals: ['每月结余比前两年稳定。', '收入来源更加集中。'],
        actions: ['保留能重复收费的事情。', '先留下一笔结余。'],
        transition: '前两年验证有效的收入方式，可以继续保留。',
        evidenceKeys: ['wealth.2028'],
      },
    ],
    route: ['确认真实收入来源。', '保留重复收费的事情。', '让每月结余稳定下来。'],
  },
}

function renderReader() {
  return render(
    <MemoryRouter initialEntries={['/reports/18']}>
      <Routes><Route path="/reports/:id" element={<ReportReaderPage />} /></Routes>
    </MemoryRouter>,
  )
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

  it('财富v2先展示三年总断，并完整列出五条钱路', async () => {
    vi.mocked(getReport).mockResolvedValue(wealthV2Report)
    renderReader()

    expect(await screen.findByText('三年总断')).toBeInTheDocument()
    expect(screen.getByText('先看你的钱路')).toBeInTheDocument()
    for (const label of ['稳定收入', '靠能力赚钱', '项目和额外收入', '合作带来的收入', '把钱留下']) {
      expect(screen.getAllByText(label).length).toBeGreaterThan(0)
    }
    expect(screen.getByText('主要钱路')).toBeInTheDocument()
    expect(screen.getByText('辅助钱路')).toBeInTheDocument()
    expect(screen.getByText('最需留意')).toBeInTheDocument()
  })

  it('财富v2按通用序号渲染三年内容和2028年结论', async () => {
    vi.mocked(getReport).mockResolvedValue(wealthV2Report)
    renderReader()

    expect(await screen.findByText('第一年')).toBeInTheDocument()
    expect(screen.getByText('第二年')).toBeInTheDocument()
    expect(screen.getByText('第三年')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '2028 · 戊申' })).toBeInTheDocument()
    expect(screen.getByText('前两年验证有效的收入方式，可以继续保留。')).toBeInTheDocument()
    expect(screen.queryByText('专业依据')).not.toBeInTheDocument()
    expect(screen.queryByText('wealth.2028')).not.toBeInTheDocument()
  })

  it('事业与财富v1继续使用旧版两年阅读结构', async () => {
    renderReader()
    expect(await screen.findByText('两年总断')).toBeInTheDocument()

    vi.mocked(getReport).mockResolvedValue({
      ...report,
      topic: 'wealth',
      contentVersion: 'wealth-narrative-v1',
    })
    renderReader()
    expect((await screen.findAllByText('两年总断')).length).toBeGreaterThan(0)
  })
})
