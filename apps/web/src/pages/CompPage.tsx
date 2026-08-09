import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Button, ButtonRow } from '@/components/Button'
import { Card, CardTitle } from '@/components/Card'
import { EmptyState } from '@/components/EmptyState'
import { FooterNote } from '@/components/FooterNote'
import { SegControl } from '@/components/SegControl'
import { TopBar } from '@/components/TopBar'
import { computeCompatibility } from '@/lib/compRules'
import { WUXING_LABEL } from '@/lib/wuxing'
import { getBaziApi } from '@/services/baziApi'
import { useBaziStore } from '@/store/useBaziStore'
import { useToastStore } from '@/store/useToastStore'
import type { Gender, PaipanRequest, PaipanResult, WuxingKey } from '@/types/bazi'

const WUXING_KEYS: WuxingKey[] = ['jin', 'mu', 'shui', 'huo', 'tu']

function PillarsRow({ result, color }: { result: PaipanResult; color: string }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 3 }}>
      {(['year', 'month', 'day', 'time'] as const).map((key) => (
        <div
          key={key}
          style={{
            fontFamily: 'var(--font-display)',
            fontSize: 14,
            border: '1px solid var(--line)',
            borderRadius: 8,
            padding: '5px 0',
            background: '#faf5e8',
            color,
          }}
        >
          {result.pillars[key].gan}
          {result.pillars[key].zhi}
        </div>
      ))}
    </div>
  )
}

export function CompPage() {
  const toast = useToastStore((s) => s.show)
  const personA = useBaziStore((s) => s.result)
  const api = getBaziApi()

  const [bGender, setBGender] = useState<Gender>('female')
  const [bDatetime, setBDatetime] = useState('1996-03-18T10:00')
  const [personB, setPersonB] = useState<PaipanResult | null>(null)

  const mutation = useMutation({
    mutationFn: (req: PaipanRequest) => api.paipan(req),
    onSuccess: setPersonB,
    onError: (err: Error) => toast(err.message || '排盘失败，请重试'),
  })

  const comp = personA && personB ? computeCompatibility(personA, personB) : null

  return (
    <>
      <TopBar title="八字合婚" />

      {!personA ? (
        <>
          <EmptyState title="还没有命盘数据" hint="先去排一次盘" linkTo="/input" linkText="先去排一次盘" />
          <FooterNote>合婚结论以实际算法输出为准</FooterNote>
        </>
      ) : (
        <>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, margin: '4px 14px 12px' }}>
            <div className="card" style={{ margin: 0, textAlign: 'center', padding: '12px 8px' }}>
              <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--red)' }}>甲方 · {personA.shengXiao}</div>
              <div className="meta-line" style={{ margin: '3px 0 8px' }}>{personA.lunarText}</div>
              <PillarsRow result={personA} color="var(--red)" />
            </div>
            <div className="card" style={{ margin: 0, textAlign: 'center', padding: '12px 8px' }}>
              <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--water)' }}>
                {personB ? `乙方 · ${personB.shengXiao}` : '乙方'}
              </div>
              <div className="meta-line" style={{ margin: '3px 0 8px' }}>
                {personB ? personB.lunarText : '待排盘'}
              </div>
              {personB ? <PillarsRow result={personB} color="var(--water)" /> : <div style={{ color: 'var(--ink-3)', fontSize: 12, padding: '8px 0' }}>—</div>}
            </div>
          </div>

          <Card>
            <CardTitle hint="乙方信息">输入第二人</CardTitle>
            <div className="field">
              <div className="lbl">性别</div>
              <SegControl
                two
                options={[
                  { label: '男 · 乾造', value: 'male' },
                  { label: '女 · 坤造', value: 'female' },
                ]}
                value={bGender}
                onChange={(v) => setBGender(v as Gender)}
              />
            </div>
            <div className="field">
              <div className="lbl">公历出生日期与时间</div>
              <input
                className="input-box"
                type="datetime-local"
                value={bDatetime}
                onChange={(e) => setBDatetime(e.target.value)}
              />
            </div>
            <div style={{ padding: '0 16px 14px' }}>
              <Button block onClick={() => bDatetime && mutation.mutate({ gender: bGender, solarDateTime: bDatetime, trueSolarTime: false })} disabled={mutation.isPending}>
                {mutation.isPending ? '排盘中…' : '排乙方命盘'}
              </Button>
            </div>
          </Card>

          {comp && personB && (
            <>
              <div className="hero" style={{ textAlign: 'center', padding: 16 }}>
                <div style={{ fontFamily: 'var(--font-display)', fontSize: 38, fontWeight: 700, lineHeight: 1 }}>{comp.score}</div>
                <div style={{ fontSize: 12, letterSpacing: 3, marginTop: 6 }}>婚配指数 · {comp.grade}（参考规则）</div>
              </div>

              <Card>
                <CardTitle hint="本气计数">五行互补</CardTitle>
                <div style={{ padding: '0 16px 14px' }}>
                  <table style={{ width: '100%', fontSize: 12, borderCollapse: 'collapse' }}>
                    <tbody>
                      <tr>
                        <td style={{ border: '1px solid var(--line)', padding: '7px 2px', color: 'var(--red)' }}>甲方</td>
                        {WUXING_KEYS.map((k) => (
                          <td key={k} style={{ border: '1px solid var(--line)', padding: '7px 2px', textAlign: 'center' }}>{personA.wuXing[k]}</td>
                        ))}
                      </tr>
                      <tr>
                        <td style={{ border: '1px solid var(--line)', padding: '7px 2px', color: 'var(--water)' }}>乙方</td>
                        {WUXING_KEYS.map((k) => (
                          <td key={k} style={{ border: '1px solid var(--line)', padding: '7px 2px', textAlign: 'center' }}>{personB.wuXing[k]}</td>
                        ))}
                      </tr>
                      <tr>
                        <td style={{ border: '1px solid var(--line)', padding: '7px 2px', background: '#faf5e8' }}>五行</td>
                        {WUXING_KEYS.map((k) => (
                          <td key={k} style={{ border: '1px solid var(--line)', padding: '7px 2px', textAlign: 'center', background: '#faf5e8' }}>{WUXING_LABEL[k]}</td>
                        ))}
                      </tr>
                    </tbody>
                  </table>
                </div>
              </Card>

              <Card>
                <CardTitle hint="参考规则">冲合分析</CardTitle>
                <div className="row">
                  <span className="k">生肖关系</span>
                  <span style={{ color: comp.shengXiaoRelation === '六合' ? 'var(--jade)' : 'var(--ink-3)', fontWeight: 600, fontSize: 12 }}>
                    {comp.shengXiaoRelation === '六合' ? `六合（${personA.shengXiao}·${personB.shengXiao}）` : '无六合'}
                  </span>
                </div>
                <div className="row">
                  <span className="k">日主关系</span>
                  <span style={{ color: comp.dayMasterRelation === '相克' ? 'var(--red)' : 'var(--jade)', fontWeight: 600, fontSize: 12 }}>
                    {comp.dayMasterRelation}
                  </span>
                </div>
                <div className="row">
                  <span className="k">日柱纳音</span>
                  <span style={{ color: comp.nayinRelation === '相克' ? 'var(--red)' : 'var(--jade)', fontWeight: 600, fontSize: 12 }}>
                    {personA.pillars.day.naYin} · {personB.pillars.day.naYin}　{comp.nayinRelation}
                  </span>
                </div>
                <div className="row">
                  <span className="k">夫妻宫</span>
                  <span style={{ color: comp.spousePalaceRelation === '六冲' ? 'var(--red)' : 'var(--jade)', fontWeight: 600, fontSize: 12 }}>
                    {personA.pillars.day.zhi} · {personB.pillars.day.zhi}　{comp.spousePalaceRelation === '—' ? '无特殊' : comp.spousePalaceRelation}
                  </span>
                </div>
                <div className="row">
                  <span className="k">年柱关系</span>
                  <span style={{ color: comp.yearRelation === '六冲' ? 'var(--red)' : 'var(--jade)', fontWeight: 600, fontSize: 12 }}>
                    {comp.yearRelation === '—' && !comp.yearGanHe ? '无特殊' : `${comp.yearRelation === '—' ? '' : comp.yearRelation}${comp.yearGanHe ? '·年干五合' : ''}`}
                  </span>
                </div>
                {comp.tips.map((tip) => (
                  <div className="row" key={tip}>
                    <span className="k">{tip}</span>
                  </div>
                ))}
              </Card>

              <ButtonRow>
                <Button variant="primary" onClick={() => toast('合婚报告生成中…（示例）')}>生成合婚报告</Button>
                <Button onClick={() => setPersonB(null)}>重置</Button>
                <Button onClick={() => setBDatetime('')}>清空</Button>
              </ButtonRow>
            </>
          )}
          <FooterNote>合婚结论为示例规则 · 以实际算法输出为准</FooterNote>
        </>
      )}
    </>
  )
}
