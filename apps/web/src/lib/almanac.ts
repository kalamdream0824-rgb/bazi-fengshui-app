import { Lunar } from 'lunar-javascript'

export interface AlmanacInfo {
  ganZhi: string
  yi: string[]
  ji: string[]
  chongDesc: string
  sha: string
  chongShengXiao: string
  zhiXing: string
  pengZu: string
  positionFu: string
}

export function getAlmanac(date: Date): AlmanacInfo {
  const lunar = Lunar.fromDate(date)
  return {
    ganZhi: lunar.getDayInGanZhiExact(),
    yi: lunar.getDayYi(),
    ji: lunar.getDayJi(),
    chongDesc: lunar.getDayChongDesc(),
    sha: lunar.getDaySha(),
    chongShengXiao: lunar.getDayChongShengXiao(),
    zhiXing: lunar.getZhiXing(),
    pengZu: `${lunar.getPengZuGan()} ${lunar.getPengZuZhi()}`.trim(),
    positionFu: lunar.getDayPositionFu(),
  }
}

export function getAlmanacFor(offsetDays: number): AlmanacInfo {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  return getAlmanac(d)
}
