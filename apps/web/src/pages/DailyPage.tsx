import { useState } from 'react'
import { Button } from '@/components/Button'
import { Card, CardTitle } from '@/components/Card'
import { FooterNote } from '@/components/FooterNote'
import { SegControl } from '@/components/SegControl'
import { TopBar } from '@/components/TopBar'
import { getAlmanacFor } from '@/lib/almanac'
import { getGanZhiFor } from '@/lib/baziMapper'
import { dailyFortune } from '@/lib/dailyFortune'
import { useBaziStore } from '@/store/useBaziStore'
import { useToastStore } from '@/store/useToastStore'

const TODAY = getGanZhiFor(0)
const TOMORROW = getGanZhiFor(1)
const ALM_TODAY = getAlmanacFor(0)
const ALM_TOMORROW = getAlmanacFor(1)

type DayKey = 'today' | 'tomorrow'

export function DailyPage() {
  const toast = useToastStore((s) => s.show)
  const result = useBaziStore((s) => s.result)
  const [day, setDay] = useState<DayKey>('today')

  const info = day === 'today' ? TODAY : TOMORROW
  const alm = day === 'today' ? ALM_TODAY : ALM_TOMORROW
  const fortune = dailyFortune(result, alm)

  return (
    <>
      <TopBar title="每日运势" />

      <div className="hero">
        <div className="date">{day === 'today' ? '今日' : '明日'} · {info.dateText}</div>
        <div className="gz">{info.ganZhi}</div>
        <SegControl
          two
          className="hero-seg"
          options={[
            { label: '今日', value: 'today' },
            { label: '明日', value: 'tomorrow' },
          ]}
          value={day}
          onChange={(v) => setDay(v as DayKey)}
        />
        <div style={{ fontSize: 12, letterSpacing: 2, marginBottom: 8 }}>
          {'★'.repeat(fortune.star)}
          {'☆'.repeat(5 - fortune.star)}
          <span style={{ letterSpacing: 1 }}>　{fortune.personalized ? '个性化运势' : '黄历参考'}</span>
        </div>
        <div style={{ fontSize: 12, lineHeight: 1.8, opacity: 0.95 }}>{fortune.summary}</div>
        {fortune.personalized ? null : (
          <div style={{ fontSize: 11, opacity: 0.8, marginTop: 6 }}>排盘后可生成个性化运势（仅供参考）</div>
        )}
      </div>

      <Card>
        <CardTitle hint="真实黄历">宜忌</CardTitle>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, padding: '4px 16px 14px' }}>
          <div>
            <div className="meta-line" style={{ marginBottom: 6 }}>宜</div>
            {alm.yi.slice(0, 5).map((t) => (
              <span key={t} className="chip" style={{ background: 'rgba(63,143,95,.10)', borderColor: 'rgba(63,143,95,.28)', color: 'var(--wood)', margin: '0 6px 6px 0' }}>{t}</span>
            ))}
          </div>
          <div>
            <div className="meta-line" style={{ marginBottom: 6 }}>忌</div>
            {alm.ji.slice(0, 3).map((t) => (
              <span key={t} className="chip no" style={{ background: 'rgba(176,58,46,.08)', borderColor: 'rgba(176,58,46,.25)', color: 'var(--red)', margin: '0 6px 6px 0' }}>{t}</span>
            ))}
          </div>
        </div>
      </Card>

      <Card>
        <CardTitle hint="真实推算">今日指引</CardTitle>
        <div style={{ padding: '0 16px 14px', fontSize: 12.5, color: 'var(--ink-2)', lineHeight: 2 }}>
          幸运色　<b style={{ color: 'var(--ink)' }}>{fortune.luckyColor}</b>
          <br />
          幸运方位　<b style={{ color: 'var(--ink)' }}>{fortune.direction}</b>
          <br />
          冲煞　<b style={{ color: 'var(--ink)' }}>{alm.chongDesc} · 煞{alm.sha}</b>
          <br />
          注意事项　<b style={{ color: 'var(--ink)' }}>{fortune.tip || '—'}</b>
        </div>
      </Card>

      <Card>
        <CardTitle hint="干支为真实推算">明日预告</CardTitle>
        <div className="row">
          <span className="k">{TOMORROW.dateText}</span>
          <span style={{ fontFamily: 'var(--font-display)', fontSize: 17, fontWeight: 700 }}>{TOMORROW.ganZhi.split('日')[0]}日</span>
        </div>
      </Card>

      <div style={{ padding: '0 14px 12px' }}>
        <Button variant="primary" block onClick={() => toast('分享功能规划中')}>分享今日运势</Button>
      </div>
      <FooterNote>运势内容仅供传统文化研究参考</FooterNote>
    </>
  )
}
