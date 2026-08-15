import type { DaYun } from '@/types/bazi'

export function DaYunList({ daYun }: { daYun: DaYun[] }) {
  return (
    <div className="dayun-axis">
      {daYun.map((dy) => (
        <div key={dy.ganZhi} className={`dy ${dy.isCurrent ? 'current' : ''}`}>
          <span className="age">{dy.ageRange}</span>
          <span className="gz">{dy.ganZhi}</span>
          <span className="yr">{dy.yearRange}</span>
          <span className="meta">
            {[dy.shiShen, dy.naYin, dy.xunKong, dy.starFortune].filter(Boolean).join(' · ')}
          </span>
          {dy.isCurrent ? <span className="tag">今</span> : null}
        </div>
      ))}
    </div>
  )
}
