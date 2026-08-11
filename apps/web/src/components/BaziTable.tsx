import { useState } from 'react'
import type { Pillar, PillarKey } from '@/types/bazi'
import { SHISHEN_TIP, TERM_TIPS } from '@/lib/explainer'

const TIPS = TERM_TIPS

const KEYS: PillarKey[] = ['year', 'month', 'day', 'time']
const LABELS: Record<PillarKey, string> = { year: '年柱', month: '月柱', day: '日柱', time: '时柱' }

interface BaziTableProps {
  pillars: Record<PillarKey, Pillar>
}

export function BaziTable({ pillars }: BaziTableProps) {
  const [tip, setTip] = useState<{ title: string; text: string } | null>(null)

  return (
    <>
      <div className="table-wrap">
        <table className="bazi-table">
          <thead>
            <tr>
              <th>四柱</th>
              {KEYS.map((k) => (
                <th key={k} className={k === 'day' ? 'day' : ''}>
                  {LABELS[k]}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            <tr>
              <td className="label tipable" onClick={() => setTip({ title: '主星', text: TIPS['主星'] })}>
                主星
              </td>
              {KEYS.map((k) => (
                <td key={k}>
                  <span
                    className={`ss ${k === 'day' ? 'owner' : 'tipable'}`}
                    onClick={
                      k === 'day'
                        ? undefined
                        : () => setTip({ title: pillars[k].shiShen, text: SHISHEN_TIP[pillars[k].shiShen] ?? TIPS['主星'] })
                    }
                  >
                    {pillars[k].shiShen}
                  </span>
                </td>
              ))}
            </tr>
            <tr>
              <td className="label tipable" onClick={() => setTip({ title: '天干', text: TIPS['天干'] })}>
                天干
              </td>
              {KEYS.map((k) => (
                <td key={k}>
                  <span
                    className={`gz tipable ${k === 'day' ? 'day' : ''}`}
                    onClick={() => setTip({ title: pillars[k].gan, text: TIPS['天干'] })}
                  >
                    {pillars[k].gan}
                  </span>
                </td>
              ))}
            </tr>
            <tr>
              <td className="label tipable" onClick={() => setTip({ title: '地支', text: TIPS['地支'] })}>
                地支
              </td>
              {KEYS.map((k) => (
                <td key={k}>
                  <span
                    className={`gz tipable ${k === 'day' ? 'day' : ''}`}
                    onClick={() => setTip({ title: pillars[k].zhi, text: TIPS['地支'] })}
                  >
                    {pillars[k].zhi}
                  </span>
                </td>
              ))}
            </tr>
            <tr>
              <td className="label tipable" onClick={() => setTip({ title: '藏干', text: TIPS['藏干'] })}>
                藏干
              </td>
              {KEYS.map((k) => (
                <td key={k} className="hidegan">
                  {pillars[k].hideGan.map((hg) => (
                    <span
                      key={hg.gan}
                      className={`tipable wuxing-${hg.wuxing}`}
                      onClick={() => setTip({ title: hg.gan, text: `${hg.gan}（${hg.wuxing}）· ${TIPS['藏干']}` })}
                    >
                      {hg.gan}
                    </span>
                  ))}
                </td>
              ))}
            </tr>
            <tr>
              <td className="label tipable" onClick={() => setTip({ title: '纳音', text: TIPS['纳音'] })}>
                纳音
              </td>
              {KEYS.map((k) => (
                <td key={k}>
                  <span className="ny tipable" onClick={() => setTip({ title: pillars[k].naYin, text: TIPS['纳音'] })}>
                    {pillars[k].naYin}
                  </span>
                </td>
              ))}
            </tr>
            <tr>
              <td className="label tipable" onClick={() => setTip({ title: '星运', text: TIPS['星运'] })}>
                星运
              </td>
              {KEYS.map((k) => (
                <td key={k}>
                  <span className="xy tipable" onClick={() => setTip({ title: pillars[k].diShi, text: TIPS['星运'] })}>
                    {pillars[k].diShi}
                  </span>
                </td>
              ))}
            </tr>
            <tr>
              <td className="label tipable" onClick={() => setTip({ title: '神煞', text: TIPS['神煞'] })}>
                神煞
              </td>
              {KEYS.map((k) => (
                <td key={k}>
                  <span className="shensha">{pillars[k].shenSha.length ? pillars[k].shenSha.join('·') : '—'}</span>
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>
      <div className="hint">点击 十神 / 天干 / 地支 / 藏干 / 纳音 / 星运 查看释义</div>

      <div className={`sheet ${tip ? 'show' : ''}`}>
        <button className="close" aria-label="关闭" onClick={() => setTip(null)}>
          ×
        </button>
        <div className="st">{tip?.title ?? '释义'}</div>
        <div className="txt">{tip?.text ?? ''}</div>
      </div>
    </>
  )
}
