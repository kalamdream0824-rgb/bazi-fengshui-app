import { jsPDF } from 'jspdf'
import html2canvas from 'html2canvas'
import type { PaipanRequest, PaipanResult, PillarKey, WuxingKey } from '@/types/bazi'
import { WUXING_LABEL } from './wuxing'

const A4_W = 210
const A4_H = 297
const PAGE_W = 794 // 96dpi A4 宽度

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

function pillarRow(label: string, get: (k: PillarKey) => string): string {
  return `<tr>
    <td style="padding:8px 4px;border:1px solid ${C.line};background:${C.paper};font-size:12px;color:${C.ink2}">${label}</td>
    ${KEYS.map((k) => `<td style="padding:8px 4px;border:1px solid ${C.line};text-align:center;font-size:14px;color:${k === 'day' ? C.red : C.ink}">${get(k)}</td>`).join('')}
  </tr>`
}

export function buildReportHtml(request: PaipanRequest, result: PaipanResult): string {
  const genderLabel = request.gender === 'male' ? '乾造' : '坤造'
  const today = new Date().toLocaleDateString('zh-CN')

  const wuxingRow = (['jin', 'mu', 'shui', 'huo', 'tu'] as WuxingKey[])
    .map(
      (k) => `<td style="padding:8px 4px;text-align:center;font-size:13px">${WUXING_LABEL[k]} ${result.wuXing[k]}</td>`,
    )
    .join('')

  return `
    <div style="width:${PAGE_W}px;background:${C.paper};color:${C.ink};font-family:'Songti SC','PingFang SC',serif;box-sizing:border-box">

      <div class="page" style="width:100%;min-height:900px;padding:48px 56px;box-sizing:border-box">
        <div style="border:2px solid ${C.gold};padding:64px 40px;text-align:center">
          <div style="font-size:52px;letter-spacing:18px;color:${C.ink}">命 书</div>
          <div style="width:54px;height:54px;margin:28px auto 0;background:${C.red};color:#fff6e8;border-radius:6px 12px 6px 12px;display:flex;align-items:center;justify-content:center;font-size:24px;transform:rotate(-4deg)">命</div>
          <div style="margin-top:32px;font-size:17px">${request.name || '示例'} · ${genderLabel}</div>
          <div style="margin-top:10px;font-size:13px;color:${C.ink2}">农历 ${result.lunarText} · ${result.timeZhi}时</div>
          <div style="margin-top:6px;font-size:13px;color:${C.ink2}">公历 ${result.solarText} · 生肖 ${result.shengXiao}</div>
        </div>
        <div style="margin-top:40px;text-align:center;font-size:11px;color:${C.ink2}">生成日期 ${today} · 仅供传统文化研究参考</div>
      </div>

      <div class="page" style="width:100%;min-height:900px;padding:36px 40px;box-sizing:border-box">
        <div style="font-size:20px;font-weight:bold;border-left:4px solid ${C.red};padding-left:10px;margin-bottom:16px">一、命盘概览</div>
        <table style="width:100%;border-collapse:collapse">
          <tr>
            <td style="padding:8px 4px;border:1px solid ${C.line};background:${C.paper};font-size:12px;color:${C.ink2}">四柱</td>
            ${KEYS.map((k) => `<td style="padding:8px 4px;border:1px solid ${C.line};text-align:center;font-size:13px;font-weight:bold;color:${k === 'day' ? C.red : C.ink}">${LABELS[k]}</td>`).join('')}
          </tr>
          ${pillarRow('主星', (k) => result.pillars[k].shiShen)}
          ${pillarRow('天干', (k) => result.pillars[k].gan)}
          ${pillarRow('地支', (k) => result.pillars[k].zhi)}
          ${pillarRow('藏干', (k) => result.pillars[k].hideGan.map((h) => h.gan).join(' '))}
          ${pillarRow('副星', (k) => result.pillars[k].hideGan.map((h) => h.shiShen).join(' '))}
          ${pillarRow('空亡', (k) => result.pillars[k].xunKong)}
          ${pillarRow('纳音', (k) => result.pillars[k].naYin)}
          ${pillarRow('星运', (k) => result.pillars[k].diShi)}
          ${pillarRow('自坐', (k) => result.pillars[k].ziZuo)}
          ${pillarRow('神煞', (k) => result.pillars[k].shenSha.join('·') || '—')}
        </table>
        ${result.taiYuan ? `<div style="margin-top:14px;font-size:13px;color:${C.ink}">
          胎元 ${result.taiYuan}（${result.taiYuanNaYin}） · 命宫 ${result.mingGong}（${result.mingGongNaYin}） · 身宫 ${result.shenGong}（${result.shenGongNaYin}）
        </div>` : ''}
        <div style="font-size:14px;font-weight:bold;margin:22px 0 8px">五行占比（本气计数）</div>
        <table style="width:100%;border-collapse:collapse"><tr>${wuxingRow}</tr></table>
        ${result.trueSolar ? `<div style="margin-top:16px;font-size:12px;color:${C.ink2}">真太阳时校正：${result.trueSolar.original} → ${result.trueSolar.adjusted}（经度 ${result.trueSolar.longitude}°E）</div>` : ''}
      </div>

      <div class="page" style="width:100%;min-height:900px;padding:36px 40px;box-sizing:border-box">
        <div style="font-size:20px;font-weight:bold;border-left:4px solid ${C.red};padding-left:10px;margin-bottom:16px">二、大运与参考</div>
        <div style="font-size:14px;font-weight:bold;margin-bottom:8px">大运（十年一运）</div>
        <table style="width:100%;border-collapse:collapse">
          ${result.daYun.map((d) => `<tr><td style="padding:8px 10px;border:1px solid ${C.line};font-size:13px">${d.ageRange}</td><td style="padding:8px 10px;border:1px solid ${C.line};font-size:15px;font-weight:bold;color:${d.isCurrent ? C.red : C.ink}">${d.ganZhi}${d.isCurrent ? '（今）' : ''}</td><td style="padding:8px 10px;border:1px solid ${C.line};font-size:12px;color:${C.ink2}">${d.yearRange}</td></tr>`).join('')}
        </table>
        <div style="font-size:14px;font-weight:bold;margin:22px 0 8px">十神概览</div>
        <div style="font-size:13px;line-height:2;color:${C.ink}">
          ${KEYS.map((k) => `${LABELS[k]}：${result.pillars[k].gan}${result.pillars[k].zhi}（${result.pillars[k].shiShen}）`).join('　')}
        </div>
        <div style="font-size:14px;font-weight:bold;margin:22px 0 8px">参考建议</div>
        <div style="font-size:13px;line-height:2;color:${C.ink2}">
          本命书为排盘数据的整理与呈现，供传统文化研究参考。命理分析存在流派差异，请结合自身实际情况理性看待，不作决策依据。
        </div>
        <div style="margin-top:36px;text-align:center;font-size:11px;color:${C.ink2}">— 本文档由「朱墨星图」排盘工具生成 —</div>
      </div>
    </div>
  `
}

export function reportFileName(request: PaipanRequest, result: PaipanResult): string {
  return `命书-${request.name || '示例'}-${result.solarText.slice(0, 10)}.pdf`
}

export async function exportMingshuPdf(request: PaipanRequest, result: PaipanResult): Promise<void> {
  const container = document.createElement('div')
  container.style.cssText = `position:fixed;left:-10000px;top:0;width:${PAGE_W}px;background:${C.paper};z-index:-1`
  container.innerHTML = buildReportHtml(request, result)
  document.body.appendChild(container)

  try {
    const pdf = new jsPDF({ unit: 'mm', format: 'a4', orientation: 'portrait' })
    const pages = Array.from(container.querySelectorAll<HTMLElement>('.page'))

    for (let i = 0; i < pages.length; i += 1) {
      const canvas = await html2canvas(pages[i], { scale: 1.5, backgroundColor: C.paper, useCORS: true })
      // JPEG 压缩显著减小 PDF 体积（页面为不透明纸色底，无损透明需求）
      const img = canvas.toDataURL('image/jpeg', 0.85)
      if (i > 0) pdf.addPage()

      const ratio = canvas.width / canvas.height
      let w = A4_W - 16
      let h = w / ratio
      if (h > A4_H - 16) {
        h = A4_H - 16
        w = h * ratio
      }
      pdf.addImage(img, 'PNG', (A4_W - w) / 2, (A4_H - h) / 2, w, h)
    }

    pdf.save(reportFileName(request, result))
  } finally {
    container.remove()
  }
}
