import cityGeo from '@/data/cityGeo.json'

type CityGeo = Record<string, { lng: number; lat: number }>

const GEO = cityGeo as CityGeo

/** 省会/特别行政区经度兜底 */
const PROVINCE_LNG: Record<string, number> = {
  北京市: 116.41,
  天津市: 117.2,
  上海市: 121.47,
  重庆市: 106.55,
  河北省: 114.51,
  山西省: 112.55,
  内蒙古自治区: 111.75,
  辽宁省: 123.43,
  吉林省: 125.32,
  黑龙江省: 126.53,
  江苏省: 118.78,
  浙江省: 120.15,
  安徽省: 117.28,
  福建省: 119.3,
  江西省: 115.86,
  山东省: 117.12,
  河南省: 113.62,
  湖北省: 114.3,
  湖南省: 112.94,
  广东省: 113.27,
  广西壮族自治区: 108.37,
  海南省: 110.35,
  四川省: 104.07,
  贵州省: 106.63,
  云南省: 102.83,
  西藏自治区: 91.14,
  陕西省: 108.94,
  甘肃省: 103.83,
  青海省: 101.78,
  宁夏回族自治区: 106.23,
  新疆维吾尔自治区: 87.62,
  香港特别行政区: 114.17,
  澳门特别行政区: 113.55,
  台湾省: 121.56,
}

function normalizeCity(name: string): string {
  return name.replace(/(自治州|地区|自治盟|盟|市|州)$/, '')
}

function lookupCity(name: string): number | null {
  if (GEO[name]) return GEO[name].lng
  const key = normalizeCity(name)
  if (GEO[key]) return GEO[key].lng
  return null
}

/** 从「省 市」字符串解析经度；城市缺失时回退到省会经度 */
export function longitudeOf(birthPlace: string): number | null {
  const [province = '', city = ''] = birthPlace.split(' ')
  const cityLng = city ? lookupCity(city) : null
  if (cityLng != null) return cityLng
  return PROVINCE_LNG[province] ?? null
}

/** 均时差（分钟），NOAA 近似公式 */
export function equationOfTimeMinutes(date: Date): number {
  const start = new Date(date.getFullYear(), 0, 0)
  const dayOfYear = Math.floor((date.getTime() - start.getTime()) / 86400000)
  const b = (2 * Math.PI * (dayOfYear - 81)) / 364
  return 9.87 * Math.sin(2 * b) - 7.53 * Math.cos(b) - 1.5 * Math.sin(b)
}

export interface TrueSolarResult {
  adjusted: Date
  offsetMinutes: number
  eotMinutes: number
}

/** 真太阳时 = 北京时间 + 经度差校正 + 均时差 */
export function trueSolarTime(date: Date, longitude: number): TrueSolarResult {
  const offsetMinutes = (longitude - 120) * 4
  const eotMinutes = equationOfTimeMinutes(date)
  const total = offsetMinutes + eotMinutes
  return { adjusted: new Date(date.getTime() + total * 60000), offsetMinutes, eotMinutes }
}

export function formatAdjusted(date: Date): string {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}T${p(date.getHours())}:${p(date.getMinutes())}`
}
