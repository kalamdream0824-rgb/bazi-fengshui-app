import { useMemo, useState } from 'react'
import { Card, CardTitle } from '@/components/Card'
import { EmptyState } from '@/components/EmptyState'
import { FooterNote } from '@/components/FooterNote'
import { SegControl } from '@/components/SegControl'
import { TopBar } from '@/components/TopBar'
import { EVENT_TYPES, pickDays, type DayEventType } from '@/lib/dayPicker'
import { useBaziStore } from '@/store/useBaziStore'

export function DayPickerPage() {
  const shengXiao = useBaziStore((s) => s.result?.shengXiao)
  const [eventType, setEventType] = useState<DayEventType>('marry')
  const current = EVENT_TYPES.find((e) => e.value === eventType)
  const results = useMemo(() => pickDays(eventType, 60, shengXiao), [eventType, shengXiao])
  const top = results.slice(0, 8)

  return (
    <>
      <TopBar title="择日" />

      <Card>
        <CardTitle hint="按事项匹配未来 60 天">选择事项</CardTitle>
        <SegControl
          options={EVENT_TYPES.map((e) => ({ label: e.label, value: e.value }))}
          value={eventType}
          onChange={(v) => setEventType(v as DayEventType)}
        />
        <div className="meta-line" style={{ padding: '0 16px 14px' }}>
          {current?.desc}
          {shengXiao ? ` · 已按生肖 ${shengXiao} 避冲` : ' · 排盘后可自动避冲命主生肖'}
        </div>
      </Card>

      {top.length === 0 ? (
        <EmptyState title="未来 60 天暂无完全匹配的日期" hint="可切换事项或延长时间范围（示例规则）" />
      ) : (
        <Card>
          <CardTitle hint="推荐参考 · 温和语气">推荐日期</CardTitle>
          {top.map((d) => (
            <div className="row" key={d.offset}>
              <div style={{ minWidth: 0 }}>
                <div style={{ fontWeight: 600 }}>
                  {d.dateText} {d.ganZhi}（{d.zhiXing}日）
                </div>
                <div className="meta-line" style={{ marginTop: 3 }}>{d.reasons.join('；')}</div>
                <div className="meta-line" style={{ marginTop: 3 }}>
                  宜：{d.yi.slice(0, 4).join('、')} 冲煞：{d.chongDesc} · 煞{d.sha}
                </div>
              </div>
              <span style={{ color: 'var(--jade)', fontWeight: 600, fontSize: 12 }}>推荐</span>
            </div>
          ))}
        </Card>
      )}

      <FooterNote>择日结果按传统黄历规则匹配，仅供传统文化研究参考</FooterNote>
    </>
  )
}
