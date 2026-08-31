import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation, useNavigate } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import { getMe } from '@/services/membershipApi'
import { mockPayReportCheckout, prepareReportCheckout } from '@/services/payApi'
import { createReport } from '@/services/reportApi'
import { useAuthStore } from '@/store/useAuthStore'
import { useBaziStore } from '@/store/useBaziStore'
import { ReportPage } from './ReportPage'

vi.mock('@/services/reportApi', () => ({ createReport: vi.fn() }))
vi.mock('@/services/membershipApi', () => ({ getMe: vi.fn() }))
vi.mock('@/services/payApi', () => ({
  prepareReportCheckout: vi.fn(),
  mockPayReportCheckout: vi.fn(),
}))

const request = {
  name: '林先生',
  gender: 'male' as const,
  solarDateTime: '1995-10-08T14:30:00',
  birthPlace: '上海',
  trueSolarTime: false,
}

function renderPage() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter>
        <ReportPage />
        <LocationProbe />
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function LocationProbe() {
  return <output data-testid="current-path">{useLocation().pathname}</output>
}

function MembershipReturnStub() {
  const navigate = useNavigate()
  return <button type="button" onClick={() => navigate(-1)}>完成开通并返回</button>
}

function renderMembershipRoundTrip() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(
    <QueryClientProvider client={client}>
      <MemoryRouter initialEntries={['/report']}>
        <Routes>
          <Route path="/report" element={<ReportPage />} />
          <Route path="/membership" element={<MembershipReturnStub />} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

function findMemberGenerateButton() {
  return screen.findByRole('button', { name: '使用会员权益生成通俗版命书' })
}

function findPurchaseGenerateButton() {
  return screen.findByRole('button', { name: '购买并生成通俗版命书 · ¥6.9' })
}

describe('ReportPage', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_API_MODE', 'http')
    useAuthStore.getState().setAuth('report-token', 'tester')
    useBaziStore.getState().setResult(request, paipan(request))
    vi.mocked(createReport).mockResolvedValue({
      id: 18,
      subject: '林先生',
      topic: 'career',
      edition: 'plain',
      status: 'ready',
      contentVersion: 'career-narrative-v1',
      content: { thesis: '今年先做实成果。', contextSummary: '求职中', years: [], route: [] },
      createdAt: '2026-08-23T16:00:00',
      generatedAt: '2026-08-23T16:00:00',
    })
    vi.mocked(getMe).mockResolvedValue({
      username: 'tester',
      plan: 'member_1m',
      memberExpireAt: '2026-09-30T00:00:00',
      isMember: true,
    })
    vi.mocked(prepareReportCheckout).mockResolvedValue({
      orderId: 81,
      reportId: 28,
      topic: 'wealth',
      edition: 'plain',
      amountCents: 690,
      status: 'pending',
    })
    vi.mocked(mockPayReportCheckout).mockResolvedValue({
      id: 28,
      subject: '林先生',
      topic: 'wealth',
      edition: 'plain',
      status: 'ready',
      contentVersion: 'wealth-narrative-v3',
      content: {} as never,
      createdAt: '2026-08-30T16:00:00',
      generatedAt: '2026-08-30T16:00:00',
    })
  })

  it('综合内测仅允许会员额度生成，专业版仍不开放', async () => {
    renderPage()

    expect(screen.getByRole('radio', { name: /事业运势/ })).toHaveAttribute('aria-checked', 'true')
    expect(screen.getByRole('radio', { name: /通俗版.*6\.9/ })).toHaveAttribute('aria-checked', 'true')
    const professional = screen.getByRole('radio', { name: /专业版.*设计中.*暂未开放/ })
    expect(professional).toBeDisabled()
    expect(professional).not.toHaveTextContent('12.9')

    fireEvent.click(screen.getByRole('radio', { name: /综合运势/ }))

    const generate = await findMemberGenerateButton()
    expect(generate).toBeEnabled()
    expect(screen.getByText(/综合通俗版内测中.*会员额度.*暂不单独售卖/)).toBeInTheDocument()
    fireEvent.click(generate)
    await waitFor(() => expect(createReport).toHaveBeenCalledWith(
      request,
      'overall',
      'plain',
    ))
    expect(prepareReportCheckout).not.toHaveBeenCalled()
  })

  it('非会员可从综合内测入口前往会员页，但不能创建单份报告订单', async () => {
    vi.mocked(getMe).mockResolvedValue({
      username: 'tester', plan: null, memberExpireAt: null, isMember: false,
    })
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /综合运势/ }))

    const button = await screen.findByRole('button', { name: '开通会员后生成综合命书' })
    expect(button).toBeEnabled()
    expect(screen.getByText('综合通俗版内测中，会员可生成；暂不支持单份购买')).toBeInTheDocument()
    fireEvent.click(button)
    expect(screen.getByTestId('current-path')).toHaveTextContent('/membership')
    expect(createReport).not.toHaveBeenCalled()
    expect(prepareReportCheckout).not.toHaveBeenCalled()
  })

  it('开通会员返回后保留综合主题并可直接生成', async () => {
    vi.mocked(getMe)
      .mockResolvedValueOnce({
        username: 'tester', plan: null, memberExpireAt: null, isMember: false,
      })
      .mockResolvedValue({
        username: 'tester',
        plan: 'member_1m',
        memberExpireAt: '2026-09-30T00:00:00',
        isMember: true,
      })
    renderMembershipRoundTrip()

    fireEvent.click(screen.getByRole('radio', { name: /综合运势/ }))
    fireEvent.click(await screen.findByRole('button', { name: '开通会员后生成综合命书' }))
    fireEvent.click(screen.getByRole('button', { name: '完成开通并返回' }))

    expect(await screen.findByRole('radio', { name: /综合运势/ })).toHaveAttribute('aria-checked', 'true')
    expect(await findMemberGenerateButton()).toBeEnabled()
  })

  it('综合内测达到会员日限额后不提供单份购买入口', async () => {
    vi.mocked(createReport).mockRejectedValue(Object.assign(
      new Error('MEMBER_DAILY_REPORT_LIMIT'),
      { code: 'MEMBER_DAILY_REPORT_LIMIT' },
    ))
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /综合运势/ }))
    fireEvent.click(await findMemberGenerateButton())

    expect(await screen.findByText(
      '今天的3份会员命书已全部生成；综合版内测期间暂不支持单独购买，本次未扣费',
    )).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /单独购买通俗版命书/ })).not.toBeInTheDocument()
    expect(prepareReportCheckout).not.toHaveBeenCalled()
  })

  it('会员额度内的按钮不显示单份价格，避免误以为仍会扣费', async () => {
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))

    expect(await screen.findByRole('button', {
      name: '使用会员权益生成通俗版命书',
    })).toBeEnabled()
    expect(screen.getByLabelText('已选命书')).toHaveTextContent('会员额度内免单')
  })

  it('财富主题直接展示三年通俗版，并明确禁用专业版', async () => {
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))

    expect(screen.queryByRole('heading', { name: '补充财富现状' })).not.toBeInTheDocument()
    expect(screen.getByLabelText('命书封面')).toHaveTextContent('财富运势 · 通俗版 · 未来三年')
    expect(screen.getByLabelText('已选命书')).toHaveTextContent('未来三年')
    expect(screen.getByRole('radio', { name: /通俗版.*6\.9/ })).toHaveAttribute('aria-checked', 'true')
    const professional = screen.getByRole('radio', { name: /专业版.*设计中.*暂未开放/ })
    expect(professional).toBeDisabled()
    expect(professional).toHaveAttribute('aria-disabled', 'true')
    expect(professional).not.toHaveTextContent('12.9')
    expect(await findMemberGenerateButton()).toBeEnabled()
  })

  it('财富通俗版无需问卷即可生成，且不提交上下文', async () => {
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))

    const generate = await findMemberGenerateButton()
    fireEvent.click(generate)

    await waitFor(() => expect(createReport).toHaveBeenCalledWith(
      request,
      'wealth',
      'plain',
    ))
  })

  it('非会员不会调用免费生成接口，而是创建订单并支付后打开同一份命书', async () => {
    vi.mocked(getMe).mockResolvedValue({
      username: 'tester',
      plan: null,
      memberExpireAt: null,
      isMember: false,
    })
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))
    fireEvent.click(await findPurchaseGenerateButton())

    await waitFor(() => expect(prepareReportCheckout).toHaveBeenCalledWith(
      request,
      'wealth',
      'plain',
    ))
    expect(mockPayReportCheckout).toHaveBeenCalledWith(81)
    expect(createReport).not.toHaveBeenCalled()
  })

  it('生成质量失败时只说明权益未扣除，不展示技术错误', async () => {
    vi.mocked(createReport).mockRejectedValue(Object.assign(
      new Error('wealth headline planning failed: insufficient_diversity'),
      { code: 'REPORT_GENERATION_UNAVAILABLE' },
    ))
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))
    fireEvent.click(await findMemberGenerateButton())

    expect(await screen.findByText('本次命书暂未生成成功，未扣除费用或使用次数，请稍后再试')).toBeInTheDocument()
    expect(screen.queryByText(/insufficient_diversity/)).not.toBeInTheDocument()
  })

  it('未知服务异常也不向用户展示技术信息', async () => {
    vi.mocked(createReport).mockRejectedValue(new Error('NetworkError: connection reset by peer'))
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))
    fireEvent.click(await findMemberGenerateButton())

    expect(await screen.findByText('命书暂未生成成功，请稍后再试；本次不会扣除费用或使用次数')).toBeInTheDocument()
    expect(screen.queryByText(/connection reset/)).not.toBeInTheDocument()
  })

  it('会员当天三份用完后说明可单独购买且本次未扣费', async () => {
    vi.mocked(createReport).mockRejectedValue(Object.assign(
      new Error('MEMBER_DAILY_REPORT_LIMIT'),
      { code: 'MEMBER_DAILY_REPORT_LIMIT' },
    ))
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))
    fireEvent.click(await findMemberGenerateButton())

    expect(await screen.findByText('今天的3份会员命书已全部生成；继续生成需单独购买，本次未扣费')).toBeInTheDocument()
    expect(screen.queryByText('MEMBER_DAILY_REPORT_LIMIT')).not.toBeInTheDocument()
    expect(prepareReportCheckout).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: '单独购买通俗版命书 · ¥6.9' }))
    await waitFor(() => expect(prepareReportCheckout).toHaveBeenCalled())
    expect(mockPayReportCheckout).toHaveBeenCalledWith(81)
  })

  it('单份支付失败时不假装命书已生成，也不展示技术错误', async () => {
    vi.mocked(getMe).mockResolvedValue({
      username: 'tester', plan: null, memberExpireAt: null, isMember: false,
    })
    vi.mocked(mockPayReportCheckout).mockRejectedValue(new Error('provider socket closed'))
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))
    fireEvent.click(await findPurchaseGenerateButton())

    expect(await screen.findByText('支付未完成，命书尚未解锁，请稍后再试')).toBeInTheDocument()
    expect(screen.queryByText(/socket closed/)).not.toBeInTheDocument()
    expect(createReport).not.toHaveBeenCalled()
  })

  it('感情主题只显示三种关系状态，选中后才能生成通俗版', async () => {
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /感情运势/ }))

    expect(screen.queryByRole('heading', { name: '补充事业现状' })).not.toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '选择当前关系状态' })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '单身或尚未确定关系' })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '已确认交往关系' })).toBeInTheDocument()
    expect(screen.getByRole('radio', { name: '已婚或长期共同生活' })).toBeInTheDocument()
    expect(await findMemberGenerateButton()).toBeDisabled()
    expect(screen.getByRole('radio', { name: /专业版.*设计中.*暂未开放/ })).toBeDisabled()

    fireEvent.click(screen.getByRole('radio', { name: '已确认交往关系' }))
    fireEvent.click(await findMemberGenerateButton())

    await waitFor(() => expect(createReport).toHaveBeenCalledWith(
      request,
      'relationship',
      'plain',
      { relationshipContext: { status: 'dating' } },
    ))
  })

  it('单身显示今年重点和明年参考，切换状态恢复三年，未选状态不承诺周期', () => {
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /感情运势/ }))
    expect(screen.getByLabelText('命书封面')).not.toHaveTextContent('未来三年')
    fireEvent.click(screen.getByRole('radio', { name: '单身或尚未确定关系' }))
    expect(screen.getByLabelText('命书封面')).toHaveTextContent('今年重点＋明年参考')
    expect(screen.getByLabelText('已选命书')).toHaveTextContent('今年重点＋明年参考')
    expect(screen.getByText(/年度解读，不是未来十二个月/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('radio', { name: '已婚或长期共同生活' }))
    expect(screen.getByLabelText('命书封面')).toHaveTextContent('未来三年')
    fireEvent.click(screen.getByRole('radio', { name: '已确认交往关系' }))
    expect(screen.getByLabelText('命书封面')).toHaveTextContent('未来三年')
  })

  it('离开感情主题后清除上一次选择', async () => {
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /感情运势/ }))
    fireEvent.click(screen.getByRole('radio', { name: '单身或尚未确定关系' }))
    expect(await findMemberGenerateButton()).toBeEnabled()

    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))
    fireEvent.click(screen.getByRole('radio', { name: /感情运势/ }))

    expect(screen.getByRole('radio', { name: '单身或尚未确定关系' })).toHaveAttribute(
      'aria-checked',
      'false',
    )
    expect(await findMemberGenerateButton()).toBeDisabled()
  })

  it('未登录时明确提示需要登录而不调用生成接口', () => {
    useAuthStore.getState().clear()
    renderPage()

    expect(screen.getByRole('button', { name: '登录后生成命书' })).toBeDisabled()
    expect(screen.getByText('后端联调生成需要先登录')).toBeInTheDocument()
    expect(createReport).not.toHaveBeenCalled()
  })

  it('事业主题完成三项状态问卷后才允许生成并提交答案', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: '补充事业现状' })).toBeInTheDocument()
    expect(screen.getByText('不会改变命盘计算，只帮助判断落到岗位、求职或经营场景')).toBeInTheDocument()
    expect(await findMemberGenerateButton()).toBeDisabled()

    fireEvent.click(screen.getByRole('radio', { name: '求职中' }))
    fireEvent.click(screen.getByRole('radio', { name: '跳槽换岗' }))
    fireEvent.click(screen.getByRole('radio', { name: '进展停滞' }))

    const generate = await findMemberGenerateButton()
    expect(generate).toBeEnabled()
    fireEvent.click(generate)

    await waitFor(() => expect(createReport).toHaveBeenCalledWith(
      request,
      'career',
      'plain',
      { careerContext: { status: 'job_seeking', goal: 'job_change', pace: 'stalled' } },
    ))
  })

  afterEach(() => {
    useBaziStore.getState().clear()
    useAuthStore.getState().clear()
    vi.mocked(createReport).mockReset()
    vi.mocked(getMe).mockReset()
    vi.mocked(prepareReportCheckout).mockReset()
    vi.mocked(mockPayReportCheckout).mockReset()
    vi.unstubAllEnvs()
  })
})
