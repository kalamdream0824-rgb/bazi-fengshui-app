import { useMemo, useState } from 'react'
import { Solar } from 'lunar-javascript'

interface BaziRow {
  label: string
  gan: string
  zhi: string
  shiShen: string
  naYin: string
}

function toLocalInputValue(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function computeBazi(value: string) {
  const [date, time] = value.split('T')
  const [y, m, d] = date.split('-').map(Number)
  const [h, min] = time.split(':').map(Number)
  const solar = Solar.fromYmdHms(y, m, d, h, min, 0)
  const lunar = solar.getLunar()
  const ec = lunar.getEightChar()
  const rows: BaziRow[] = [
    { label: '年柱', gan: ec.getYearGan(), zhi: ec.getYearZhi(), shiShen: ec.getYearShiShenGan(), naYin: ec.getYearNaYin() },
    { label: '月柱', gan: ec.getMonthGan(), zhi: ec.getMonthZhi(), shiShen: ec.getMonthShiShenGan(), naYin: ec.getMonthNaYin() },
    { label: '日柱', gan: ec.getDayGan(), zhi: ec.getDayZhi(), shiShen: '日主', naYin: ec.getDayNaYin() },
    { label: '时柱', gan: ec.getTimeGan(), zhi: ec.getTimeZhi(), shiShen: ec.getTimeShiShenGan(), naYin: ec.getTimeNaYin() },
  ]
  return { lunarText: lunar.toString(), shengXiao: lunar.getYearShengXiao(), rows }
}

export default function App() {
  const [birth, setBirth] = useState(toLocalInputValue(new Date()))
  const result = useMemo(() => computeBazi(birth), [birth])

  return (
    <main className="app">
      <header>
        <h1>八字命理排盘</h1>
        <p className="subtitle">基于 lunar-javascript 的演示版，后续可扩展大运、流年、合婚等功能</p>
      </header>

      <section className="card">
        <label htmlFor="birth">出生日期时间</label>
        <input
          id="birth"
          type="datetime-local"
          value={birth}
          onChange={(e) => setBirth(e.target.value)}
        />
      </section>

      <section className="card">
        <div className="meta">
          <span>{result.lunarText}</span>
          <span className="badge">生肖：{result.shengXiao}</span>
        </div>
        <table>
          <thead>
            <tr>
              <th>柱</th>
              <th>天干</th>
              <th>地支</th>
              <th>十神</th>
              <th>纳音</th>
            </tr>
          </thead>
          <tbody>
            {result.rows.map((row) => (
              <tr key={row.label}>
                <td>{row.label}</td>
                <td className="gan">{row.gan}</td>
                <td className="zhi">{row.zhi}</td>
                <td>{row.shiShen}</td>
                <td>{row.naYin}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </section>
    </main>
  )
}
