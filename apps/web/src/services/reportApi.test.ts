import { afterEach, describe, expect, expectTypeOf, it, vi } from 'vitest'
import type { PaipanRequest } from '@/types/bazi'
import { useAuthStore } from '@/store/useAuthStore'
import { createReport, fetchReportPreview, getReport, listReports } from './reportApi'
import type { WealthContentV3, WealthHeadlineMetaV3 } from './reportApi'

const request: PaipanRequest = {
  name: '林先生',
  gender: 'male',
  solarDateTime: '1995-10-08T13:05:00',
  birthPlace: '广东省 深圳市',
  trueSolarTime: true,
}

describe('reportApi', () => {
  it('财富 v3.9 类型保留标题规划元数据并兼容历史版本', () => {
    const metadata: WealthHeadlineMetaV3 = {
      plannerVersion: 'wealth-headline-v4',
      themeKey: 'project_payment_timing',
      pathKey: 'project_income',
      subjectKey: 'project_terms',
      angleKey: 'support',
      objectKey: 'payment_timing',
      corePhraseKeys: ['project_payment_timing'],
      selectionReasonCodes: ['annual_root', 'catalog.70'],
    }
    const historical: Pick<WealthContentV3, 'copyVersion' | 'headlinePlannerVersion'> = {
      copyVersion: 'wealth-plain-v3.2',
    }
    const current: Pick<WealthContentV3, 'copyVersion' | 'headlinePlannerVersion'> = {
      copyVersion: 'wealth-plain-v3.18',
      headlinePlannerVersion: 'wealth-headline-v4',
    }

    expect(metadata.pathKey).toBe('project_income')
    expect(historical.headlinePlannerVersion).toBeUndefined()
    expect(current.headlinePlannerVersion).toBe('wealth-headline-v4')
    expectTypeOf<WealthContentV3['copyVersion']>().toEqualTypeOf<
      'wealth-plain-v3' | 'wealth-plain-v3.1' | 'wealth-plain-v3.2' | 'wealth-plain-v3.3' | 'wealth-plain-v3.4' | 'wealth-plain-v3.5' | 'wealth-plain-v3.6' | 'wealth-plain-v3.7' | 'wealth-plain-v3.8' | 'wealth-plain-v3.9' | 'wealth-plain-v3.10' | 'wealth-plain-v3.11' | 'wealth-plain-v3.12' | 'wealth-plain-v3.13' | 'wealth-plain-v3.14' | 'wealth-plain-v3.15' | 'wealth-plain-v3.16' | 'wealth-plain-v3.17' | 'wealth-plain-v3.18'
    >()
  })

  it('生成事业命书后返回可保存的页面报告', async () => {
    useAuthStore.getState().setAuth('report-token', 'tester')
    const report = {
      id: 18,
      subject: '林先生',
      topic: 'career',
      edition: 'plain',
      status: 'ready',
      contentVersion: 'career-narrative-v1',
      content: { thesis: '今年先做实成果。', contextSummary: '在职', years: [], route: [] },
      createdAt: '2026-08-23T16:00:00',
      generatedAt: '2026-08-23T16:00:00',
    }
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(report), { status: 200, headers: { 'Content-Type': 'application/json' } }),
    )
    vi.stubGlobal('fetch', fetchMock)
    const careerContext = { status: 'employed' as const, goal: 'promotion' as const, pace: 'smooth' as const }

    await expect(createReport(request, 'career', 'plain', { careerContext })).resolves.toEqual(report)
    expect(fetchMock).toHaveBeenCalledWith('/api/v1/reports', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ request, topic: 'career', edition: 'plain', careerContext }),
    }))
  })

  it('生成财富命书时忽略旧问卷参数，只提交命盘、主题和通俗版本', async () => {
    useAuthStore.getState().setAuth('report-token', 'tester')
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ id: 28, topic: 'wealth' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    const accidentalContext = {
      status: 'employed' as const,
      goal: 'promotion' as const,
      pace: 'smooth' as const,
    }

    await createReport(request, 'wealth', 'plain', { careerContext: accidentalContext })

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/reports', expect.objectContaining({
      body: JSON.stringify({ request, topic: 'wealth', edition: 'plain' }),
    }))
  })

  it('生成感情命书时只提交当前关系状态', async () => {
    useAuthStore.getState().setAuth('report-token', 'tester')
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ id: 38, topic: 'relationship' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    await createReport(request, 'relationship', 'plain', {
      relationshipContext: { status: 'dating' },
    })

    expect(fetchMock).toHaveBeenCalledWith('/api/v1/reports', expect.objectContaining({
      body: JSON.stringify({
        request,
        topic: 'relationship',
        edition: 'plain',
        relationshipContext: { status: 'dating' },
      }),
    }))
  })

  it('读取当前账号的命书列表与单份详情', async () => {
    useAuthStore.getState().setAuth('report-token', 'tester')
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response('[]', { status: 200, headers: { 'Content-Type': 'application/json' } }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ id: 9 }), { status: 200, headers: { 'Content-Type': 'application/json' } }))
    vi.stubGlobal('fetch', fetchMock)

    await expect(listReports()).resolves.toEqual([])
    await expect(getReport(9)).resolves.toEqual({ id: 9 })
    expect(fetchMock).toHaveBeenNthCalledWith(1, '/api/v1/reports', expect.any(Object))
    expect(fetchMock).toHaveBeenNthCalledWith(2, '/api/v1/reports/9', expect.any(Object))
  })

  it('携带主题版本和命盘请求下载后端文字型 PDF', async () => {
    useAuthStore.getState().setAuth('report-token', 'tester')
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(new Blob(['pdf-bytes'], { type: 'application/pdf' }), {
        status: 200,
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Disposition': "attachment; filename*=UTF-8''%E5%91%BD%E4%B9%A6-%E8%B4%A2%E5%AF%8C%E8%BF%90%E5%8A%BF-%E4%B8%93%E4%B8%9A%E7%89%88.pdf",
        },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const result = await fetchReportPreview(request, 'wealth', 'professional')

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/reports/preview',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ request, topic: 'wealth', edition: 'professional' }),
        headers: expect.objectContaining({ Authorization: 'Bearer report-token' }),
      }),
    )
    expect(result.fileName).toBe('命书-财富运势-专业版.pdf')
    expect(result.blob.type).toBe('application/pdf')
  })

  it('后端错误时展示可读业务信息', async () => {
    useAuthStore.getState().setAuth('report-token', 'tester')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify({ code: 'REPORT_TOPIC_UNSUPPORTED', message: '不支持的命书主题' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      }),
    ))

    await expect(fetchReportPreview(request, 'wealth', 'plain')).rejects.toThrow('不支持的命书主题')
  })

  it('生成接口保留业务错误码供页面选择安全文案', async () => {
    useAuthStore.getState().setAuth('report-token', 'tester')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(
      new Response(JSON.stringify({
        code: 'REPORT_GENERATION_UNAVAILABLE',
        message: '本次命书暂未生成成功',
      }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' },
      }),
    ))

    const error = await createReport(request, 'wealth', 'plain').catch((reason: unknown) => reason)

    expect(error).toBeInstanceOf(Error)
    expect(error).toMatchObject({
      code: 'REPORT_GENERATION_UNAVAILABLE',
      message: '本次命书暂未生成成功',
    })
  })

  it('事业主题单独携带完整现实状态问卷', async () => {
    useAuthStore.getState().setAuth('report-token', 'tester')
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(new Blob(['pdf-bytes'], { type: 'application/pdf' }), { status: 200 }),
    )
    vi.stubGlobal('fetch', fetchMock)
    const careerContext = {
      status: 'job_seeking' as const,
      goal: 'job_change' as const,
      pace: 'stalled' as const,
    }

    await fetchReportPreview(request, 'career', 'plain', careerContext)

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/v1/reports/preview',
      expect.objectContaining({
        body: JSON.stringify({ request, topic: 'career', edition: 'plain', careerContext }),
      }),
    )
  })

  afterEach(() => {
    useAuthStore.getState().clear()
    vi.unstubAllGlobals()
  })
})
