import type { WuxingKey } from '@/types/bazi'

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
