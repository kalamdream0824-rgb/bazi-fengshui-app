import { useState } from 'react'
import type { Pillar, PillarKey } from '@/types/bazi'

const TIPS: Record<string, string> = {
  主星: '十神是日主与其他干支关系的称谓：生我、克我、我生、我克、同我，各分正偏共十种。',
  藏干: '地支内藏天干，代表气机层次，如申中藏庚金、壬水、戊土。',
  纳音: '纳音是六十甲子配五行的古法称谓，用于象意参考。',
  星运: '十二长生描述五行能量在十二地支中的旺衰状态，如长生、帝旺、墓、绝。',
  神煞: '神煞源自星命术的吉凶星曜体系，需结合全局论断。',
}

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
                      k === 'day' ? undefined : () => setTip({ title: pillars[k].shiShen, text: TIPS['主星'] })
                    }
                  >
                    {pillars[k].shiShen}
                  </span>
                </td>
              ))}
            </tr>
            <tr>
              <td className="label">天干</td>
              {KEYS.map((k) => (
                <td key={k}>
                  <span className={`gz ${k === 'day' ? 'day' : ''}`}>{pillars[k].gan}</span>
                </td>
              ))}
            </tr>
            <tr>
              <td className="label">地支</td>
              {KEYS.map((k) => (
                <td key={k}>
                  <span className={`gz ${k === 'day' ? 'day' : ''}`}>{pillars[k].zhi}</span>
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
                  <span className="shensha">{pillars[k].shenSha.length ? pillars[k].shenSha.join(' ') : '—'}</span>
                </td>
              ))}
            </tr>
          </tbody>
        </table>
      </div>
      <div className="hint">点击 十神 / 藏干 / 纳音 / 星运 查看释义</div>

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
