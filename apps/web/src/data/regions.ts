import pc from 'china-division/dist/pc.json'
import hkMtTw from 'china-division/dist/HK-MO-TW.json'

export interface Region {
  province: string
  cities: string[]
}

function buildRegions(): Region[] {
  const list: Region[] = Object.entries(pc).map(([province, cities]) => ({
    province,
    cities: cities as string[],
  }))

  // 港澳台：以次级区域/市县作为“市”级选项
  for (const [province, regionMap] of Object.entries(hkMtTw)) {
    list.push({ province, cities: Object.keys(regionMap as Record<string, unknown>) })
  }

  return list
}

export const REGIONS: Region[] = buildRegions()
