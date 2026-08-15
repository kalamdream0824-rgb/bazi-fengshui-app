import { useAuthStore } from '@/store/useAuthStore'
import type { HistoryRecord } from './historyStore'
import { listHistory } from './historyStore'

function isHttpMode(): boolean {
  return import.meta.env.VITE_API_MODE === 'http'
}

function authHeaders(): Record<string, string> {
  const token = useAuthStore.getState().token
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/** 登录后将本地历史批量上传（后端按 request_json 去重） */
export async function syncLocalHistoryToCloud(): Promise<{ uploaded: number }> {
  if (!isHttpMode()) {
    return { uploaded: 0 }
  }
  const records = await listHistory()
  let uploaded = 0
  for (const record of records) {
    const res = await fetch('/api/v1/records', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: JSON.stringify(record.request),
    })
    if (res.ok) {
      uploaded += 1
    }
  }
  return { uploaded }
}

interface CloudRecordDto {
  id: number
  request: HistoryRecord['request']
  result: HistoryRecord['result']
  createdAt: string
}

export async function listCloudRecords(): Promise<HistoryRecord[]> {
  if (!isHttpMode()) {
    return []
  }
  const res = await fetch('/api/v1/records', { headers: authHeaders() })
  if (!res.ok) {
    throw new Error('获取云端记录失败')
  }
  const records = (await res.json()) as CloudRecordDto[]
  return records.map((r) => ({ id: r.id, request: r.request, result: r.result, createdAt: r.createdAt }))
}

export async function deleteCloudRecord(id: number): Promise<void> {
  if (!isHttpMode()) {
    return
  }
  await fetch(`/api/v1/records/${id}`, { method: 'DELETE', headers: authHeaders() })
}

/** 清空当前用户云端全部记录 */
export async function clearCloudRecords(): Promise<void> {
  if (!isHttpMode()) {
    return
  }
  await fetch('/api/v1/records', { method: 'DELETE', headers: authHeaders() })
}
