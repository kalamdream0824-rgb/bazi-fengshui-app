import { openDB, type DBSchema, type IDBPDatabase } from 'idb'
import type { PaipanRequest, PaipanResult } from '@/types/bazi'

export interface HistoryRecord {
  id?: number
  request: PaipanRequest
  result: PaipanResult
  createdAt: string
}

interface BaziDB extends DBSchema {
  records: {
    key: number
    value: HistoryRecord
    indexes: { byCreatedAt: string }
  }
}

const DB_NAME = 'bazi-app'
const STORE = 'records'

let dbPromise: Promise<IDBPDatabase<BaziDB>> | null = null

function getDb(): Promise<IDBPDatabase<BaziDB>> {
  if (!dbPromise) {
    dbPromise = openDB<BaziDB>(DB_NAME, 1, {
      upgrade(db) {
        const store = db.createObjectStore(STORE, { keyPath: 'id', autoIncrement: true })
        store.createIndex('byCreatedAt', 'createdAt')
      },
    })
  }
  return dbPromise
}

export async function addHistory(request: PaipanRequest, result: PaipanResult): Promise<void> {
  const db = await getDb()
  await db.add(STORE, { request, result, createdAt: new Date().toISOString() })
}

export async function listHistory(): Promise<HistoryRecord[]> {
  const db = await getDb()
  const records = await db.getAllFromIndex(STORE, 'byCreatedAt')
  return records.reverse() // 新的在前
}

export async function removeHistory(id: number): Promise<void> {
  const db = await getDb()
  await db.delete(STORE, id)
}

export async function clearHistory(): Promise<void> {
  const db = await getDb()
  await db.clear(STORE)
}

const BACKUP_VERSION = 1

export interface BackupFile {
  version: number
  exportedAt: string
  records: HistoryRecord[]
}

export function historyToBackup(records: HistoryRecord[]): BackupFile {
  return { version: BACKUP_VERSION, exportedAt: new Date().toISOString(), records }
}

export function backupToRecords(data: unknown): HistoryRecord[] {
  if (!data || typeof data !== 'object') {
    throw new Error('备份文件格式不正确')
  }
  const backup = data as BackupFile
  if (backup.version !== BACKUP_VERSION || !Array.isArray(backup.records)) {
    throw new Error('备份文件格式不正确')
  }
  return backup.records
    .filter((r) => r && r.request && r.result && typeof r.createdAt === 'string')
    .map((r) => ({ request: r.request, result: r.result, createdAt: r.createdAt }))
}

function dedupeKey(r: HistoryRecord): string {
  return `${r.request.gender}|${r.request.solarDateTime}|${r.request.name ?? ''}`
}

export async function importHistory(records: HistoryRecord[]): Promise<{ added: number; skipped: number }> {
  const db = await getDb()
  const existing = await db.getAll(STORE)
  const keys = new Set(existing.map(dedupeKey))
  let added = 0
  let skipped = 0
  for (const r of records) {
    const key = dedupeKey(r)
    if (keys.has(key)) {
      skipped += 1
      continue
    }
    keys.add(key)
    await db.add(STORE, r)
    added += 1
  }
  return { added, skipped }
}

export function downloadBackup(records: HistoryRecord[]): void {
  const blob = new Blob([JSON.stringify(historyToBackup(records), null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `bazi-backup-${new Date().toISOString().slice(0, 10)}.json`
  a.click()
  URL.revokeObjectURL(url)
}
