import { useState } from 'react'
import { Navigate } from 'react-router-dom'
import { Card, CardTitle } from '@/components/Card'
import { DaYunList } from '@/components/DaYunList'
import { ExplainCard } from '@/components/ExplainCard'
import { FooterNote } from '@/components/FooterNote'
import { SegControl } from '@/components/SegControl'
import { TopBar } from '@/components/TopBar'
import { GAN_WUXING, WUXING_LABEL, computeWuxingDetail } from '@/lib/wuxing'
import { useBaziWithFallback } from '@/hooks/useBaziWithFallback'
import { explain } from '@/lib/explainer'
import { computeGeJu, computeWangShuai } from '@/lib/geJu'

type Panel = 'yun' | 'liu' | 'ss' | 'geju' | 'wuxing' | 'shensha'

export function ProPage() {
  const { result } = useBaziWithFallback()
  const [panel, setPanel] = useState<Panel>('yun')

  if (!result) {
    return <Navigate to="/input" replace />
  }

  const currentDaYun = result.daYun.find((d) => d.isCurrent)
  const explanation = explain(result)
  const wuxingDetail = computeWuxingDetail(result)
  const geJu = computeGeJu(result)
  const wangShuai = computeWangShuai(result)
  const startAge = result.daYun[0] ? Number.parseInt(result.daYun[0].ageRange.split(' - ')[0], 10) : 0
  const nowYear = new Date().getFullYear()

  return (
    <>
      <TopBar title="专业细盘" />

      <div
        style={{
          margin: '4px 14px 10px',
          fontSize: 12,
          color: 'var(--ink-3)',
          display: 'flex',
          justifyContent: 'space-between',
        }}
      >
        <span>
          当前大运 <b style={{ color: 'var(--ink)' }}>{currentDaYun?.ganZhi ?? '—'}</b>
        </span>
        <span>{result.currentYearGanZhi}</span>
      </div>

      <SegControl
        className="six"
        options={[
          { label: '大运', value: 'yun' },
          { label: '流年', value: 'liu' },
          { label: '十神', value: 'ss' },
          { label: '格局', value: 'geju' },
          { label: '五行', value: 'wuxing' },
          { label: '神煞', value: 'shensha' },
        ]}
        value={panel}
        onChange={(v) => setPanel(v as Panel)}
      />

      {panel === 'yun' && (
        <>
          {result.yunStart && (
            <Card>
              <CardTitle hint="公历 · 依阴阳年顺逆">起运</CardTitle>
              <div className="row">
                <span className="k">起运时间</span>
                <span className="v">
                  {result.yunStart.year}年{result.yunStart.month}月{result.yunStart.day}日 {result.yunStart.hour}时
                </span>
              </div>
              <div className="row">
                <span className="k">大运顺逆</span>
                <span className="v">{result.yunStart.forward ? '顺行' : '逆行'}</span>
              </div>
              {startAge > 0 && (
                <div className="row">
                  <span className="k">起运岁数</span>
                  <span className="v">{startAge} 岁</span>
                </div>
              )}
            </Card>
          )}
          <ExplainCard explanation={explanation} blockKey="dayun" />
          <Card>
            <CardTitle hint="十年一运">大运</CardTitle>
            <DaYunList daYun={result.daYun} />
          </Card>
        </>
      )}

      {panel === 'liu' && (
        <>
          <ExplainCard explanation={explanation} blockKey="liunian" />
          <Card>
            <CardTitle hint="起运后逐年 · 对照原局">流年</CardTitle>
            {result.liuNianList && result.liuNianList.length > 0 ? (
              result.liuNianList.map((ln) => (
                <div className={`row ${ln.year === nowYear ? 'current' : ''}`} key={ln.year}>
                  <span className="k">
                    {ln.year} · {ln.age} 岁
                  </span>
                  <span className="gz">{ln.ganZhi}</span>
                  <span className="v">
                    {[ln.shiShen, ln.naYin, ln.starFortune, ln.xunKong ? `空${ln.xunKong}` : '', ln.shenSha.join('·')]
                      .filter(Boolean)
                      .join(' · ')}
                  </span>
                </div>
              ))
            ) : (
              <div className="empty">暂无流年数据</div>
            )}
          </Card>
          <Card>
            <CardTitle hint="时柱起 · 起运后逐年">小运</CardTitle>
            {result.xiaoYunList && result.xiaoYunList.length > 0 ? (
              result.xiaoYunList.map((xy) => (
                <div className={`row ${xy.year === nowYear ? 'current' : ''}`} key={xy.year}>
                  <span className="k">
                    {xy.year} · {xy.age} 岁
                  </span>
                  <span className="gz">{xy.ganZhi}</span>
                  <span className="v">{[xy.shiShen, xy.naYin, xy.shenSha.join('·')].filter(Boolean).join(' · ')}</span>
                </div>
              ))
            ) : (
              <div className="empty">暂无小运数据</div>
            )}
          </Card>
          <Card>
            <CardTitle hint={`今年（${nowYear}）逐月 · 节气月`}>流月</CardTitle>
            {result.currentYearLiuYue && result.currentYearLiuYue.length > 0 ? (
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(4, 1fr)',
                  gap: 8,
                  padding: '4px 16px 16px',
                }}
              >
                {result.currentYearLiuYue.map((ly) => (
                  <div
                    key={ly.index}
                    style={{
                      textAlign: 'center',
                      border: '1px solid var(--line)',
                      borderRadius: 10,
                      padding: '8px 2px',
                      background: '#faf5e8',
                    }}
                  >
                    <div className="meta-line">{ly.monthName}</div>
                    <div style={{ fontFamily: 'var(--font-display)', fontSize: 15, fontWeight: 700, marginTop: 4 }}>
                      {ly.ganZhi}
                    </div>
                    <div className="meta-line" style={{ marginTop: 4 }}>
                      {ly.shiShen} · {ly.naYin}
                    </div>
                    <div className="meta-line">{ly.shenSha.length > 0 ? ly.shenSha.join('·') : ''}</div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="empty">暂无流月数据</div>
            )}
          </Card>
        </>
      )}

      {panel === 'ss' && (
        <>
          <ExplainCard explanation={explanation} blockKey="shishen" />
          <Card>
            <CardTitle hint="主星（天干）+ 副星（藏干）">十神分布</CardTitle>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 8, padding: '4px 16px 16px' }}>
              {(['year', 'month', 'day', 'time'] as const).map((key) => {
                const p = result.pillars[key]
                return (
                  <div
                    key={key}
                    style={{
                      textAlign: 'center',
                      border: '1px solid var(--line)',
                      borderRadius: 10,
                      padding: '8px 2px',
                      background: '#faf5e8',
                    }}
                  >
                    <div className="meta-line">
                      {key === 'year' ? '年' : key === 'month' ? '月' : key === 'day' ? '日' : '时'}柱 · {p.gan}
                      {p.zhi}
                    </div>
                    <div style={{ fontFamily: 'var(--font-display)', fontSize: 15, fontWeight: 700, marginTop: 4 }}>
                      {p.shiShen}
                    </div>
                    <div className="meta-line" style={{ marginTop: 4 }}>
                      {WUXING_LABEL[GAN_WUXING[p.gan]]} · {p.ziZuo}
                    </div>
                    <div style={{ marginTop: 6, fontSize: 11, color: 'var(--ink-3)', lineHeight: 1.6 }}>
                      {p.hideGan.map((h) => (
                        <div key={h.gan}>
                          {h.gan} · {h.shiShen}
                        </div>
                      ))}
                    </div>
                  </div>
                )
              })}
            </div>
          </Card>
        </>
      )}

      {panel === 'wuxing' && (
        <>
          <Card>
            <CardTitle hint="本气计数 + 含藏干加权（本气1 / 中气0.5 / 余气0.3）">五行明细</CardTitle>
            {wuxingDetail.map((d) => (
              <div className={`row ${d.missing ? 'missing' : ''}`} key={d.key}>
                <span className="k">{d.label}</span>
                <span className="v">
                  本气 {d.stemCount} · 含藏干 {d.weightedCount}
                  {d.missing ? '（缺失）' : ''}
                </span>
              </div>
            ))}
          </Card>
        </>
      )}

      {panel === 'geju' && (
        <>
          <Card>
            <CardTitle hint="月支藏干透干优先，不透取本气 · 参考口径">月令格局</CardTitle>
            <div className="row">
              <span className="k">月柱</span>
              <span className="v">{geJu.monthGanZhi}</span>
            </div>
            <div className="row">
              <span className="k">取格</span>
              <span className="v">
                {geJu.trans ? `藏干 ${geJu.gan}（${geJu.shiShen}）透出取格` : `本气 ${geJu.gan}（${geJu.shiShen}）`}
              </span>
            </div>
            <div className="row">
              <span className="k">格局</span>
              <span className="v" style={{ fontWeight: 700 }}>
                {geJu.name}
              </span>
            </div>
          </Card>
          <Card>
            <CardTitle hint={`得分 ${wangShuai.score} · 得令+2 / 支生扶+1 / 干比劫+1印+0.5 · 参考口径`}>
              日主旺衰粗判 · {wangShuai.level}
            </CardTitle>
            {wangShuai.items.map((item) => (
              <div className="row" key={item.label}>
                <span className="k">{item.label}</span>
                <span className="v">
                  {item.score > 0 ? `+${item.score}` : item.score} · {item.note}
                </span>
              </div>
            ))}
          </Card>
        </>
      )}

      {panel === 'shensha' && (
        <>
          <ExplainCard explanation={explanation} blockKey="shensha" />
          <Card>
            <CardTitle hint="大运/流年干支对照原局 · 口径见设计文档">
              流年神煞（2026 · {result.currentLiuNian?.ganZhi ?? '—'}）
            </CardTitle>
            <div style={{ padding: '0 16px 14px', fontSize: 13 }}>
              {result.currentLiuNian?.shenSha.join('·') || '—'}
            </div>
          </Card>
          <Card>
            <CardTitle hint="大运干支对照原局">大运神煞</CardTitle>
            {result.daYun.map((d) => (
              <div className="row" key={d.ganZhi}>
                <span className="k">{d.ganZhi}</span>
                <span className="v">{d.shenSha.join('·') || '—'}</span>
              </div>
            ))}
          </Card>
        </>
      )}

      <FooterNote>大运流年内容以实际算法输出为准</FooterNote>
    </>
  )
}
