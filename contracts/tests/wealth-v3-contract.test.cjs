const assert = require('node:assert/strict')
const { existsSync, readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const { createRequire } = require('node:module')
const { test } = require('node:test')

const requireWeb = createRequire(resolve(__dirname, '../../apps/web/package.json'))
const Ajv = requireWeb('ajv')
const read = file => JSON.parse(readFileSync(resolve(__dirname, '../drafts', file), 'utf8'))
const schema = read('wealth-v3.schema.json')
const examples = read('wealth-v3.examples.json').examples
const validate = new Ajv({ allErrors: true, schemaId: 'auto' }).compile(schema)
const clone = value => JSON.parse(JSON.stringify(value))

const addV33Metadata = report => {
  report.content.copyVersion = 'wealth-plain-v3.3'
  report.content.headlinePlannerVersion = 'wealth-headline-v1'
  report.content.years.forEach((year, index) => {
    year.headlineMeta = {
      plannerVersion: 'wealth-headline-v1',
      themeKey: ['project_payment_timing', 'stable_receipt_support', 'retention_expense_limit'][index],
      pathKey: ['project_income', 'stable_income', 'retention'][index],
      subjectKey: ['project_terms', 'stable_receipt', 'retention_budget'][index],
      angleKey: ['support', 'support', 'balance'][index],
      objectKey: ['payment_timing', 'monthly_receipt', 'expense_limit'][index],
      corePhraseKeys: [['project_payment_timing'], ['stable_monthly_receipt'], ['retention_expense_limit']][index],
      selectionReasonCodes: ['annual_root', `catalog.${[70, 10, 140][index]}`],
    }
  })
  return report
}

const addV318ActionGuides = report => {
  addV33Metadata(report)
  report.content.copyVersion = 'wealth-plain-v3.18'
  report.content.headlinePlannerVersion = 'wealth-headline-v4'
  report.content.years.forEach((year, index) => {
    year.headlineMeta.plannerVersion = 'wealth-headline-v4'
    const selected = year.overview
    const guideBlock = (field, kind) => ({
      ...selected,
      id: `${year.year}.guide.${field}`,
      kind,
      templateId: `action_guide.${year.headlineMeta.themeKey}.${field}`,
    })
    year.actionGuide = {
      path: year.headlineMeta.pathKey,
      problem: guideBlock('problem', 'interpretation'),
      action: guideBlock('action', 'general_advice'),
      expectedChange: guideBlock('expected', 'observation'),
      checkTiming: guideBlock('timing', 'method_note'),
      successSignal: guideBlock('success', 'observation'),
      adjustmentCondition: guideBlock('condition', 'observation'),
      fallbackAction: guideBlock('fallback', 'general_advice'),
    }
  })
  return report
}

test('wealth v3 keeps an explicit versioned schema', () => {
  assert.ok(existsSync(resolve(__dirname, '../drafts/wealth-v3.schema.json')),
    'Missing wealth v3 draft schema: task 2 must define the contract first')
})

for (const example of examples) {
  test(`versioned structure accepts ${example.name} (not an engine acceptance test)`, () => {
    assert.ok(validate(example.report), JSON.stringify(validate.errors))
  })
}

test('historical v3.2 remains valid without headline planning metadata', () => {
  const report = clone(examples[0].report)
  report.content.copyVersion = 'wealth-plain-v3.2'
  delete report.content.headlinePlannerVersion
  report.content.years.forEach(year => { delete year.headlineMeta })
  assert.ok(validate(report), JSON.stringify(validate.errors))
})

test('v3.3 accepts complete report-level and annual headline metadata', () => {
  const report = addV33Metadata(clone(examples[0].report))
  assert.ok(validate(report), JSON.stringify(validate.errors))
})

test('v3.3 rejects a missing report-level planner version', () => {
  const report = addV33Metadata(clone(examples[0].report))
  delete report.content.headlinePlannerVersion
  assert.equal(validate(report), false)
})

test('v3.3 rejects an annual item without headline metadata', () => {
  const report = addV33Metadata(clone(examples[0].report))
  delete report.content.years[1].headlineMeta
  assert.equal(validate(report), false)
})

test('v3.18 requires a complete annual action guide', () => {
  const report = addV318ActionGuides(clone(examples[0].report))
  assert.ok(validate(report), JSON.stringify(validate.errors))
  delete report.content.years[1].actionGuide
  assert.equal(validate(report), false)
})

const mutations = [
  ['wrong version', r => { r.contentVersion = 'wealth-narrative-v2' }],
  ['professional edition', r => { r.edition = 'professional' }],
  ['five-year product response', r => { r.content.horizonYears = 5 }],
  ['missing risk instead of explicit null', r => { delete r.content.years[0].risk }],
  ['empty risk object', r => { r.content.years[0].risk = {} }],
  ['risk with no references', r => {
    r.content.years[0].risk = {
      path: 'retention', limitingEvidenceIds: [], reading: r.content.years[0].retention,
    }
  }],
  ['four decisions instead of five', r => { r.content.years[0].decisions.pop() }],
  ['interpretation without a decision', r => { r.content.thesis.decisionIds = [] }],
  ['unapproved certainty level', r => { r.content.years[0].decisions[0].strength = 'certain' }],
  ['negative support sum', r => { r.content.years[0].decisions[0].supportWeight = -1 }],
  ['none focus with a forced winner', r => {
    r.content.years[0].focus = { state: 'none', primaryCandidates: ['stable_income'], secondaryCandidates: [] }
  }],
  ['one-candidate tie', r => {
    r.content.years[0].focus = { state: 'tied', primaryCandidates: ['stable_income'], secondaryCandidates: [] }
  }],
  ['duplicate tied candidates', r => {
    r.content.years[0].focus = { state: 'tied', primaryCandidates: ['stable_income', 'stable_income'], secondaryCandidates: [] }
  }],
  ['changed year with empty differences', r => { r.content.years[0].comparison.direction = 'changed' }],
  ['unknown field', r => { r.content.guaranteedIncome = 10000 }],
]

for (const [label, mutate] of mutations) {
  test(`versioned schema rejects ${label}`, () => {
    const report = clone(examples[0].report)
    mutate(report)
    assert.equal(validate(report), false, label)
  })
}

test('v3 content is registered for live backend and frontend reading', () => {
  assert.equal(schema['x-status'], 'implemented')
  const api = readFileSync(resolve(__dirname, '../openapi.yaml'), 'utf8')
  assert.match(api, /x-planned-contracts:[\s\S]*wealth-narrative-v3:/)
  assert.match(api, /wealth-narrative-v3:[\s\S]*status: implemented/)
  const currentInfo = api.split('    ReportInfo:')[1].split('    CareerNarrativePlan:')[0]
  assert.ok(currentInfo.includes("./drafts/wealth-v3.schema.json#/definitions/Content"))
  assert.match(currentInfo, /wealth-narrative-v3/)
})

test('constructed examples reference only their own facts and decisions', () => {
  for (const { report } of examples) {
    const c = report.content
    const decisions = new Map(c.years.flatMap(y => y.decisions.map(d => [d.id, d])))
    const evidence = new Map(c.years.flatMap(y => y.evidence.map(e => [e.id, e])))
    for (const y of c.years) {
      const facts = new Set(y.facts.map(f => f.id))
      assert.deepEqual([...new Set(y.decisions.map(d => d.path))].sort(),
        schema.definitions.Path.enum.slice().sort())
      y.evidence.forEach(e => e.rootFactIds.forEach(id => assert.ok(facts.has(id), id)))
      for (const d of y.decisions) {
        assert.equal(d.year, y.year)
        const support = d.supportingEvidenceIds.map(id => evidence.get(id))
        const limits = d.limitingEvidenceIds.map(id => evidence.get(id))
        support.forEach(e => assert.ok(e && e.path === d.path && e.weight > 0))
        limits.forEach(e => assert.ok(e && e.path === d.path && e.weight < 0))
        assert.equal(d.supportWeight, support.reduce((sum, e) => sum + e.weight, 0))
        assert.equal(d.limitationWeight, limits.reduce((sum, e) => sum - e.weight, 0))
        assert.equal(d.netWeight, d.supportWeight - d.limitationWeight)
      }
      if (y.risk) y.risk.limitingEvidenceIds.forEach(id => assert.ok(evidence.get(id)?.weight < 0))
    }
    const visit = value => {
      if (!value || typeof value !== 'object') return
      if ('templateId' in value) value.decisionIds.forEach(id => {
        assert.ok(decisions.has(id), id)
        assert.ok(value.years.includes(decisions.get(id).year), id)
      })
      Object.values(value).forEach(visit)
    }
    visit(c)
  }
})
