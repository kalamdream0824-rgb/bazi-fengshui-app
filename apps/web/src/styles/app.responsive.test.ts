import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const appCss = readFileSync(resolve(process.cwd(), 'src/styles/app.css'), 'utf8')

describe('财富命书移动端摘要布局', () => {
  it('在 430px 视口把三张钱路摘要改为两列，并让主钱路占满首行', () => {
    expect(appCss).toMatch(
      /@media \(max-width: 430px\)[\s\S]*?\.wealth-reader__directions > div\s*\{[^}]*grid-template-columns:\s*repeat\(2,/,
    )
    expect(appCss).toMatch(
      /@media \(max-width: 430px\)[\s\S]*?\.money-direction\.is-primary\s*\{[^}]*grid-column:\s*1 \/ -1/,
    )
  })
})

describe('综合v3命书移动布局', () => {
  it('430px下收紧主次联动线，避免三列内容撑出纸面', () => {
    expect(appCss).toMatch(
      /@media \(max-width: 430px\)[\s\S]*?\.overall-v3-year__decision\s*\{[^}]*grid-template-columns:\s*minmax\(0, 1fr\) 28px minmax\(0, 1fr\)/,
    )
    expect(appCss).toMatch(
      /@media \(max-width: 430px\)[\s\S]*?\.overall-v3-year__decision > div\s*\{[^}]*padding-inline:\s*9px/,
    )
  })

  it('360px下观察项改为单列，纸面继续与页面等宽对齐', () => {
    expect(appCss).toMatch(
      /@media \(max-width: 390px\)[\s\S]*?\.overall-v3-year__observations ul\s*\{[^}]*grid-template-columns:\s*1fr/,
    )
    expect(appCss).toMatch(
      /\.report-reader__paper,\s*\.report-library > main\s*\{[^}]*width:\s*100%;[^}]*margin:\s*8px 0 0/,
    )
  })
})
