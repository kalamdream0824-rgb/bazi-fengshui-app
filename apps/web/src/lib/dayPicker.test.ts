import { describe, expect, it } from 'vitest'
import type { AlmanacInfo } from './almanac'
import { evaluateDay, pickDays } from './dayPicker'

function mk(over: Partial<AlmanacInfo>): AlmanacInfo {
  return {
    ganZhi: '乙卯',
    yi: [],
    ji: [],
    chongDesc: '冲（己酉）鸡',
    sha: '西',
    chongShengXiao: '鸡',
    zhiXing: '定',
    pengZu: '',
    positionFu: '正东',
    ...over,
  }
}

describe('evaluateDay', () => {
  it('忌关键字一票否决', () => {
    const r = evaluateDay('marry', mk({ yi: ['嫁娶'], ji: ['安葬'] }))
    expect(r.pass).toBe(false)
    expect(r.reasons.some((t) => t.includes('安葬'))).toBe(true)
  })

  it('诸事不宜否决', () => {
    expect(evaluateDay('open', mk({ yi: ['开市'], ji: ['诸事不宜'] })).pass).toBe(false)
  })

  it('命中宜项通过并给出理由', () => {
    const r = evaluateDay('marry', mk({ yi: ['嫁娶', '祭祀'], ji: ['祭祀'] }))
    expect(r.pass).toBe(true)
    expect(r.reasons.some((t) => t.includes('嫁娶'))).toBe(true)
  })

  it('冲命主生肖减分并标注', () => {
    const r = evaluateDay('marry', mk({ yi: ['嫁娶'], ji: [], chongShengXiao: '猪' }), '猪')
    expect(r.pass).toBe(true)
    expect(r.reasons.some((t) => t.includes('冲猪'))).toBe(true)
  })
})

describe('pickDays', () => {
  it('确定性且只返回推荐日', () => {
    const fake = (offset: number) =>
      mk({ yi: offset % 2 === 0 ? ['嫁娶'] : [], ji: offset % 5 === 0 ? ['安葬'] : [] })
    const a = pickDays('marry', 10, undefined, fake)
    const b = pickDays('marry', 10, undefined, fake)
    expect(JSON.stringify(a)).toBe(JSON.stringify(b))
    expect(a.every((d) => d.recommended)).toBe(true)
  })
})
