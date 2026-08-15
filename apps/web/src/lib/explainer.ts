import type { PaipanResult, PillarKey, WuxingKey } from '@/types/bazi'
import { GAN_WUXING, WUXING_LABEL } from './wuxing'

export type ExplainBlockKey = 'daymaster' | 'pillars' | 'shishen' | 'shensha' | 'dayun' | 'liunian'

export interface ExplainPoint {
  label: string
  text: string
}

export interface ExplainBlock {
  key: ExplainBlockKey
  title: string
  points: ExplainPoint[]
}

export interface ChartExplanation {
  overview: string[]
  blocks: ExplainBlock[]
}

/** 十天干通俗意象（传统命理常见说法，仅供文化参考） */
const GAN_IMAGE: Record<string, string> = {
  甲: '甲木如参天大树，传统多与正直、向上的倾向相连',
  乙: '乙木如花草藤蔓，传统多与柔韧、善于应变的倾向相连',
  丙: '丙火如太阳，传统多与热情、外放的倾向相连',
  丁: '丁火如灯烛，传统多与细腻、专注的倾向相连',
  戊: '戊土如高山厚土，传统多与稳重、包容的倾向相连',
  己: '己土如田园之土，传统多与务实、温和的倾向相连',
  庚: '庚金如刀剑，传统多与果断、刚毅的倾向相连',
  辛: '辛金如珠玉，传统多与精致、敏锐的倾向相连',
  壬: '壬水如江河，传统多与流动、开阔的倾向相连',
  癸: '癸水如雨露，传统多与细腻、灵动的倾向相连',
}

/** 五行特质（传统命理常见说法，仅供文化参考） */
const WUXING_TRAIT: Record<WuxingKey, string> = {
  jin: '金：传统上与果断、原则性相连',
  mu: '木：传统上与生长、进取相连',
  shui: '水：传统上与流动、智慧相连',
  huo: '火：传统上与热情、行动相连',
  tu: '土：传统上与承载、稳定相连',
}

/** 十神通俗解释（传统命理框架） */
export const SHISHEN_TIP: Record<string, string> = {
  正官: '传统上代表规则、责任感与自律的倾向',
  七杀: '传统上代表魄力与行动力的倾向',
  正印: '传统上代表学习、庇护与接纳的倾向',
  偏印: '传统上代表独特思维与领悟力的倾向',
  比肩: '传统上代表自立与同辈关系的倾向',
  劫财: '传统上代表竞争与共享的倾向',
  食神: '传统上代表表达与享受的倾向',
  伤官: '传统上代表才华与表达欲的倾向',
  正财: '传统上代表务实与积累的倾向',
  偏财: '传统上代表灵活与交际的倾向',
}

/** 神煞通俗词条（查法见 shenSha.ts，此处仅做通俗转译，强调参考） */
const SHEN_SHA_TIP: Record<string, string> = {
  桃花: '传统称与人缘、魅力相关',
  驿马: '传统称与奔波、变动、远行相关',
  华盖: '传统称与孤高、艺术、思考相关',
  劫煞: '传统称与波动相关，遇事多留余地',
  亡神: '传统称心思较深、变化较多',
  将星: '传统称与领导力、统御相关',
  羊刃: '传统称与刚烈、行动力相关',
  禄神: '传统称与资源、安稳相关',
  天乙贵人: '传统称遇事多助力、贵人缘相关',
  文昌: '传统称与学业、文采相关',
  红艳: '传统称与魅力、情感表达相关',
  流霞: '传统称情绪起伏较明显，日常多注意安全',
  金舆: '传统称与体面、出行相关',
  魁罡: '传统称与果断、刚直相关',
  十恶大败: '传统称理财需多谨慎',
  阴阳差错: '传统称感情经营需多沟通',
  孤鸾煞: '传统称感情路需多经营',
  孤辰: '传统称独立性强、喜独处的倾向',
  寡宿: '传统称喜静、独处的倾向',
  空亡: '传统称虚浮、落空之象，需结合全局论断',
}

/** 基础术语词条（供 BaziTable 等释义复用） */
export const TERM_TIPS: Record<string, string> = {
  天干: '天干共十个（甲乙丙丁戊己庚辛壬癸），是干支纪法的上半部分，也用于表示五行属性。',
  地支: '地支共十二个（子丑寅卯辰巳午未申酉戌亥），是干支纪法的下半部分，对应十二生肖与时辰。',
  五行: '金木水火土五种基本属性，传统用于描述事物的性质与关系。',
  日主: '日柱的天干，命理中以它代表"自己"，是排盘分析的原点。',
  大运: '十年一步的大运，传统框架中用于看人生阶段的整体倾向。',
  流年: '每一年的干支，传统框架中用于看当年的倾向。',
  主星: '十神是日主与其他干支关系的称谓：生我、克我、我生、我克、同我，各分正偏共十种。',
  藏干: '地支内藏天干，代表气机层次，如申中藏庚金、壬水、戊土。',
  副星: '藏干对应日主的十神，如申中藏庚壬戊，对壬水日主分别为偏印、比肩、七杀。',
  空亡: '以日柱起旬，旬内空出的两个地支称为旬空。四支落空者传统称虚浮、落空之象，需结合全局论断。',
  胎元: '以月柱推算的受胎干支，传统用于参考先天根基，月干进一位、月支进三位。',
  命宫: '以月支与时支推算的宫位干支，传统称与人生框架、精神方向相关。',
  身宫: '以月支与时支按另一方向推算的宫位干支，传统称与后天作为、身体力行相关。',
  纳音: '纳音是六十甲子配五行的古法称谓，用于象意参考。',
  星运: '十二长生描述五行能量在十二地支中的旺衰状态，如长生、帝旺、墓、绝。',
  自坐: '自坐以本柱天干为太极点，看它在本柱地支上的十二长生状态，如甲子日甲木坐子为沐浴。星运则以日干为太极点看四支，两者口径不同。',
  神煞: '神煞源自星命术的吉凶星曜体系，需结合全局论断。',
}

const PILLAR_ROLE: Record<PillarKey, string> = {
  year: '传统框架中多与祖上、早年环境相关',
  month: '传统框架中多与父母、成长环境相关',
  day: '传统框架中代表自身，也是婚姻宫所在',
  time: '传统框架中多与晚年、子女相关',
}

const REFERENCE = '以上均为传统命理框架下的文化参考，不作人生断言。'
const BLOCK_REFERENCE = '仅供参考'

function countStrongWeak(wuXing: Record<WuxingKey, number>): { strong: string[]; weak: string[]; even: boolean } {
  const keys = Object.keys(wuXing) as WuxingKey[]
  const values = keys.map((k) => wuXing[k])
  const max = Math.max(...values)
  const min = Math.min(...values)
  if (max === min) {
    return { strong: [], weak: [], even: true }
  }
  return {
    strong: keys.filter((k) => wuXing[k] === max).map((k) => WUXING_LABEL[k]),
    weak: keys.filter((k) => wuXing[k] === min).map((k) => WUXING_LABEL[k]),
    even: false,
  }
}

function dayMasterText(result: PaipanResult): string {
  const gan = result.pillars.day.gan
  const wuXing = GAN_WUXING[gan]
  return `你的日主是「${gan}${WUXING_LABEL[wuXing]}」——${GAN_IMAGE[gan]}。`
}

function balanceText(result: PaipanResult): string {
  const { strong, weak, even } = countStrongWeak(result.wuXing)
  if (even) {
    return '五行分布较为均衡，传统上多与平和、全面的倾向相连。'
  }
  return `五行中${strong.join('、')}偏旺、${weak.join('、')}偏弱，传统上常被解读为与${strong
    .map((k) => k)
    .join('、')}相关的特质较明显、${weak.map((k) => k).join('、')}相关特质较内敛。`
}

function shenShaText(name: string): string {
  return SHEN_SHA_TIP[name] ?? `传统命理神煞之一（${name}），需结合全局论断，${BLOCK_REFERENCE}。`
}

function uniqueShenSha(list: string[][]): string[] {
  return Array.from(new Set(list.flat().filter(Boolean)))
}

/** 生成整份新人解读（总览 + 六块） */
export function explain(result: PaipanResult): ChartExplanation {
  const dayGan = result.pillars.day.gan
  const dayWuxing = WUXING_LABEL[GAN_WUXING[dayGan]]
  const overview = [
    dayMasterText(result),
    `${balanceText(result)}${WUXING_TRAIT[GAN_WUXING[dayGan]]}。`,
    REFERENCE,
  ]

  const pillarKeys: PillarKey[] = ['year', 'month', 'day', 'time']
  const pillarPoints = pillarKeys.map((k) => ({
    label: `${k === 'year' ? '年柱' : k === 'month' ? '月柱' : k === 'day' ? '日柱' : '时柱'}（${result.pillars[k].gan}${result.pillars[k].zhi}）`,
    text: PILLAR_ROLE[k],
  }))

  const shiShenSet = Array.from(new Set(pillarKeys.map((k) => result.pillars[k].shiShen)))
  const shiShenPoints = [
    {
      label: `日主十神（${result.pillars.day.shiShen}）`,
      text: SHISHEN_TIP[result.pillars.day.shiShen] ?? '传统命理十神之一，需结合全局论断。',
    },
    ...shiShenSet
      .filter((s) => s !== result.pillars.day.shiShen)
      .map((s) => ({
        label: s,
        text: SHISHEN_TIP[s] ?? '传统命理十神之一，需结合全局论断。',
      })),
    { label: '整体', text: `四柱十神分布反映的是性格与关系层面的传统参考，${BLOCK_REFERENCE}。` },
  ]

  const pillarShenSha = uniqueShenSha(pillarKeys.map((k) => result.pillars[k].shenSha))
  const shenShaPoints =
    pillarShenSha.length > 0
      ? pillarShenSha.map((s) => ({ label: s, text: shenShaText(s) }))
      : [{ label: '神煞', text: `本命局未出现高频神煞，${BLOCK_REFERENCE}。` }]

  const currentDaYun = result.daYun.find((d) => d.isCurrent)
  const dayunPoints = currentDaYun
    ? [
        {
          label: `当前大运（${currentDaYun.ganZhi}）`,
          text: `年龄 ${currentDaYun.ageRange}（${currentDaYun.yearRange}），传统框架中这一步运与「${currentDaYun.ganZhi}」相关的主题相连，${BLOCK_REFERENCE}。`,
        },
      ]
    : [{ label: '大运', text: `暂无当前大运数据，${BLOCK_REFERENCE}。` }]

  const liuNian = result.currentLiuNian
  const liunianPoints = liuNian
    ? [
        {
          label: `今年（${liuNian.ganZhi}）`,
          text: `传统框架中常以「${liuNian.ganZhi}」与命局的互动来看今年的倾向，${
            liuNian.shenSha.length > 0 ? `今年涉及神煞：${liuNian.shenSha.join('、')}。` : ''
          }${BLOCK_REFERENCE}。`,
        },
      ]
    : [{ label: '流年', text: `暂无今年流年数据，${BLOCK_REFERENCE}。` }]

  return {
    overview,
    blocks: [
      { key: 'daymaster', title: '日主与五行', points: [{ label: `日主（${dayGan}${dayWuxing}）`, text: dayMasterText(result) }, { label: '五行分布', text: balanceText(result) }] },
      { key: 'pillars', title: '四柱定位', points: pillarPoints },
      { key: 'shishen', title: '十神性格', points: shiShenPoints },
      { key: 'shensha', title: '神煞参考', points: shenShaPoints },
      { key: 'dayun', title: '当前大运', points: dayunPoints },
      { key: 'liunian', title: '流年参考', points: liunianPoints },
    ],
  }
}

/** 单块解读（ProPage 按页签取用） */
export function explainBlock(explanation: ChartExplanation, key: ExplainBlockKey): ExplainBlock | null {
  return explanation.blocks.find((b) => b.key === key) ?? null
}
