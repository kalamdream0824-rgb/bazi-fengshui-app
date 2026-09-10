import { Link } from 'react-router-dom'
import type { WealthPathV3, WealthV3SavedReport, WealthYearV3 } from '@/services/reportApi'
import { NarrativeTimeline } from './NarrativeTimeline'

type AnnualCopy = { text: string; appliesToYears: number[] }
type AnnualCopySections = {
  income: AnnualCopy[]
  retention: AnnualCopy[]
  risk: AnnualCopy[]
  observations: AnnualCopy[]
  actions: AnnualCopy[]
}

const LEGACY_PATH_LABELS: Record<WealthPathV3, string> = {
  stable_income: '稳定收入',
  skill_income: '靠能力赚钱',
  project_income: '项目和额外收入',
  cooperation_income: '合作带来的收入',
  retention: '把钱留下',
}

const MONEY_STATE_LABELS: Record<WealthPathV3, string> = {
  stable_income: '进账稳定',
  skill_income: '投入回报',
  project_income: '到账节奏',
  cooperation_income: '资金责任',
  retention: '收支结余',
}

const ANNUAL_COPY_GROUPING_VERSIONS = new Set(['wealth-plain-v3.6', 'wealth-plain-v3.7', 'wealth-plain-v3.8', 'wealth-plain-v3.9', 'wealth-plain-v3.10', 'wealth-plain-v3.11', 'wealth-plain-v3.12', 'wealth-plain-v3.13', 'wealth-plain-v3.14', 'wealth-plain-v3.15', 'wealth-plain-v3.16'])

export function WealthV3ReportReader({ report }: { report: WealthV3SavedReport }) {
  const usesMoneyStateCopy = ['wealth-plain-v3.4', 'wealth-plain-v3.5', 'wealth-plain-v3.6', 'wealth-plain-v3.7', 'wealth-plain-v3.8', 'wealth-plain-v3.9', 'wealth-plain-v3.10', 'wealth-plain-v3.11', 'wealth-plain-v3.12', 'wealth-plain-v3.13', 'wealth-plain-v3.14', 'wealth-plain-v3.15', 'wealth-plain-v3.16'].includes(report.content.copyVersion)
  const pathLabels = usesMoneyStateCopy ? MONEY_STATE_LABELS : LEGACY_PATH_LABELS
  const annualCopy = groupRepeatedAnnualCopy(
    report.content.years,
    ANNUAL_COPY_GROUPING_VERSIONS.has(report.content.copyVersion),
  )

  return (
    <main className="report-reader__paper wealth-reader wealth-v3-reader">
      <header className="report-reader__masthead">
        <div>
          <span>财富命书 · 通俗版</span>
          <small>{report.subject} · {report.content.asOf.slice(0, 4)} 年起三年</small>
        </div>
        <i aria-hidden="true">财</i>
      </header>

      <section className="report-reader__thesis" aria-labelledby="wealth-v3-thesis">
        <span>三年总览</span>
        <h1 id="wealth-v3-thesis">{report.content.thesis.text}</h1>
        <p className="wealth-reader__summary">{report.content.summary.text}</p>
      </section>

      <NarrativeTimeline timeline={report.content.timeline} />

      <section className="wealth-reader__paths" aria-labelledby="wealth-v3-paths">
        <header>
          <span>{usesMoneyStateCopy ? '资金状态账册' : '五路账册'}</span>
          <h2 id="wealth-v3-paths">{usesMoneyStateCopy ? '五项资金状态逐项看' : '五条钱路逐项看'}</h2>
        </header>
        <div>
          {report.content.pathSummaries.map((path, index) => (
            <article className={`wealth-path is-${path.path}`} key={path.path}>
              <b aria-hidden="true">{index + 1}</b>
              <span>
                <strong>{pathLabels[path.path]}</strong>
                <p>{path.reading.text}</p>
              </span>
            </article>
          ))}
        </div>
      </section>

      {report.content.riskSummary ? (
        <section className="wealth-v3-risk-summary">
          <h2>三年里需要留意</h2>
          <p>{report.content.riskSummary.text}</p>
        </section>
      ) : null}

      <div className="report-reader__years wealth-reader__years">
        {report.content.years.map((year, index) => {
          const copy = annualCopy.get(year.year)!
          return <article className="report-year wealth-year" key={year.year}>
            <div className="report-year__rail" aria-hidden="true">
              <b>{index + 1}</b><span />
            </div>
            <div className="report-year__body">
              <header><h2>{year.year} · {year.ganZhi}</h2></header>
              <p className="report-year__verdict">{year.overview.text}</p>
              {year.focus.state === 'tied' ? (
                <section className="wealth-v3-year__focus is-tied">
                  <h3>可以一起关注的方向</h3>
                  <p>{year.focus.primaryCandidates.map((path) => pathLabels[path]).join('、')}</p>
                </section>
              ) : year.focus.state === 'leading' ? (
                <section className="wealth-v3-year__focus is-leading">
                  <h3>相对更值得关注的方向</h3>
                  <p>{pathLabels[year.focus.primaryCandidates[0]]}</p>
                </section>
              ) : null}
              {copy.income.length > 0 ? (
                <section className="wealth-year__income">
                  <h3>{usesMoneyStateCopy ? '这一年的资金变化' : '这一年的收入方向'}</h3>
                  {copy.income.map((item) => <AnnualParagraph item={item} key={item.text} />)}
                </section>
              ) : null}
              {copy.retention.length > 0 ? (
                <section className="wealth-v3-year__retention">
                  <h3>钱能不能留下</h3>
                  {copy.retention.map((item) => <AnnualParagraph item={item} key={item.text} />)}
                </section>
              ) : null}
              {copy.risk.length > 0 ? (
                <section className="wealth-v3-year__risk">
                  <h3>需要留意的一项</h3>
                  {copy.risk.map((item) => <AnnualParagraph item={item} key={item.text} />)}
                </section>
              ) : null}
              {copy.observations.length > 0 ? (
                <section className="wealth-v3-year__observations">
                  <h3>可以留意的现实情况</h3>
                  <ul>{copy.observations.map((item) => <AnnualListItem item={item} key={item.text} />)}</ul>
                </section>
              ) : null}
              {copy.actions.length > 0 ? (
                <section className="wealth-v3-year__actions">
                  <h3>可以先做这些事</h3>
                  <ol>{copy.actions.map((item) => <AnnualListItem item={item} key={item.text} />)}</ol>
                </section>
              ) : null}
              {year.comparison ? (
                <section className="wealth-v3-year__comparison">
                  <h3>和 {year.comparison.toYear} 年相比</h3>
                  <p>{year.comparison.reading.text}</p>
                </section>
              ) : null}
            </div>
          </article>
        })}
      </div>

      {report.content.route.length > 0 ? (
        <section className="report-reader__route wealth-v3-reader__route">
          <h2>接下来三年，可以这样安排</h2>
          <ol>{report.content.route.map((block) => <li key={block.id}>{block.text}</li>)}</ol>
        </section>
      ) : null}

      <p className="wealth-v3-reader__note">{report.content.readingNote.text}</p>

      <footer className="report-reader__footer">
        <small>生成于 {new Date(report.generatedAt).toLocaleDateString('zh-CN')}</small>
        <Link to="/reports">查看我的全部命书</Link>
      </footer>
    </main>
  )
}

function AnnualParagraph({ item }: { item: AnnualCopy }) {
  return <p><span>{item.text}</span><ApplicableYears years={item.appliesToYears} /></p>
}

function AnnualListItem({ item }: { item: AnnualCopy }) {
  return <li><span>{item.text}</span><ApplicableYears years={item.appliesToYears} /></li>
}

function ApplicableYears({ years }: { years: number[] }) {
  if (years.length < 2) return null
  return <small className="wealth-year__applies">适用于 {years.join('、')} 年</small>
}

function groupRepeatedAnnualCopy(years: WealthYearV3[], consolidate: boolean) {
  const income = groupCategory(years, (year) => year.income.map((block) => block.text), consolidate)
  const retention = groupCategory(years, (year) => [year.retention.text], consolidate)
  const risk = groupCategory(years, (year) => year.risk ? [year.risk.reading.text] : [], consolidate)
  const observations = groupCategory(years, (year) => year.observations.map((block) => block.text), consolidate)
  const actions = groupCategory(years, (year) => year.actions.map((block) => block.text), consolidate)
  return new Map(years.map((year) => [year.year, {
    income: income.get(year.year) ?? [],
    retention: retention.get(year.year) ?? [],
    risk: risk.get(year.year) ?? [],
    observations: observations.get(year.year) ?? [],
    actions: actions.get(year.year) ?? [],
  } satisfies AnnualCopySections]))
}

function groupCategory(
  years: WealthYearV3[],
  select: (year: WealthYearV3) => string[],
  consolidate: boolean,
) {
  if (!consolidate) {
    return new Map(years.map((year) => [year.year,
      select(year).flatMap(splitSentences).map((text) => ({ text, appliesToYears: [year.year] })),
    ]))
  }
  const occurrences = new Map<string, { firstYear: number; years: number[] }>()
  for (const year of years) {
    for (const text of new Set(select(year).flatMap(splitSentences))) {
      const existing = occurrences.get(text)
      if (existing) existing.years.push(year.year)
      else occurrences.set(text, { firstYear: year.year, years: [year.year] })
    }
  }
  const result = new Map<number, AnnualCopy[]>(years.map((year) => [year.year, []]))
  for (const [text, occurrence] of occurrences) {
    result.get(occurrence.firstYear)!.push({ text, appliesToYears: occurrence.years })
  }
  return result
}

function splitSentences(text: string) {
  return text.match(/[^。！？]+[。！？]?/g)?.map((sentence) => sentence.trim()).filter(Boolean) ?? []
}
