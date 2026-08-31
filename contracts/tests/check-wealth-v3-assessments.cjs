// Checks actual Java task-4 output fragments, not a completed report or prose quality.
// First capture: mvn -o -Dtest=WealthProvenanceResolverTest -Dwealth.captureAssessments=true test
const assert = require('node:assert/strict')
const { readFileSync } = require('node:fs')
const { resolve } = require('node:path')
const { createRequire } = require('node:module')
const requireWeb = createRequire(resolve(__dirname, '../../apps/web/package.json'))
const Ajv = requireWeb('ajv')
const schema = JSON.parse(readFileSync(resolve(__dirname, '../drafts/wealth-v3.schema.json'), 'utf8'))
assert.ok(process.argv[2], 'Pass the generated Java assessment artifact path')
const data = JSON.parse(readFileSync(resolve(process.argv[2]), 'utf8'))
assert.equal(data.scope, 'internal-assessment-only-not-a-ready-report')
assert.equal(data.calculationVersion, 'wealth-path-v2')
assert.equal(data.policyVersion, 'wealth-expression-v1')
assert.equal(data.cases.length, 15)
const ajv = new Ajv({ allErrors: true, schemaId: 'auto' }).addSchema(schema)
const validators = Object.fromEntries(['Fact', 'Evidence', 'Decision', 'Focus']
  .map(name => [name, ajv.compile({ $ref: `${schema.$id}#/definitions/${name}` })]))
const validate = (name, value) => assert.ok(validators[name](value),
  `${name}: ${JSON.stringify(validators[name].errors)}`)
let years = 0
let decisions = 0
for (const sample of data.cases) {
  for (const year of sample.years) {
    years++
    year.facts.forEach(f => validate('Fact', f))
    year.evidence.forEach(e => validate('Evidence', e))
    year.decisions.forEach(d => { validate('Decision', d); decisions++ })
    validate('Focus', year.focus)
    assert.equal(new Set(year.decisions.map(d => d.path)).size, 5)
    const facts = new Set(year.facts.map(f => f.id))
    const evidence = new Map(year.evidence.map(e => [e.id, e]))
    year.evidence.forEach(e => e.rootFactIds.forEach(id => assert.ok(facts.has(id))))
    if (year.risk) year.risk.limitingEvidenceIds.forEach(id => {
      const e = evidence.get(id)
      assert.ok(e && e.weight < 0 && e.path === year.risk.path)
    })
  }
}
assert.equal(years, 46)
assert.equal(decisions, 230)
console.log(`Java assessment fragments match draft fields: ${data.cases.length} cases, ${years} years, ${decisions} decisions. This is not ready-report validation.`)
