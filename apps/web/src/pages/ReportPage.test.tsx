import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import { createReport } from '@/services/reportApi'
import { useAuthStore } from '@/store/useAuthStore'
import { useBaziStore } from '@/store/useBaziStore'
import { ReportPage } from './ReportPage'

vi.mock('@/services/reportApi', () => ({ createReport: vi.fn() }))

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
      </MemoryRouter>
    </QueryClientProvider>,
  )
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
  })

  it('页面阅读版默认进入事业主题，尚未迁移的综合主题不允许误生成', () => {
    renderPage()

    expect(screen.getByRole('radio', { name: /事业运势/ })).toHaveAttribute('aria-checked', 'true')
    expect(screen.getByRole('radio', { name: /通俗版.*6\.9/ })).toHaveAttribute('aria-checked', 'true')

    fireEvent.click(screen.getByRole('radio', { name: /综合运势/ }))
    fireEvent.click(screen.getByRole('radio', { name: /专业版.*12\.9/ }))

    expect(screen.getByRole('button', { name: '生成专业版命书 · ¥12.9' })).toBeDisabled()
    expect(screen.getByText(/综合与感情主题仍在打磨/)).toBeInTheDocument()
  })

  it('财富主题直接展示三年通俗版，并明确禁用专业版', () => {
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
    expect(screen.getByRole('button', { name: '生成通俗版命书 · ¥6.9' })).toBeEnabled()
  })

  it('从事业专业版切到财富主题时自动回到通俗版', () => {
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /专业版.*12\.9/ }))
    expect(screen.getByRole('radio', { name: /专业版.*12\.9/ })).toHaveAttribute('aria-checked', 'true')

    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))

    expect(screen.getByRole('radio', { name: /通俗版.*6\.9/ })).toHaveAttribute('aria-checked', 'true')
    expect(screen.getByRole('radio', { name: /专业版.*设计中.*暂未开放/ })).not.toHaveAttribute(
      'aria-checked',
      'true',
    )
  })

  it('财富通俗版无需问卷即可生成，且不提交上下文', async () => {
    renderPage()
    fireEvent.click(screen.getByRole('radio', { name: /财富运势/ }))

    const generate = screen.getByRole('button', { name: '生成通俗版命书 · ¥6.9' })
    fireEvent.click(generate)

    await waitFor(() => expect(createReport).toHaveBeenCalledWith(
      request,
      'wealth',
      'plain',
    ))
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
    expect(screen.getByRole('button', { name: '生成通俗版命书 · ¥6.9' })).toBeDisabled()

    fireEvent.click(screen.getByRole('radio', { name: '求职中' }))
    fireEvent.click(screen.getByRole('radio', { name: '跳槽换岗' }))
    fireEvent.click(screen.getByRole('radio', { name: '进展停滞' }))

    const generate = screen.getByRole('button', { name: '生成通俗版命书 · ¥6.9' })
    expect(generate).toBeEnabled()
    fireEvent.click(generate)

    await waitFor(() => expect(createReport).toHaveBeenCalledWith(
      request,
      'career',
      'plain',
      { status: 'job_seeking', goal: 'job_change', pace: 'stalled' },
    ))
  })

  afterEach(() => {
    useBaziStore.getState().clear()
    useAuthStore.getState().clear()
    vi.mocked(createReport).mockReset()
    vi.unstubAllEnvs()
  })
})
