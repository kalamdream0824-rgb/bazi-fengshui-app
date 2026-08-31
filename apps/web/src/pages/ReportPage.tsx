import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Button, ButtonRow } from '@/components/Button'
import { EmptyState } from '@/components/EmptyState'
import { FooterNote } from '@/components/FooterNote'
import { TopBar } from '@/components/TopBar'
import { useBaziWithFallback } from '@/hooks/useBaziWithFallback'
import { getMe } from '@/services/membershipApi'
import { mockPayReportCheckout, prepareReportCheckout } from '@/services/payApi'
import {
  createReport,
  type CareerContextInput,
  type RelationshipStatus,
  type ReportEditionCode,
  type ReportTopicCode,
} from '@/services/reportApi'
import { useAuthStore } from '@/store/useAuthStore'
import { useToastStore } from '@/store/useToastStore'
import type { MembershipInfo } from '@/types/bazi'

const TOPICS: Array<{ code: ReportTopicCode; seal: string; label: string; note: string }> = [
  { code: 'overall', seal: '总', label: '综合运势', note: '三年主线与年度节奏' },
  { code: 'career', seal: '业', label: '事业运势', note: '职责、协作与发展窗口' },
  { code: 'wealth', seal: '财', label: '财富运势', note: '收入、支出与合同约定' },
  { code: 'relationship', seal: '缘', label: '感情运势', note: '互动、沟通与关系边界' },
]

const EDITIONS: Array<{
  code: ReportEditionCode
  label: string
  price: string
  note: string
}> = [
  { code: 'plain', label: '通俗版', price: '6.9', note: '直白结论 · 现实信号 · 行动建议' },
  { code: 'professional', label: '专业版', price: '12.9', note: '规则键 · 完整证据 · 置信等级' },
]

type CareerContextKey = keyof CareerContextInput

const CAREER_QUESTIONS: Array<{
  key: CareerContextKey
  seal: string
  label: string
  options: Array<{ value: string; label: string }>
}> = [
  {
    key: 'status',
    seal: '壹',
    label: '当前状态',
    options: [
      { value: 'employed', label: '在职' },
      { value: 'self_employed', label: '自营或创业' },
      { value: 'job_seeking', label: '求职中' },
      { value: 'studying', label: '学习或准备入行' },
    ],
  },
  {
    key: 'goal',
    seal: '贰',
    label: '当前目标',
    options: [
      { value: 'promotion', label: '晋升增责' },
      { value: 'job_change', label: '跳槽换岗' },
      { value: 'stability', label: '稳住现状' },
      { value: 'transition', label: '转型换方向' },
    ],
  },
  {
    key: 'pace',
    seal: '叁',
    label: '近期体感',
    options: [
      { value: 'smooth', label: '推进顺利' },
      { value: 'stalled', label: '进展停滞' },
      { value: 'high_pressure', label: '负荷很高' },
      { value: 'preparing_change', label: '正在准备变化' },
    ],
  },
]

const RELATIONSHIP_STATUSES: Array<{ code: RelationshipStatus; seal: string; label: string }> = [
  { code: 'single', seal: '初', label: '单身或尚未确定关系' },
  { code: 'dating', seal: '伴', label: '已确认交往关系' },
  { code: 'married', seal: '合', label: '已婚或长期共同生活' },
]

function completedCareerContext(
  value: Partial<CareerContextInput>,
): CareerContextInput | null {
  if (!value.status || !value.goal || !value.pace) return null
  return { status: value.status, goal: value.goal, pace: value.pace }
}

function generationFailureMessage(error: unknown): string {
  const code = error instanceof Error && 'code' in error
    ? String((error as Error & { code: unknown }).code)
    : ''
  if (code === 'MEMBER_DAILY_REPORT_LIMIT') {
    return '今天的3份会员命书已全部生成；继续生成需单独购买，本次未扣费'
  }
  if (code === 'REPORT_GENERATION_UNAVAILABLE') {
    return '本次命书暂未生成成功，未扣除费用或使用次数，请稍后再试'
  }
  return '命书暂未生成成功，请稍后再试；本次不会扣除费用或使用次数'
}

export function ReportPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const token = useAuthStore((state) => state.token)
  const toast = useToastStore((state) => state.show)
  const { request, result } = useBaziWithFallback()
  const isHttpMode = import.meta.env.VITE_API_MODE === 'http'
  const [topic, setTopic] = useState<ReportTopicCode>(() => {
    const requestedTopic = searchParams.get('topic')
    return TOPICS.some((item) => item.code === requestedTopic)
      ? requestedTopic as ReportTopicCode
      : 'career'
  })
  const [edition, setEdition] = useState<ReportEditionCode>('plain')
  const [careerContext, setCareerContext] = useState<Partial<CareerContextInput>>({})
  const [relationshipStatus, setRelationshipStatus] = useState<RelationshipStatus | null>(null)
  const [generating, setGenerating] = useState(false)
  const [status, setStatus] = useState('')
  const [purchaseAvailable, setPurchaseAvailable] = useState(false)
  const [overallDailyLimitReached, setOverallDailyLimitReached] = useState(false)
  const [membership, setMembership] = useState<MembershipInfo | null>(null)
  const [membershipStatus, setMembershipStatus] = useState<'idle' | 'loading' | 'ready' | 'error'>(
    token && isHttpMode ? 'loading' : 'idle',
  )

  const selectedTopic = TOPICS.find((item) => item.code === topic) ?? TOPICS[0]
  const selectedEdition = EDITIONS.find((item) => item.code === edition) ?? EDITIONS[0]
  const completeCareerContext = completedCareerContext(careerContext)
  const reportYears = topic === 'relationship'
    ? relationshipStatus === 'single' ? '今年重点＋明年参考'
      : relationshipStatus ? '未来三年' : '请先选择关系状态'
    : topic === 'career' ? '未来两年' : '未来三年'
  const canGenerate = Boolean(
    token && isHttpMode && request && (
      (topic === 'career' && completeCareerContext)
      || (topic === 'overall' && membership?.isMember && !overallDailyLimitReached)
      || topic === 'wealth'
      || (topic === 'relationship' && relationshipStatus)
    ),
  )
  const canOpenMembership = Boolean(
    token
      && isHttpMode
      && topic === 'overall'
      && membershipStatus === 'ready'
      && membership
      && !membership.isMember,
  )

  useEffect(() => {
    let active = true
    if (!token || !isHttpMode) {
      return () => { active = false }
    }

    getMe()
      .then((info) => {
        if (!active) return
        setMembership(info)
        setMembershipStatus('ready')
      })
      .catch(() => {
        if (!active) return
        setMembership(null)
        setMembershipStatus('error')
      })

    return () => { active = false }
  }, [isHttpMode, token])

  const handleTopicChange = (nextTopic: ReportTopicCode) => {
    setTopic(nextTopic)
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      next.set('topic', nextTopic)
      return next
    }, { replace: true })
    setRelationshipStatus(null)
    setEdition('plain')
    setPurchaseAvailable(false)
    setOverallDailyLimitReached(false)
    setStatus('')
  }

  const handleGenerate = async () => {
    if (!request || !canGenerate || generating || membershipStatus !== 'ready' || !membership) return
    setGenerating(true)
    setStatus('')
    let purchaseStage: 'none' | 'preparing' | 'paying' = purchaseAvailable ? 'preparing' : 'none'
    try {
      const options = topic === 'career'
        ? { careerContext: completeCareerContext ?? undefined }
        : topic === 'relationship' && relationshipStatus
          ? { relationshipContext: { status: relationshipStatus } }
          : undefined
      if (!purchaseAvailable && !membership.isMember) purchaseStage = 'preparing'

      let report
      if (purchaseStage === 'none') {
        report = options
          ? await createReport(request, topic, edition, options)
          : await createReport(request, topic, edition)
      } else {
        const checkout = options
          ? await prepareReportCheckout(request, topic, edition, options)
          : await prepareReportCheckout(request, topic, edition)
        purchaseStage = 'paying'
        report = await mockPayReportCheckout(checkout.orderId)
      }
      const message = purchaseStage === 'paying'
        ? `模拟支付完成，${selectedEdition.label}命书已解锁并保存`
        : `${selectedEdition.label}命书已生成并保存`
      setStatus(message)
      toast(message)
      navigate(`/reports/${report.id}`)
    } catch (error) {
      const code = error instanceof Error && 'code' in error
        ? String((error as Error & { code: unknown }).code)
        : ''
      if (code === 'MEMBER_DAILY_REPORT_LIMIT') {
        if (topic === 'overall') setOverallDailyLimitReached(true)
        else setPurchaseAvailable(true)
      }
      const message = topic === 'overall' && code === 'MEMBER_DAILY_REPORT_LIMIT'
        ? '今天的3份会员命书已全部生成；综合版内测期间暂不支持单独购买，本次未扣费'
        : purchaseStage === 'paying'
        ? '支付未完成，命书尚未解锁，请稍后再试'
        : purchaseStage === 'preparing' && code !== 'REPORT_GENERATION_UNAVAILABLE'
          ? '命书暂未生成成功，本次未付款，请稍后再试'
          : generationFailureMessage(error)
      setStatus(message)
      toast(message)
    } finally {
      setGenerating(false)
    }
  }

  const handlePrimaryAction = () => {
    if (canOpenMembership) {
      navigate('/membership')
      return
    }
    void handleGenerate()
  }

  return (
    <div className="report-page">
      <TopBar title="命书报告" tone="night" />

      {!result ? (
        <div className="report-page__empty">
          <EmptyState title="还没有命盘数据" hint="先去排一次盘" linkTo="/input" linkText="先去排一次盘" />
          <FooterNote>命书内容仅供传统文化研究参考</FooterNote>
        </div>
      ) : (
        <>
          <section className="report-cover" aria-label="命书封面">
            <div className="report-cover__orbit" aria-hidden="true" />
            <div className="report-cover__kicker">八字命书</div>
            <span className="report-cover__seal">命</span>
            <h1 className="report-cover__title">命书</h1>
            <div className="report-cover__pillars" aria-label="四柱">
              {(['year', 'month', 'day', 'time'] as const).map((key) => {
                const pillar = result.pillars[key]
                const label = key === 'year' ? '年柱' : key === 'month' ? '月柱' : key === 'day' ? '日柱' : '时柱'
                return (
                  <div className={`report-cover__pillar ${key === 'day' ? 'is-day' : ''}`} key={key}>
                    <small>{label}</small>
                    <span>{pillar.gan}{pillar.zhi}</span>
                  </div>
                )
              })}
            </div>
            <div className="report-cover__subject">
              {request?.name || '示例'} · {request?.gender === 'male' ? '乾造' : '坤造'}
            </div>
            <div className="report-cover__meta">
              {selectedTopic.label} · {selectedEdition.label} · {reportYears}
            </div>
          </section>

          <main className="report-page__document">
            <section className="report-order" aria-label="命书生成选择">
              <header className="report-order__header">
                <span className="report-order__eyebrow">命题</span>
                <div>
                  <h2>这份命书，重点回答什么？</h2>
                  <p>选择一个主题，报告会让它贯穿逐年判断与行动路线。</p>
                </div>
              </header>

              <div className="report-topic-slips" role="radiogroup" aria-label="命书主题">
                {TOPICS.map((item) => (
                  <button
                    aria-checked={topic === item.code}
                    className={`report-topic-slip ${topic === item.code ? 'is-selected' : ''}`}
                    key={item.code}
                    onClick={() => handleTopicChange(item.code)}
                    role="radio"
                    type="button"
                  >
                    <span className="report-topic-slip__seal" aria-hidden="true">{item.seal}</span>
                    <span>
                      <strong>{item.label}</strong>
                      <small>{item.note}</small>
                    </span>
                  </button>
                ))}
              </div>

              {topic === 'career' && (
                <section className="career-questionnaire" aria-labelledby="career-questionnaire-title">
                  <header className="career-questionnaire__header">
                    <span aria-hidden="true">问</span>
                    <div>
                      <h3 id="career-questionnaire-title">补充事业现状</h3>
                      <p>不会改变命盘计算，只帮助判断落到岗位、求职或经营场景</p>
                    </div>
                  </header>
                  <div className="career-questionnaire__body">
                    {CAREER_QUESTIONS.map((question) => {
                      const labelId = `career-question-${question.key}`
                      return (
                        <div className="career-question" key={question.key}>
                          <div className="career-question__label" id={labelId}>
                            <span aria-hidden="true">{question.seal}</span>
                            <strong>{question.label}</strong>
                          </div>
                          <div
                            aria-labelledby={labelId}
                            className="career-question__options"
                            role="radiogroup"
                          >
                            {question.options.map((option) => {
                              const selected = careerContext[question.key] === option.value
                              return (
                                <button
                                  aria-checked={selected}
                                  className={`career-question__option ${selected ? 'is-selected' : ''}`}
                                  key={option.value}
                                  onClick={() => setCareerContext((current) => ({
                                    ...current,
                                    [question.key]: option.value,
                                  }))}
                                  role="radio"
                                  type="button"
                                >
                                  {option.label}
                                </button>
                              )
                            })}
                          </div>
                        </div>
                      )
                    })}
                  </div>
                </section>
              )}

              {topic === 'relationship' && (
                <section className="relationship-context" aria-labelledby="relationship-context-title">
                  <header className="relationship-context__header">
                    <span aria-hidden="true">缘</span>
                    <div>
                      <h3 id="relationship-context-title">选择当前关系状态</h3>
                      <p>{relationshipStatus === 'single'
                        ? '今年详细展开，明年简短参考；年度解读，不是未来十二个月'
                        : '按关系状态安排阅读重点，同一年度的命盘计算不变'}</p>
                    </div>
                  </header>
                  <div className="relationship-context__choices" role="radiogroup" aria-labelledby="relationship-context-title">
                    {RELATIONSHIP_STATUSES.map((item) => (
                      <button
                        aria-checked={relationshipStatus === item.code}
                        className={`relationship-context__choice ${relationshipStatus === item.code ? 'is-selected' : ''}`}
                        key={item.code}
                        onClick={() => setRelationshipStatus(item.code)}
                        role="radio"
                        type="button"
                      >
                        <span aria-hidden="true">{item.seal}</span>
                        <strong>{item.label}</strong>
                      </button>
                    ))}
                  </div>
                </section>
              )}

              <div className="report-order__divider"><span>选择解读版本</span></div>

              <div className="report-edition-plates" role="radiogroup" aria-label="命书版本">
                {EDITIONS.map((item) => {
                  const unavailable = item.code === 'professional'
                  return (
                    <button
                      aria-checked={!unavailable && edition === item.code}
                      aria-disabled={unavailable}
                      className={`report-edition-plate ${edition === item.code ? 'is-selected' : ''} ${unavailable ? 'is-disabled' : ''}`}
                      disabled={unavailable}
                      key={item.code}
                      onClick={() => setEdition(item.code)}
                      role="radio"
                      type="button"
                    >
                      <span className="report-edition-plate__topline">
                        <strong>{item.label}</strong>
                        {unavailable
                          ? <em>设计中 · 暂未开放</em>
                          : topic === 'overall' ? <em>会员内测</em> : <b>¥{item.price}</b>}
                      </span>
                      <small>{unavailable
                        ? '专业内容标准尚未完成验收'
                        : topic === 'overall' ? '四维逐年判断 · 暂不单独售卖' : item.note}</small>
                    </button>
                  )
                })}
              </div>

              <aside className="report-order__summary" aria-label="已选命书">
                <span>已选</span>
                <strong>{selectedTopic.label} · {selectedEdition.label}</strong>
                <small>
                  {reportYears}
                  {membershipStatus === 'loading' && ' · 正在确认会员权益'}
                  {membershipStatus === 'error' && ' · 暂时无法确认会员权益'}
                  {membershipStatus === 'ready' && topic === 'overall' && membership?.isMember
                    && ' · 会员内测 · 成功后计入今日次数'}
                  {membershipStatus === 'ready' && topic === 'overall' && !membership?.isMember
                    && ' · 内测中 · 暂不单独售卖'}
                  {membershipStatus === 'ready' && topic !== 'overall' && membership?.isMember
                    && ' · 会员额度内免单 · 成功后计入今日次数'}
                  {membershipStatus === 'ready' && topic !== 'overall' && !membership?.isMember
                    && ` · 单份购买 ¥${selectedEdition.price} · 当前为模拟支付`}
                </small>
              </aside>

              {!token && <p className="report-order__notice">后端联调生成需要先登录</p>}
              {token && !isHttpMode && (
                <p className="report-order__notice">请使用 VITE_API_MODE=http 启动前端以连接报告服务</p>
              )}
              {token && isHttpMode && membershipStatus === 'error' && (
                <p className="report-order__notice">暂时无法确认会员权益，请刷新页面后重试</p>
              )}
              {token && isHttpMode && topic === 'career' && !completeCareerContext && (
                <p className="report-order__notice">完成三项事业状态后即可生成</p>
              )}
              {token && isHttpMode && topic === 'relationship' && !relationshipStatus && (
                <p className="report-order__notice">选择当前关系状态后即可生成</p>
              )}
              {token && isHttpMode && topic === 'overall' && membershipStatus === 'ready' && (
                <p className="report-order__notice">{membership?.isMember
                  ? '综合通俗版内测中，可使用会员额度生成；暂不单独售卖'
                  : '综合通俗版内测中，会员可生成；暂不支持单份购买'}</p>
              )}

              <ButtonRow>
                <Button
                  variant="primary"
                  onClick={handlePrimaryAction}
                  disabled={(!canGenerate && !canOpenMembership) || generating || membershipStatus !== 'ready'}
                >
                  {!token
                    ? '登录后生成命书'
                    : generating
                      ? `正在生成${selectedEdition.label}…`
                      : membershipStatus === 'loading'
                        ? '正在确认会员权益…'
                      : membershipStatus === 'error'
                          ? '暂时无法确认会员权益'
                      : topic === 'overall' && overallDailyLimitReached
                        ? '今日会员次数已用完'
                      : topic === 'overall' && !membership?.isMember
                        ? '开通会员后生成综合命书'
                      : purchaseAvailable
                        ? `单独购买${selectedEdition.label}命书 · ¥${selectedEdition.price}`
                        : membership?.isMember
                          ? `使用会员权益生成${selectedEdition.label}命书`
                          : `购买并生成${selectedEdition.label}命书 · ¥${selectedEdition.price}`}
                </Button>
                <Button onClick={() => window.history.back()}>返回</Button>
              </ButtonRow>

              <p aria-live="polite" className="report-order__status">{status}</p>
            </section>
            <FooterNote>命书内容仅供传统文化研究参考，不作为现实决策的唯一依据</FooterNote>
          </main>
        </>
      )}
    </div>
  )
}
