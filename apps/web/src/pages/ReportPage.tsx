import { useState } from 'react'
import { Button, ButtonRow } from '@/components/Button'
import { EmptyState } from '@/components/EmptyState'
import { FooterNote } from '@/components/FooterNote'
import { TopBar } from '@/components/TopBar'
import { GAN_WUXING, WUXING_LABEL } from '@/lib/wuxing'
import { explain } from '@/lib/explainer'
import { computeWangShuai } from '@/lib/geJu'
import { exportMingshuPdf } from '@/lib/reportPdf'
import { useBaziWithFallback } from '@/hooks/useBaziWithFallback'
import { useToastStore } from '@/store/useToastStore'

const TOC = [
  ['壹', '命盘概览', 'P.02'],
  ['贰', '五行与用神', 'P.03'],
  ['叁', '大运流年', 'P.04'],
  ['肆', '十神详析', 'P.05'],
  ['伍', '综合建议', 'P.06'],
] as const

type TocNumber = (typeof TOC)[number][0]

export function ReportPage() {
  const toast = useToastStore((s) => s.show)
  const [exporting, setExporting] = useState(false)
  const [activeChapter, setActiveChapter] = useState<TocNumber>('壹')
  const { request, result } = useBaziWithFallback()
  const explanation = result ? explain(result) : null
  const wangShuai = result ? computeWangShuai(result) : null
  const currentDaYun = result?.daYun.find((item) => item.isCurrent)

  const handleExport = async () => {
    if (!request || !result || !explanation || exporting) return
    setExporting(true)
    try {
      await exportMingshuPdf(request, result, explanation)
      toast('命书 PDF 已生成')
    } catch {
      toast('生成失败，请重试')
    } finally {
      setExporting(false)
    }
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
          {(() => {
            const dayElement = WUXING_LABEL[GAN_WUXING[result.pillars.day.gan]]
            const dayMaster = `${result.pillars.day.gan}${dayElement}`
            const activeToc = TOC.find(([num]) => num === activeChapter) ?? TOC[0]
            const activeChapterIndex = TOC.findIndex(([num]) => num === activeChapter) + 1
            const chapterPoints =
              activeChapter === '贰'
                ? explanation?.blocks.find((block) => block.key === 'daymaster')?.points.slice(0, 2) ?? []
                : activeChapter === '叁'
                  ? explanation?.blocks.filter((block) => block.key === 'dayun' || block.key === 'liunian').flatMap((block) => block.points).slice(0, 2) ?? []
                  : activeChapter === '肆'
                    ? explanation?.blocks.filter((block) => block.key === 'shishen' || block.key === 'shensha').flatMap((block) => block.points).slice(0, 2) ?? []
                    : activeChapter === '伍'
                      ? (explanation?.overview.slice(0, 2).map((text, index) => ({ label: index === 0 ? '命盘提示' : '阅读口径', text })) ?? [])
                      : []

            return (
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
                    <span>
                      {pillar.gan}
                      {pillar.zhi}
                    </span>
                  </div>
                )
              })}
            </div>
            <div className="report-cover__subject">
              {request?.name || '示例'} · {request?.gender === 'male' ? '乾造' : '坤造'}
            </div>
            <div className="report-cover__meta">{result.lunarText} · 五章命书 · 传统文化研究参考</div>
          </section>

          <div className="report-page__document">
            <section className="report-dossier" aria-label="命盘档案">
              <nav className="report-dossier__index" aria-label="章节索引">
                {TOC.map(([num, title]) => (
                  <button
                    aria-current={num === activeChapter ? 'page' : undefined}
                    aria-label={`第${num}章：${title}`}
                    className={num === activeChapter ? 'is-current' : ''}
                    key={num}
                    onClick={() => setActiveChapter(num)}
                    type="button"
                  >
                    {num}
                  </button>
                ))}
              </nav>
              <div className="report-dossier__sheet">
                <header className="report-dossier__heading">
                  <span>{activeChapterIndex < 10 ? `0${activeChapterIndex}` : activeChapterIndex}</span>
                  <h2>{activeToc[1]}</h2>
                </header>

                {activeChapter === '壹' ? (
                  <>
                    <section className="report-dossier__identity">
                      <div className="report-dossier__glyph" aria-hidden="true">
                        {result.pillars.day.gan}
                      </div>
                      <div>
                        <div className="report-dossier__eyebrow">日主档案</div>
                        <h3>{dayMaster}日主</h3>
                        <p>{explanation?.overview[0] ?? '结合命盘结构，以下内容仅供传统文化研究参考。'}</p>
                      </div>
                    </section>

                    <dl className="report-dossier__summary" aria-label="命盘提要">
                      <div>
                        <dt>日主</dt>
                        <dd>{dayMaster}</dd>
                      </div>
                      <div>
                        <dt>旺衰</dt>
                        <dd>{wangShuai?.level ?? '—'}</dd>
                      </div>
                      <div>
                        <dt>当前大运</dt>
                        <dd>{currentDaYun?.ganZhi ?? '—'}</dd>
                      </div>
                    </dl>
                  </>
                ) : (
                  <section aria-live="polite" className="report-dossier__chapter-detail">
                    {activeChapter === '贰' && (
                      <>
                        <div className="report-dossier__chapter-lede">五行分布</div>
                        <dl className="report-dossier__wuxing" aria-label="五行分布">
                          {(['jin', 'mu', 'shui', 'huo', 'tu'] as const).map((key) => (
                            <div key={key}>
                              <dt>{WUXING_LABEL[key]}</dt>
                              <dd>{result.wuXing[key]}</dd>
                            </div>
                          ))}
                        </dl>
                        <p className="report-dossier__scope">本页只展示五行结构；用神需结合流派、寒暖燥湿等综合判断，暂不作确定性推断。</p>
                      </>
                    )}

                    {activeChapter === '叁' && (
                      <dl className="report-dossier__summary report-dossier__summary--two" aria-label="大运流年提要">
                        <div>
                          <dt>当前大运</dt>
                          <dd>{currentDaYun?.ganZhi ?? '—'}</dd>
                        </div>
                        <div>
                          <dt>今年流年</dt>
                          <dd>{result.currentLiuNian?.ganZhi ?? result.currentYearGanZhi}</dd>
                        </div>
                      </dl>
                    )}

                    {activeChapter === '肆' && (
                      <dl className="report-dossier__summary report-dossier__summary--two" aria-label="十神提要">
                        <div>
                          <dt>日主十神</dt>
                          <dd>{result.pillars.day.shiShen}</dd>
                        </div>
                        <div>
                          <dt>命局神煞</dt>
                          <dd>{Array.from(new Set(Object.values(result.pillars).flatMap((pillar) => pillar.shenSha))).length || '—'}</dd>
                        </div>
                      </dl>
                    )}

                    {activeChapter === '伍' && <div className="report-dossier__chapter-lede">传统文化研究参考</div>}

                    <div className="report-dossier__notes">
                      {chapterPoints.map((point) => (
                        <article key={`${point.label}-${point.text}`}>
                          <h3>{point.label}</h3>
                          <p>{point.text}</p>
                        </article>
                      ))}
                    </div>
                  </section>
                )}

              </div>
            </section>

            <div className="report-price">
              深度版命书 <b style={{ color: 'var(--red)', fontSize: 16 }}>¥9.9</b>（示例定价，待定）
            </div>
            <ButtonRow>
              <Button variant="primary" onClick={handleExport} disabled={exporting}>
                {exporting ? '生成中…' : '生成命书'}
              </Button>
              <Button onClick={() => window.history.back()}>返回</Button>
            </ButtonRow>
            <FooterNote>命书内容仅供传统文化研究参考</FooterNote>
          </div>
              </>
            )
          })()}
        </>
      )}
    </div>
  )
}
