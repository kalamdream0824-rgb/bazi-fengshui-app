import { WUXING_LABEL } from '@/lib/wuxing'
import type { WuxingKey } from '@/types/bazi'

const ORDER: WuxingKey[] = ['jin', 'mu', 'shui', 'huo', 'tu']

export function WuxingBar({ wuXing }: { wuXing: Record<WuxingKey, number> }) {
  const max = Math.max(...ORDER.map((k) => wuXing[k]), 1)
  return (
    <div className="wuxing">
      {ORDER.map((key) => (
        <div key={key} className="wx">
          <div className="bar">
            <i className={`wuxing-${key}`} style={{ width: `${(wuXing[key] / max) * 100}%`, background: 'currentColor' }} />
          </div>
          <div className={`char wuxing-${key}`}>{WUXING_LABEL[key]}</div>
          <div className="n">{wuXing[key]}</div>
        </div>
      ))}
    </div>
  )
}
