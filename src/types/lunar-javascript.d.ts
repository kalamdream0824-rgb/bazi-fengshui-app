declare module 'lunar-javascript' {
  export class Solar {
    static fromYmdHms(year: number, month: number, day: number, hour: number, minute: number, second: number): Solar
    getLunar(): Lunar
  }

  export class Lunar {
    getEightChar(): EightChar
    getYearShengXiao(): string
    toString(): string
  }

  export class EightChar {
    getYearGan(): string
    getYearZhi(): string
    getYearShiShenGan(): string
    getYearNaYin(): string
    getMonthGan(): string
    getMonthZhi(): string
    getMonthShiShenGan(): string
    getMonthNaYin(): string
    getDayGan(): string
    getDayZhi(): string
    getDayNaYin(): string
    getTimeGan(): string
    getTimeZhi(): string
    getTimeShiShenGan(): string
    getTimeNaYin(): string
  }
}
