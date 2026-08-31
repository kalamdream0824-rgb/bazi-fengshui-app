// Validate actual Java task-5 fragments, not completed report content or natural-language quality.
// Capture in apps/server: mvn -o -Dtest=WealthComparisonIntegrationTest -Dwealth.captureComparisons=true test
const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const { createRequire } = require('node:module')
const requireWeb = createRequire(resolve(__dirname, '../../apps/web/package.json'))
const Ajv = requireWeb('ajv')
const schema = JSON.parse(readFileSync(resolve(__dirname, '../drafts/wealth-v3.schema.json'), 'utf8'))
assert.ok(process.argv[2], 'Pass the generated Java comparison artifact path')
const data = JSON.parse(readFileSync(resolve(process.argv[2]), 'utf8'))
assert.equal(data.scope, 'internal-comparison-only-not-a-ready-report')
assert.equal(data.calculationVersion, 'wealth-path-v2')
assert.equal(data.policyVersion, 'wealth-expression-v1')
assert.equal(data.cases.length, 15)
const ajv = new Ajv({ allErrors: true, schemaId: 'auto' }).addSchema(schema)
const original = schema.definitions.Comparison
// Reuse task-5 data-only fields; check-wealth-v3-narratives.cjs validates task-6 prose as full Content.
const keys = ['toYear', 'direction', 'changes']
const validateComparison = ajv.compile({
  type: original.type,
  additionalProperties: false,
  required: keys,
  properties: Object.fromEntries(keys.map(key => [key, original.properties[key]])),
  definitions: schema.definitions,
  allOf: original.allOf,
})
const validators = Object.fromEntries(['Fact', 'Evidence', 'Decision', 'Focus']
  .map(name => [name, ajv.compile({ $ref: `${schema.$id}#/definitions/${name}` })]))
const validate = (name, value) => assert.ok(validators[name](value),
  `${name}: ${JSON.stringify(validators[name].errors)}`)
let years = 0
let comparisons = 0
for (const sample of data.cases) {
  assert.ok(sample.years.length >= 2 && sample.years.length <= 5)
  sample.years.forEach((item, index) => {
    years++
    const current = item.assessment
    current.facts.forEach(f => validate('Fact', f))
    current.evidence.forEach(e => validate('Evidence', e))
    current.decisions.forEach(d => validate('Decision', d))
    validate('Focus', current.focus)
    assert.equal(new Set(current.decisions.map(d => d.path)).size, 5)
    if (index === sample.years.length - 1) {
      assert.equal(item.comparison, null)
      return
    }
    comparisons++
    const comparison = item.comparison
    assert.ok(validateComparison(comparison), JSON.stringify(validateComparison.errors))
    const next = sample.years[index + 1].assessment
    assert.equal(next.year, current.year + 1)
    assert.equal(comparison.toYear, next.year)
    assert.equal(new Set(comparison.changes.map(c => c.path)).size, comparison.changes.length)
    for (const change of comparison.changes) {
      const before = current.decisions.find(d => d.path === change.path)
      const after = next.decisions.find(d => d.path === change.path)
      assert.ok(before && after)
      assert.equal(change.supportDelta, after.supportWeight - before.supportWeight)
      assert.equal(change.limitationDelta, after.limitationWeight - before.limitationWeight)
      change.addedEvidenceIds.forEach(id => assert.ok(next.evidence.some(e => e.id === id && e.path === change.path)))
      change.removedEvidenceIds.forEach(id => assert.ok(current.evidence.some(e => e.id === id && e.path === change.path)))
    }
  })
}
assert.equal(years, 46)
assert.equal(comparisons, 31)
console.log(`Java comparison fragments match draft fields: ${data.cases.length} cases, ${years} years, ${comparisons} comparisons. This is not ready-report validation.`)
