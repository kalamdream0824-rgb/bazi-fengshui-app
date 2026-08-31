const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const { test } = require('node:test')

const api = readFileSync(resolve(__dirname, '../openapi.yaml'), 'utf8')

function section(start, end) {
  const from = api.indexOf(start)
  assert.notEqual(from, -1, `missing contract section: ${start}`)
  const to = end ? api.indexOf(end, from + start.length) : api.length
  assert.notEqual(to, -1, `missing contract boundary: ${end}`)
  return api.slice(from, to)
}

test('checkout creates a server-priced pending order for a locked report', () => {
  const checkout = section('  /reports/checkout:', '  /reports/checkout/{orderId}/mock-pay:')
  assert.match(checkout, /post:/)
  assert.match(checkout, /ReportPreviewRequest/)
  assert.match(checkout, /ReportCheckoutInfo/)
  assert.match(checkout, /金额由服务端商品目录决定/)
  assert.match(checkout, /支付前[^\n]*locked/)
})

test('mock payment endpoint is documented as non-production and unlocks the same report', () => {
  const mockPay = section('  /reports/checkout/{orderId}/mock-pay:', '  /reports/preview:')
  assert.match(mockPay, /仅非生产环境/)
  assert.match(mockPay, /幂等/)
  assert.match(mockPay, /同一份命书/)
  assert.match(mockPay, /ReportInfo/)
})

test('locked reports are excluded from list and detail reads', () => {
  const reports = section('  /reports:', '  /reports/checkout:')
  const detail = section('  /reports/{id}:', 'components:')
  assert.match(reports, /只返回 ready/)
  assert.match(detail, /locked[^\n]*404/)
})

test('sales matrix exposes only three plain products at 690 cents', () => {
  const catalog = section('x-report-products:', 'servers:')
  for (const topic of ['career', 'wealth', 'relationship']) {
    assert.match(catalog, new RegExp(`topic: ${topic}\\n[\\s\\S]{0,100}edition: plain\\n[\\s\\S]{0,100}amountCents: 690`))
  }
  assert.match(catalog, /unavailable:[\s\S]*topic: overall[\s\S]*edition: plain/)
  for (const topic of ['career', 'wealth', 'relationship']) {
    assert.match(catalog, new RegExp(`topic: ${topic}\\n[\\s\\S]{0,100}edition: professional`))
  }
  assert.doesNotMatch(catalog, /amountCents:\s*1290/)
})

test('checkout response makes server pricing and pending state explicit', () => {
  const schema = section('    ReportCheckoutInfo:', '    ReportInfo:')
  assert.match(schema, /required: \[orderId, reportId, topic, edition, amountCents, status\]/)
  assert.match(schema, /amountCents:[\s\S]*readOnly: true/)
  assert.match(schema, /status:[\s\S]*enum: \[pending\]/)
})

test('design index records checkout ownership and mock-payment boundary', () => {
  const designIndex = readFileSync(resolve(__dirname, '../../docs/design/README.md'), 'utf8')
  assert.match(designIndex, /2026-08-30-single-report-checkout\.md/)
  assert.match(designIndex, /服务端定价/)
  assert.match(designIndex, /模拟支付[^\n]*非真实扣款/)
})
