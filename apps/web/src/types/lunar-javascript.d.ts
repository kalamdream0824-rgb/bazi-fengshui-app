declare module 'lunar-javascript' {
  export class Solar {
    static fromYmdHms(year: number, month: number, day: number, hour: number, minute: number, second: number): Solar
    static fromDate(date: Date): Solar
    getLunar(): Lunar
    toYmdHms(): string
  }

  export class Lunar {
    static fromDate(date: Date): Lunar
    next(days: number): Lunar
    getEightChar(): EightChar
    getYearShengXiao(): string
    getTimeZhi(): string
    getYearInGanZhiExact(): string
    getMonthInGanZhiExact(): string
    getDayInGanZhiExact(): string
    getDayYi(): string[]
    getDayJi(): string[]
    getDayChongDesc(): string
    getDaySha(): string
    getDayChongShengXiao(): string
    getZhiXing(): string
    getPengZuGan(): string
    getPengZuZhi(): string
    getDayPositionFu(): string
    toString(): string
  }

  export class EightChar {
    getYear(): string
    getMonth(): string
    getDay(): string
    getTime(): string
    getYearGan(): string
    getYearZhi(): string
    getYearHideGan(): string[]
    getYearShiShenGan(): string
    getYearNaYin(): string
    getYearDiShi(): string
    getYearXunKong(): string
    getMonthGan(): string
    getMonthZhi(): string
    getMonthHideGan(): string[]
    getMonthShiShenGan(): string
    getMonthNaYin(): string
    getMonthDiShi(): string
    getMonthXunKong(): string
    getDayGan(): string
    getDayZhi(): string
    getDayHideGan(): string[]
    getDayShiShenGan(): string
    getDayNaYin(): string
    getDayDiShi(): string
    getDayXunKong(): string
    getTimeGan(): string
    getTimeZhi(): string
    getTimeHideGan(): string[]
    getTimeShiShenGan(): string
    getTimeNaYin(): string
    getTimeDiShi(): string
    getTimeXunKong(): string
    getTaiYuan(): string
    getShenGong(): string
    getYun(gender: number): Yun
  }

  export class Yun {
    getDaYun(): DaYun[]
  }

  export class DaYun {
    getStartAge(): number
    getStartYear(): number
    getGanZhi(): string
  }
}
