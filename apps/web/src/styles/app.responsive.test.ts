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
