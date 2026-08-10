import html2canvas from 'html2canvas'
import type { PaipanRequest, PaipanResult, PillarKey } from '@/types/bazi'

const C = {
  paper: '#f6f1e4',
  card: '#fffcf4',
  ink: '#2f251c',
  ink2: '#6f5d48',
  red: '#b03a2e',
  gold: '#b8904a',
  line: '#e4d8bd',
}

const KEYS: PillarKey[] = ['year', 'month', 'day', 'time']
const LABELS: Record<PillarKey, string> = { year: '年柱', month: '月柱', day: '日柱', time: '时柱' }

function genderLabel(request: PaipanRequest): string {
  return request.gender === 'male' ? '乾造' : '坤造'
}

export function buildShareText(request: PaipanRequest, result: PaipanResult): string {
  const pillars = KEYS.map((k) => result.pillars[k].gan + result.pillars[k].zhi).join(' ')
  return [
    `【八字排盘】${request.name || '示例'}（${genderLabel(request)}）${result.solarText}`,
    `四柱：${pillars}`,
    `生肖：${result.shengXiao}`,
    '仅供传统文化研究参考',
  ].join('\n')
}

export function buildShareCardHtml(request: PaipanRequest, result: PaipanResult): string {
  const pillarCells = KEYS.map((k) => {
    const p = result.pillars[k]
    return `<div style="flex:1;text-align:center;border:1px solid ${C.line};border-radius:12px;background:${C.card};padding:18px 8px">
      <div style="font-size:22px;color:${C.ink2}">${LABELS[k]}</div>
      <div style="font-size:44px;font-weight:700;color:${k === 'day' ? C.red : C.ink};margin:12px 0">${p.gan}${p.zhi}</div>
      <div style="font-size:20px;color:${C.ink2}">${p.shiShen}</div>
      <div style="font-size:18px;color:${C.ink2};margin-top:8px">${p.naYin}</div>
    </div>`
  }).join('')

  return `<div style="width:1080px;height:1080px;background:${C.paper};color:${C.ink};font-family:'Songti SC','PingFang SC',serif;display:flex;flex-direction:column;box-sizing:border-box">
    <div style="padding:64px 72px 32px;display:flex;align-items:center;justify-content:space-between">
      <div style="display:flex;align-items:center;gap:16px">
        <div style="width:48px;height:48px;background:${C.red};color:#fff6e8;border-radius:8px 16px 8px 16px;display:flex;align-items:center;justify-content:center;font-size:26px">命</div>
        <div style="font-size:40px;font-weight:700;letter-spacing:8px">八字排盘</div>
      </div>
      <div style="font-size:24px;color:${C.ink2}">${request.name || '示例'} · ${genderLabel(request)}</div>
    </div>
    <div style="padding:0 72px;display:flex;gap:20px;flex:1">${pillarCells}</div>
    <div style="padding:0 72px 48px;display:flex;justify-content:space-between;font-size:24px;color:${C.ink2}">
      <span>农历 ${result.lunarText} · ${result.timeZhi}时</span><span>生肖 ${result.shengXiao}</span>
    </div>
    <div style="padding:20px 72px 40px;text-align:center;font-size:20px;color:${C.ink2}">排盘结果仅供传统文化研究参考 · 由「朱墨星图」排盘生成</div>
  </div>`
}

export async function generateShareImage(request: PaipanRequest, result: PaipanResult): Promise<string> {
  const container = document.createElement('div')
  container.style.cssText = 'position:fixed;left:-10000px;top:0;z-index:-1'
  container.innerHTML = buildShareCardHtml(request, result)
  document.body.appendChild(container)
  try {
    const canvas = await html2canvas(container.firstElementChild as HTMLElement, { scale: 1, backgroundColor: C.paper })
    return canvas.toDataURL('image/jpeg', 0.9)
  } finally {
    container.remove()
  }
}

export function downloadImage(dataUrl: string, filename: string): void {
  const a = document.createElement('a')
  a.href = dataUrl
  a.download = filename
  a.click()
}

export async function copyText(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text)
    return true
  } catch {
    return false
  }
}

export interface PreparedShare {
  text: string
  imageUrl: string
}

export async function prepareShare(request: PaipanRequest, result: PaipanResult): Promise<PreparedShare> {
  return {
    text: buildShareText(request, result),
    imageUrl: await generateShareImage(request, result),
  }
}

export async function dataUrlToFile(dataUrl: string, filename: string): Promise<File> {
  const blob = await (await fetch(dataUrl)).blob()
  return new File([blob], filename, { type: 'image/jpeg' })
}

export async function shareImageFile(imageUrl: string, filename: string): Promise<boolean> {
  if (typeof navigator.share !== 'function') {
    return false
  }
  try {
    const file = await dataUrlToFile(imageUrl, filename)
    if (navigator.canShare && navigator.canShare({ files: [file] })) {
      await navigator.share({ files: [file] })
      return true
    }
  } catch {
    // 用户取消或不支持文件分享
  }
  return false
}

export function hasNativeShare(): boolean {
  return typeof navigator.share === 'function'
}

export async function downloadShareImage(request: PaipanRequest, result: PaipanResult): Promise<void> {
  const url = await generateShareImage(request, result)
  downloadImage(url, `排盘-${request.name || '示例'}.jpg`)
}
