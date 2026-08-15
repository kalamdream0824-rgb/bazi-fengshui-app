import type { PaipanResult, PillarKey, WuxingKey } from '@/types/bazi'

export const GAN_WUXING: Record<string, WuxingKey> = {
  甲: 'mu',
  乙: 'mu',
  丙: 'huo',
  丁: 'huo',
  戊: 'tu',
  己: 'tu',
  庚: 'jin',
  辛: 'jin',
  壬: 'shui',
  癸: 'shui',
}

export const ZHI_WUXING: Record<string, WuxingKey> = {
  子: 'shui',
  丑: 'tu',
  寅: 'mu',
  卯: 'mu',
  辰: 'tu',
  巳: 'huo',
  午: 'huo',
  未: 'tu',
  申: 'jin',
  酉: 'jin',
  戌: 'tu',
  亥: 'shui',
}

export const WUXING_LABEL: Record<WuxingKey, string> = {
  jin: '金',
  mu: '木',
  shui: '水',
  huo: '火',
  tu: '土',
}

export interface WuxingDetail {
  key: WuxingKey
  label: string
  /** 本气计数（天干 + 地支本气） */
  stemCount: number
  /** 含藏干加权计数（本气 1 / 中气 0.5 / 余气 0.3，口径公示） */
  weightedCount: number
  /** 缺失（本气计数为 0 的五行） */
  missing: boolean
}

const HIDE_GAN_WEIGHT = [1, 0.5, 0.3]

/**
 * 五行明细：本气计数 + 含藏干加权计数。
 * 藏干权重口径：本气 1.0、中气 0.5、余气 0.3（传统通用计权，仅作展示参考）。
 */
export function computeWuxingDetail(result: PaipanResult): WuxingDetail[] {
  const stemCount: Record<WuxingKey, number> = { jin: 0, mu: 0, shui: 0, huo: 0, tu: 0 }
  const weightedCount: Record<WuxingKey, number> = { jin: 0, mu: 0, shui: 0, huo: 0, tu: 0 }

  for (const key of ['year', 'month', 'day', 'time'] as PillarKey[]) {
    const p = result.pillars[key]
    stemCount[GAN_WUXING[p.gan]] += 1
    stemCount[ZHI_WUXING[p.zhi]] += 1
    weightedCount[GAN_WUXING[p.gan]] += 1
    p.hideGan.forEach((hg, i) => {
      weightedCount[hg.wuxing] += HIDE_GAN_WEIGHT[Math.min(i, HIDE_GAN_WEIGHT.length - 1)]
    })
  }

  return (Object.keys(WUXING_LABEL) as WuxingKey[]).map((key) => ({
    key,
    label: WUXING_LABEL[key],
    stemCount: stemCount[key],
    weightedCount: Math.round(weightedCount[key] * 10) / 10,
    missing: stemCount[key] === 0,
  }))
}
