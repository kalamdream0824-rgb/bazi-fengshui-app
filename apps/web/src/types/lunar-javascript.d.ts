declare module 'lunar-javascript' {
  export class Solar {
    static fromYmdHms(year: number, month: number, day: number, hour: number, minute: number, second: number): Solar
    static fromDate(date: Date): Solar
    getLunar(): Lunar
    toYmdHms(): string
    getYear(): number
    getMonth(): number
    getDay(): number
    getHour(): number
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
    getYearShiShenZhi(): string[]
    getYearNaYin(): string
    getYearDiShi(): string
    getYearXunKong(): string
    getMonthGan(): string
    getMonthZhi(): string
    getMonthHideGan(): string[]
    getMonthShiShenGan(): string
    getMonthShiShenZhi(): string[]
    getMonthNaYin(): string
    getMonthDiShi(): string
    getMonthXunKong(): string
    getDayGan(): string
    getDayZhi(): string
    getDayHideGan(): string[]
    getDayShiShenGan(): string
    getDayShiShenZhi(): string[]
    getDayNaYin(): string
    getDayDiShi(): string
    getDayXunKong(): string
    getTimeGan(): string
    getTimeZhi(): string
    getTimeHideGan(): string[]
    getTimeShiShenGan(): string
    getTimeShiShenZhi(): string[]
    getTimeNaYin(): string
    getTimeDiShi(): string
    getTimeXunKong(): string
    getTaiYuan(): string
    getTaiYuanNaYin(): string
    getMingGong(): string
    getMingGongNaYin(): string
    getShenGong(): string
    getShenGongNaYin(): string
    getYun(gender: number): Yun
  }

  export class LunarUtil {
    static SHI_SHEN: Record<string, string>
    static NAYIN: Record<string, string>
    static JIA_ZI: string[]
  }

  export class Yun {
    getStartYear(): number
    getStartMonth(): number
    getStartDay(): number
    getStartHour(): number
    isForward(): boolean
    getStartSolar(): Solar
    getDaYun(): DaYun[]
    getDaYun(n: number): DaYun[]
  }

  export class DaYun {
    getStartAge(): number
    getStartYear(): number
    getEndAge(): number
    getEndYear(): number
    getIndex(): number
    getGanZhi(): string
    getXunKong(): string
    getLiuNian(n: number): LiuNian[]
  }

  export class LiuNian {
    getYear(): number
    getAge(): number
    getGanZhi(): string
    getXunKong(): string
  }
}
