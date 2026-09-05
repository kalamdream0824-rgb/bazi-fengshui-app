import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import {
  getReport,
  type LegacySavedReport,
  type OverallSavedReport,
  type RelationshipV1SavedReport,
  type SavedReport,
  type WealthV2SavedReport,
  type WealthV3SavedReport,
} from '@/services/reportApi'
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

const overallReport: OverallSavedReport = {
  id: 58,
  subject: '林先生',
  topic: 'overall',
  edition: 'plain',
  status: 'ready',
  contentVersion: 'overall-narrative-v1',
  createdAt: '2026-08-31T08:00:00',
  generatedAt: '2026-08-31T08:00:00',
  content: {
    horizonYears: 3,
    thesis: '这三年先处理生活节奏，随后重点会转到钱财安排；其他方面也要同时照看，但不必平均用力。',
    summary: '2027年相对更适合推进已有计划；2026年更需要控制负担。',
    readingNote: '这份综合命书用于比较三年的生活重点和处理顺序。',
    evidenceKeys: ['annual.stem.group.output'],
    route: ['只设一个年度重点。', '确认工作成果。', '保留日常用钱。'],
    years: [2026, 2027, 2028].map((year, index) => ({
      year,
      ganZhi: ['丙午', '丁未', '戊申'][index],
      primaryCode: (['rhythm', 'career', 'wealth'] as const)[index],
      primaryLabel: ['生活节奏', '事业责任', '钱财安排'][index],
      secondaryCode: 'relationship',
      secondaryLabel: '关系支持',
      headline: [
        '压力偏重，先把精力放在一件要紧事上',
        '条件较顺，把成果做实，别让忙碌代替进展',
        '机会与牵制并存，收入与花费一起看',
      ][index],
      verdict: '这一年先处理最影响日常的一件事，同时照看其他方面。',
      dimensions: ([
        ['rhythm', '生活节奏', '需收紧', '事情容易挤在一起，休息不足会影响判断。'],
        ['career', '事业责任', '较顺', '工作较容易得到任务或责任上的机会。'],
        ['wealth', '钱财安排', '平稳', '收入和支出大体平稳，先守住日常余量。'],
        ['relationship', '关系支持', '有机会也有牵制', '关系既有支持，也有需要说清的分歧。'],
      ] as const).map(([code, label, stance, judgment]) => ({
        code, label, stance, judgment, evidenceKeys: [`${year}.${code}.evidence`],
      })),
      priorityIssue: '最先要处理的是安排过满：每天都没有恢复时间，其他计划很难完成。',
      actions: ['删掉一个可以延后的安排。', '把下一步计划说具体。'],
      changeCondition: '如果连续两周办事效率变差，就要继续减少安排。',
      transition: index < 2 ? '下一年重点会转向其他方面。' : '这是本次三年判断的最后一年。',
      evidenceKeys: [`${year}.evidence`],
    })),
  },
}

const overallV11Report: OverallSavedReport = {
  ...overallReport,
  contentVersion: 'overall-narrative-v1.1',
  content: {
    ...overallReport.content,
    years: overallReport.content.years.map((year) => ({
      ...year,
      linkage: `先看清${year.primaryLabel}，因为它会直接影响${year.secondaryLabel}的实际安排。`,
    })),
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

const wealthBlock = (id: string, text: string, years = [2026, 2027, 2028]) => ({
  id,
  kind: 'interpretation' as const,
  text,
  templateId: id,
  years,
  decisionIds: years.map((year) => `${year}.decision.stable_income`),
})

const wealthV3Report = {
  id: 48,
  subject: '林先生',
  topic: 'wealth',
  edition: 'plain',
  status: 'ready',
  contentVersion: 'wealth-narrative-v3',
  createdAt: '2026-08-29T08:00:00',
  generatedAt: '2026-08-29T08:00:00',
  content: {
    asOf: '2026-08-29',
    zoneId: 'Asia/Shanghai',
    horizonYears: 3,
    calculationVersion: 'wealth-path-v2',
    policyVersion: 'wealth-expression-v1',
    copyVersion: 'wealth-plain-v3',
    thesis: wealthBlock('wealth.thesis', '这三年每年的收入重点不同，不必硬套成一条路。'),
    summary: wealthBlock('wealth.summary', '先看收入从哪里来，再看最后能留下多少。'),
    pathSummaries: [
      ['stable_income', '稳定收入', '三年里，固定工资和长期收入可以逐年核对。'],
      ['skill_income', '靠能力赚钱', '能解决什么问题，是这条收入要看的重点。'],
      ['project_income', '项目和额外收入', '额外项目要把成本和收款时间一起算清。'],
      ['cooperation_income', '合作带来的收入', '合作收入要先说清分账和付款。'],
      ['retention', '把钱留下', '进账不等于结余，要看最终能留下多少。'],
    ].map(([path, label, text]) => ({
      path,
      label,
      reading: wealthBlock(`wealth.path.${path}`, text),
      yearDecisionIds: [2026, 2027, 2028].map((year) => `${year}.decision.${path}`),
    })),
    riskSummary: null,
    years: [
      {
        year: 2026,
        ganZhi: '丙午',
        facts: [], evidence: [], decisions: [],
        focus: { state: 'tied', primaryCandidates: ['stable_income', 'skill_income'], secondaryCandidates: [] },
        overview: wealthBlock('2026.overview', '稳定收入和靠能力赚钱可以一起关注。', [2026]),
        income: [wealthBlock('2026.income', '一边看固定收入，一边看别人愿意为什么能力付钱。', [2026])],
        retention: wealthBlock('2026.retention', '收入到账后，先留出日常必需开支。', [2026]),
        risk: null,
        observations: [{ ...wealthBlock('2026.observation', '可以留意同一类收入能否重复出现。', [2026]), kind: 'observation' }],
        actions: [{ ...wealthBlock('2026.action', '记录每笔实际到账和对应成本。', [2026]), kind: 'general_advice' }],
        comparison: {
          toYear: 2027, direction: 'changed', changes: [],
          reading: wealthBlock('2026.comparison', '到2027年，收入方向会换一批条件来判断。', [2026, 2027]),
        },
      },
      {
        year: 2027,
        ganZhi: '丁未',
        facts: [], evidence: [], decisions: [],
        focus: { state: 'none', primaryCandidates: [], secondaryCandidates: [] },
        overview: wealthBlock('2027.overview', '这一年没有特别突出的收入方向。', [2027]),
        income: [wealthBlock('2027.income', '已有收入各有条件，不必强行选出第一名。', [2027])],
        retention: wealthBlock('2027.retention', '先确认每个月实际能留下多少。', [2027]),
        risk: null, observations: [],
        actions: [{ ...wealthBlock('2027.action', '把固定开支和临时开支分开记录。', [2027]), kind: 'general_advice' }],
        comparison: {
          toYear: 2028, direction: 'unchanged', changes: [],
          reading: wealthBlock('2027.comparison', '到2028年，现有条件没有明显变化。', [2027, 2028]),
        },
      },
      {
        year: 2028,
        ganZhi: '戊申',
        facts: [], evidence: [], decisions: [],
        focus: { state: 'leading', primaryCandidates: ['project_income'], secondaryCandidates: [] },
        overview: wealthBlock('2028.overview', '项目和额外收入相对更值得看看。', [2028]),
        income: [wealthBlock('2028.income', '额外项目要先看扣除成本后的实际收入。', [2028])],
        retention: wealthBlock('2028.retention', '新增进账不要立刻变成新增开支。', [2028]),
        risk: null, observations: [],
        actions: [{ ...wealthBlock('2028.action', '接项目前先写清价格和付款时间。', [2028]), kind: 'general_advice' }],
        comparison: null,
      },
    ],
    route: [{ ...wealthBlock('wealth.route', '先记清真实进账，再决定保留哪种收入。'), kind: 'general_advice' }],
    readingNote: { ...wealthBlock('wealth.note', '这是按年度整理的传统命理参考，不预测具体月份。'), kind: 'method_note' },
  },
} as unknown as WealthV3SavedReport

const relationshipReport: RelationshipV1SavedReport = {
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
    summary: '主要看关系连接，其次看回应与表达。',
    primaryDimensionCode: 'connection',
    secondaryDimensionCode: 'response',
    focusTied: false,
    mainRisk: null,
    dimensions: ['关系连接', '回应与表达', '日常配合', '矛盾与边界', '长期稳定'].map((label, index) => ({
      code: ['connection', 'response', 'daily_cooperation', 'boundaries', 'stability'][index],
      label,
      status: index < 2 ? '主要内容' : '不是主要内容',
      tone: 'supportive',
      judgment: `${label}有具体依据。`,
      supportingEvidenceKeys: [`relationship.${index}`],
      limitingEvidenceKeys: [],
    })),
    years: [{
      year: 2026,
      ganZhi: '丙午',
      focus: '第一年先看两个人能否稳定回应。',
      judgment: '今年两个人更容易把想法说清楚。',
      mainLimit: null,
      realitySignals: ['你愿意说出真实想法。', '对方愿意认真回答你。'],
      actions: ['你先说清自己的需要。', '两个人约定下一步安排。'],
      transition: '下一年继续看两个人能否落实安排。',
      primaryDimensionCode: 'connection',
      secondaryDimensionCode: 'response',
      riskDimensionCode: null,
      evidenceKeys: ['relationship.0'],
    }],
    evidenceKeys: ['relationship.0'],
  },
}

function timelineFor(futureYears: number[]) {
  return {
    past: {
      year: 2025,
      headline: '先回看去年的真实情况',
      checkpoints: ['回看去年是否出现过明显变化。', '如果事情有变化，是否留下了实际结果。'],
      bridge: '去年的实际经历，可以帮助你理解今年的重点。',
      evidenceKeys: ['timeline.2025'],
    },
    present: {
      year: 2026,
      headline: '今年先处理最影响眼下的一件事',
      judgment: '今年的重点已经比去年更清楚。',
      priority: '先把最重要的一件事做出实际结果。',
      evidenceKeys: ['timeline.2026'],
    },
    future: futureYears.map((year) => ({
      year,
      headline: `${year}年把已经验证的方向继续做稳`,
      action: `${year}年留下一个可以核对的实际结果。`,
      evidenceKeys: [`timeline.${year}`],
    })),
  }
}

const careerV4Report: SavedReport = {
  ...report,
  topic: 'career',
  contentVersion: 'career-narrative-v4',
  content: { ...report.content, timeline: timelineFor([2027]) },
}

const wealthV4Report: SavedReport = {
  ...wealthV3Report,
  contentVersion: 'wealth-narrative-v4',
  content: { ...wealthV3Report.content, timeline: timelineFor([2027, 2028]) },
}

const relationshipV2Report: SavedReport = {
  ...relationshipReport,
  contentVersion: 'relationship-narrative-v2',
  content: { ...relationshipReport.content, timeline: timelineFor([2027, 2028]) },
}

const relationshipSingleV2Report: SavedReport = {
  ...relationshipReport,
  contentVersion: 'relationship-single-v2',
  content: {
    relationshipStatus: 'single',
    horizonYears: 2,
    thesis: '今年可以主动认识人。',
    summary: '刚有好感，不必急着确定关系。',
    currentYear: 2026,
    outlookYear: 2027,
    sections: [{
      id: 'opportunities',
      title: '今年有没有认识人的机会？',
      paragraphs: ['如果目前没有正在了解的人，可以多给新的认识一些时间。'],
      signals: [],
      evidenceKeys: ['annual.connection'],
    }],
    outlook: ['明年的重点是说清彼此的想法。'],
    readingNote: '按年度解读，不预测具体月份。',
    evidenceKeys: ['annual.connection'],
    timeline: timelineFor([2027]),
  },
}

const overallV2Report: SavedReport = {
  ...overallV11Report,
  contentVersion: 'overall-narrative-v2',
  content: { ...overallV11Report.content, timeline: timelineFor([2027, 2028]) },
}

function renderReader() {
  return render(
    <MemoryRouter initialEntries={['/reports/18']}>
      <Routes><Route path="/reports/:id" element={<ReportReaderPage />} /></Routes>
    </MemoryRouter>,
  )
}

function expectTimelineBeforeDetails(detailsSelector: string, futureSteps: number) {
  const summary = document.querySelector('.report-reader__thesis')
  const timeline = screen.getByRole('region', { name: '命书时间线' })
  const details = document.querySelector(detailsSelector)

  expect(summary).not.toBeNull()
  expect(details).not.toBeNull()
  expect(summary!.compareDocumentPosition(timeline) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  expect(timeline.compareDocumentPosition(details!) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  expect(timeline.querySelectorAll('.narrative-timeline__future-list > li')).toHaveLength(futureSteps)
}

describe('ReportReaderPage', () => {
  it('事业v4在总览与两年正文之间展示一条未来行动', async () => {
    vi.mocked(getReport).mockResolvedValue(careerV4Report)
    renderReader()

    await screen.findByRole('region', { name: '命书时间线' })
    expectTimelineBeforeDetails('.report-reader__years', 1)
  })

  it('财富v4在总览与三年正文之间展示两条未来行动', async () => {
    vi.mocked(getReport).mockResolvedValue(wealthV4Report)
    renderReader()

    await screen.findByRole('region', { name: '命书时间线' })
    expectTimelineBeforeDetails('.report-reader__years', 2)
  })

  it('感情v2在总览与三年正文之间展示两条未来行动', async () => {
    vi.mocked(getReport).mockResolvedValue(relationshipV2Report)
    renderReader()

    await screen.findByRole('region', { name: '命书时间线' })
    expectTimelineBeforeDetails('.report-reader__years', 2)
  })

  it('单身感情v2在总览与当年正文之间展示一条未来行动', async () => {
    vi.mocked(getReport).mockResolvedValue(relationshipSingleV2Report)
    renderReader()

    await screen.findByRole('region', { name: '命书时间线' })
    expectTimelineBeforeDetails('.relationship-single-reader__current', 1)
  })

  it('综合v2在总览与三年正文之间展示两条未来行动', async () => {
    vi.mocked(getReport).mockResolvedValue(overallV2Report)
    renderReader()

    await screen.findByRole('region', { name: '命书时间线' })
    expectTimelineBeforeDetails('.report-reader__years', 2)
  })

  it('历史命书没有时间线时不显示空标题或占位区', async () => {
    vi.mocked(getReport).mockResolvedValue(report)
    renderReader()

    await screen.findByText('两年总断')
    expect(screen.queryByRole('region', { name: '命书时间线' })).not.toBeInTheDocument()
    expect(screen.queryByText('命书脉络')).not.toBeInTheDocument()
  })

  it('新版单身报告只展开今年问题，明年为摘要，使用保存时的年份', async () => {
    vi.mocked(getReport).mockResolvedValue({
      ...relationshipReport,
      contentVersion: 'relationship-single-v1',
      content: {
        relationshipStatus: 'single', horizonYears: 2,
        thesis: '今年可以主动认识人。', summary: '刚有好感，不必急着确定关系。',
        currentYear: 2026, outlookYear: 2027,
        sections: [{ id: 'opportunities', title: '今年有没有认识人的机会？',
          paragraphs: ['如果目前没有正在了解的人，可以多给新的认识一些时间。'],
          signals: [], evidenceKeys: ['annual.connection'] }],
        outlook: ['明年的重点是说清彼此的想法。'],
        readingNote: '按年度解读，不预测具体月份。',
        evidenceKeys: ['annual.connection'],
      },
    })
    renderReader()
    expect(await screen.findByRole('heading', { name: '今年有没有认识人的机会？' })).toBeInTheDocument()
    expect(screen.getByText('2026 年重点｜2027 年简短参考')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '2027 年 · 简短参考' })).toBeInTheDocument()
    expect(screen.getByText('明年的重点是说清彼此的想法。')).toBeInTheDocument()
    expect(screen.queryByText('三年总断')).not.toBeInTheDocument()
    expect(screen.queryByText('五个方面逐项看')).not.toBeInTheDocument()
    expect(screen.queryByText('第三年')).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: '查看我的全部命书' })).toHaveAttribute('href', '/reports')
  })

  it('旧单身三年快照仍完整显示第三年，不因为状态为单身而被裁短', async () => {
    vi.mocked(getReport).mockResolvedValue({
      ...relationshipReport,
      content: { ...relationshipReport.content, relationshipStatus: 'single',
        relationshipStatusLabel: '单身或尚未确定关系',
        years: [2026, 2027, 2028].map((year) => ({ ...relationshipReport.content.years[0], year })),
      },
    })
    renderReader()
    expect(await screen.findByText('三年总断')).toBeInTheDocument()
    expect(screen.getByText('第三年')).toBeInTheDocument()
  })
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

  it('综合v1先给三年主线，再按年份完整展示四个生活方面', async () => {
    vi.mocked(getReport).mockResolvedValue(overallReport)
    renderReader()

    expect(await screen.findByText('综合命书 · 通俗版')).toBeInTheDocument()
    expect(screen.getByText('三年总看')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: overallReport.content.thesis })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '2028 · 戊申' })).toBeInTheDocument()
    for (const label of ['生活节奏', '事业责任', '钱财安排', '关系支持']) {
      expect(screen.getAllByRole('heading', { name: label })).toHaveLength(3)
    }
    expect(screen.getAllByRole('heading', { name: '这一年先处理什么' })).toHaveLength(3)
    expect(screen.getAllByText('删掉一个可以延后的安排。')).toHaveLength(3)
    expect(screen.getByRole('heading', { name: '接下来三年，按这个顺序做' })).toBeInTheDocument()
    expect(screen.getByText(overallReport.content.readingNote)).toBeInTheDocument()
    expect(screen.queryByText('2026.rhythm.evidence')).not.toBeInTheDocument()
    expect(screen.queryByText(/分数|权重/)).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: '为什么先看这件事' })).not.toBeInTheDocument()
  })

  it('综合v1.1展示主焦点如何影响次焦点', async () => {
    vi.mocked(getReport).mockResolvedValue(overallV11Report)
    renderReader()

    expect(await screen.findByText('综合命书 · 通俗版')).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { name: '为什么先看这件事' })).toHaveLength(3)
    for (const year of overallV11Report.content.years) {
      expect(year.linkage).toBeDefined()
      expect(screen.getByText(year.linkage!)).toBeInTheDocument()
    }
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

  it('财富v3读取保存的三年正文，并完整展示五条钱路', async () => {
    vi.mocked(getReport).mockResolvedValue(wealthV3Report)
    renderReader()

    expect(await screen.findByRole('heading', { name: '这三年每年的收入重点不同，不必硬套成一条路。' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '2026 · 丙午' })).toBeInTheDocument()
    expect(screen.getByText('一边看固定收入，一边看别人愿意为什么能力付钱。')).toBeInTheDocument()
    for (const label of ['稳定收入', '靠能力赚钱', '项目和额外收入', '合作带来的收入', '把钱留下']) {
      expect(screen.getByText(label, { selector: '.wealth-path strong' })).toBeInTheDocument()
    }
    expect(screen.queryByText('两年总断')).not.toBeInTheDocument()
  })

  it('财富v3并列方向使用中性标题，不重新包装成最旺钱路', async () => {
    vi.mocked(getReport).mockResolvedValue(wealthV3Report)
    renderReader()

    expect(await screen.findByRole('heading', { name: '可以一起关注的方向' })).toBeInTheDocument()
    expect(screen.getByText('稳定收入、靠能力赚钱')).toBeInTheDocument()
    expect(screen.getByText('这一年没有特别突出的收入方向。')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '相对更值得关注的方向' })).toBeInTheDocument()
    expect(screen.getByText('项目和额外收入', { selector: '.wealth-v3-year__focus p' })).toBeInTheDocument()
    expect(screen.queryByText('最旺钱路')).not.toBeInTheDocument()
    expect(screen.queryByText('主要钱路')).not.toBeInTheDocument()
    expect(screen.queryByText('辅助钱路')).not.toBeInTheDocument()
  })

  it.each(['wealth-plain-v3.4', 'wealth-plain-v3.5'] as const)(
    '财富v4的%s文案使用资金状态标签且不改写历史v3标签', async (copyVersion) => {
    vi.mocked(getReport).mockResolvedValue({
      ...wealthV4Report,
      content: {
        ...wealthV4Report.content,
        copyVersion,
      },
    })
    renderReader()

    await screen.findByText('进账稳定', { selector: '.wealth-path strong' })
    for (const label of ['进账稳定', '投入回报', '到账节奏', '资金责任', '收支结余']) {
      expect(screen.getByText(label, { selector: '.wealth-path strong' })).toBeInTheDocument()
    }
    expect(screen.getByText('资金状态账册')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '五项资金状态逐项看' })).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { name: '这一年的资金变化' })).toHaveLength(3)
    expect(screen.getByText('进账稳定、投入回报')).toBeInTheDocument()
    expect(screen.getByText('到账节奏', { selector: '.wealth-v3-year__focus p' })).toBeInTheDocument()
  })

  it('财富v3有真实限制时才显示三年提醒和年度提醒', async () => {
    const riskReading = wealthBlock('2026.risk', '共同开销增加时，要先说清各自承担多少。', [2026])
    vi.mocked(getReport).mockResolvedValue({
      ...wealthV3Report,
      content: {
        ...wealthV3Report.content,
        riskSummary: wealthBlock('wealth.risk', '2026年需要留意共同开销。', [2026]),
        years: [
          {
            ...wealthV3Report.content.years[0],
            risk: { path: 'retention', limitingEvidenceIds: ['2026.evidence.retention'], reading: riskReading },
          },
          ...wealthV3Report.content.years.slice(1),
        ],
      },
    })
    renderReader()

    expect(await screen.findByRole('heading', { name: '三年里需要留意' })).toBeInTheDocument()
    expect(screen.getByText('2026年需要留意共同开销。')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '需要留意的一项' })).toBeInTheDocument()
    expect(screen.getByText('共同开销增加时，要先说清各自承担多少。')).toBeInTheDocument()
  })

  it('财富v3把观察事项明确标成现实参考，不包装成预测验证', async () => {
    vi.mocked(getReport).mockResolvedValue(wealthV3Report)
    renderReader()

    expect(await screen.findByRole('heading', { name: '可以留意的现实情况' })).toBeInTheDocument()
    expect(screen.getByText('可以留意同一类收入能否重复出现。')).toBeInTheDocument()
    expect(screen.queryByText('现实里出现这些情况，就说明方向正在发生')).not.toBeInTheDocument()
  })

  it('财富v3按年份展示留钱、行动、年度变化和阅读说明', async () => {
    vi.mocked(getReport).mockResolvedValue(wealthV3Report)
    renderReader()

    expect(await screen.findAllByRole('heading', { name: '钱能不能留下' })).toHaveLength(3)
    expect(screen.getByText('收入到账后，先留出日常必需开支。')).toBeInTheDocument()
    expect(screen.getAllByRole('heading', { name: '可以先做这些事' })).toHaveLength(3)
    expect(screen.getByText('记录每笔实际到账和对应成本。')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '和 2027 年相比' })).toBeInTheDocument()
    expect(screen.getByText('到2027年，收入方向会换一批条件来判断。')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '接下来三年，可以这样安排' })).toBeInTheDocument()
    expect(screen.getByText('先记清真实进账，再决定保留哪种收入。')).toBeInTheDocument()
    expect(screen.getByText('这是按年度整理的传统命理参考，不预测具体月份。')).toBeInTheDocument()
  })

  it('财富v3没有负向依据时不渲染提醒卡片或空占位', async () => {
    vi.mocked(getReport).mockResolvedValue(wealthV3Report)
    renderReader()

    await screen.findByRole('heading', { name: '这三年每年的收入重点不同，不必硬套成一条路。' })
    expect(screen.queryByRole('heading', { name: '三年里需要留意' })).not.toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: '需要留意的一项' })).not.toBeInTheDocument()
    expect(screen.queryByText('暂无风险')).not.toBeInTheDocument()
    expect(document.querySelector('.wealth-v3-risk-summary')).not.toBeInTheDocument()
    expect(document.querySelector('.wealth-v3-year__risk')).not.toBeInTheDocument()
  })

  it('财富v3通俗正文不直接暴露内部版本、分数和依据记录', async () => {
    vi.mocked(getReport).mockResolvedValue(wealthV3Report)
    renderReader()

    await screen.findByRole('heading', { name: '这三年每年的收入重点不同，不必硬套成一条路。' })
    expect(screen.queryByText('wealth-path-v2')).not.toBeInTheDocument()
    expect(screen.queryByText('2026.decision.stable_income')).not.toBeInTheDocument()
    expect(screen.queryByText(/净分/)).not.toBeInTheDocument()
    expect(screen.queryByText('专业依据')).not.toBeInTheDocument()
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

  it('未知内容版本明确提示不支持，不默认套用事业阅读结构', async () => {
    vi.mocked(getReport).mockResolvedValue({
      ...report,
      topic: 'wealth',
      contentVersion: 'wealth-narrative-v99',
    } as unknown as SavedReport)
    renderReader()

    expect(await screen.findByRole('heading', { name: '暂不支持读取这份命书' })).toBeInTheDocument()
    expect(screen.getByText('这份命书来自当前页面尚未支持的内容版本。')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '返回我的命书' })).toHaveAttribute('href', '/reports')
    expect(screen.queryByText('两年总断')).not.toBeInTheDocument()
  })

  it('感情v1进入独立阅读页而不是旧事业结构', async () => {
    vi.mocked(getReport).mockResolvedValue(relationshipReport)
    renderReader()

    expect(await screen.findByText('感情命书 · 通俗版')).toBeInTheDocument()
    expect(screen.getByText('五个方面逐项看')).toBeInTheDocument()
    expect(screen.queryByText('两年总断')).not.toBeInTheDocument()
  })
})
