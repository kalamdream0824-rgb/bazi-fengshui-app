import { useState } from 'react'
import { Button } from '@/components/Button'
import { Card, CardTitle } from '@/components/Card'
import { SegControl } from '@/components/SegControl'
import { TopBar } from '@/components/TopBar'
import { getGanZhiFor } from '@/lib/baziMapper'
import { useToastStore } from '@/store/useToastStore'

const TODAY = getGanZhiFor(0)
const TOMORROW = getGanZhiFor(1)

export function DailyPage() {
  const toast = useToastStore((s) => s.show)
  const [day, setDay] = useState<'today' | 'tomorrow'>('today')
  const info = day === 'today' ? TODAY : TOMORROW

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
          onChange={(v) => setDay(v as 'today' | 'tomorrow')}
        />
        <div style={{ fontSize: 12, letterSpacing: 2, marginBottom: 8 }}>★★★☆☆　运势三星（示例）</div>
        <div style={{ fontSize: 12, lineHeight: 1.8, opacity: 0.95 }}>
          {day === 'today'
            ? '今日宜稳不宜急，适合处理积压事务，忌冲动决策。'
            : '明日运势平稳，适合社交与出行，注意劳逸结合。'}
        </div>
      </div>

      <Card>
        <CardTitle hint="示例">今日宜忌</CardTitle>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, padding: '4px 16px 14px' }}>
          <div>
            <div className="meta-line" style={{ marginBottom: 6 }}>宜</div>
            {['祭祀', '出行', '会友'].map((t) => (
              <span key={t} className="chip" style={{ background: 'rgba(63,143,95,.10)', borderColor: 'rgba(63,143,95,.28)', color: 'var(--wood)', margin: '0 6px 6px 0' }}>{t}</span>
            ))}
          </div>
          <div>
            <div className="meta-line" style={{ marginBottom: 6 }}>忌</div>
            {['动土', '安葬'].map((t) => (
              <span key={t} className="chip no" style={{ background: 'rgba(176,58,46,.08)', borderColor: 'rgba(176,58,46,.25)', color: 'var(--red)', margin: '0 6px 6px 0' }}>{t}</span>
            ))}
          </div>
        </div>
      </Card>

      <Card>
        <CardTitle hint="示例">今日指引</CardTitle>
        <div style={{ padding: '0 16px 14px', fontSize: 12.5, color: 'var(--ink-2)', lineHeight: 2 }}>
          幸运色　<b style={{ color: 'var(--ink)' }}>青 / 蓝</b>
          <br />
          幸运方位　<b style={{ color: 'var(--ink)' }}>东南</b>
          <br />
          贵人属相　<b style={{ color: 'var(--ink)' }}>兔 · 羊</b>
          <br />
          注意事项　<b style={{ color: 'var(--ink)' }}>午时（11-13 点）避免重大签约</b>
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
      <div className="footer-note">运势内容为示例数据 · 以实际算法输出为准</div>
    </>
  )
}
