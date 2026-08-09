import { beforeEach, describe, expect, it } from 'vitest'
import { paipan } from '@/lib/baziMapper'
import {
  addHistory,
  backupToRecords,
  clearHistory,
  historyToBackup,
  importHistory,
  listHistory,
  removeHistory,
} from './historyStore'

const reqA = { gender: 'male' as const, solarDateTime: '1995-10-08T14:30:00', trueSolarTime: false }
const reqB = { gender: 'female' as const, solarDateTime: '1996-03-18T10:00:00', trueSolarTime: false }

describe('historyStore', () => {
  beforeEach(async () => {
    await clearHistory()
  })

  it('新增并倒序列出（新的在前）', async () => {
    await addHistory(reqA, paipan(reqA))
    await addHistory(reqB, paipan(reqB))
    const list = await listHistory()
    expect(list).toHaveLength(2)
    expect(list[0].request.gender).toBe('female')
  })

  it('按 id 删除', async () => {
    await addHistory(reqA, paipan(reqA))
    const list = await listHistory()
    await removeHistory(list[0].id as number)
    expect(await listHistory()).toHaveLength(0)
  })

  it('导出/导入 JSON 往返一致', async () => {
    await addHistory(reqA, paipan(reqA))
    const backup = historyToBackup(await listHistory())
    const restored = backupToRecords(JSON.parse(JSON.stringify(backup)))
    expect(restored).toHaveLength(1)
    expect(restored[0].request.solarDateTime).toBe('1995-10-08T14:30:00')
    expect(restored[0].id).toBeUndefined()
  })

  it('导入按「性别+出生时间+姓名」去重', async () => {
    await addHistory(reqA, paipan(reqA))
    const dup = backupToRecords(historyToBackup([{ request: reqA, result: paipan(reqA), createdAt: new Date().toISOString() }]))
    const res = await importHistory(dup)
    expect(res.added).toBe(0)
    expect(res.skipped).toBe(1)
  })

  it('非法备份文件抛错', () => {
    expect(() => backupToRecords({ version: 999 })).toThrow()
    expect(() => backupToRecords('oops')).toThrow()
  })
})
