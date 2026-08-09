import { useRef, useState, type ChangeEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button, ButtonRow } from '@/components/Button'
import { Card, CardTitle } from '@/components/Card'
import { TopBar } from '@/components/TopBar'
import { useHistory } from '@/hooks/useHistory'
import { backupToRecords, downloadBackup, importHistory, removeHistory } from '@/services/historyStore'
import { useBaziStore } from '@/store/useBaziStore'
import { useToastStore } from '@/store/useToastStore'
import type { PaipanRequest, PaipanResult } from '@/types/bazi'

export function HistoryPage() {
  const navigate = useNavigate()
  const toast = useToastStore((s) => s.show)
  const setResult = useBaziStore((s) => s.setResult)
  const { data: records = [], isLoading, invalidate } = useHistory()
  const fileRef = useRef<HTMLInputElement>(null)
  const [importing, setImporting] = useState(false)

  const openRecord = (request: PaipanRequest, result: PaipanResult) => {
    setResult(request, result)
    navigate('/chart')
  }

  const handleDelete = async (id: number) => {
    if (!window.confirm('确定删除这条排盘记录？')) return
    await removeHistory(id)
    invalidate()
  }

  const handleExport = () => {
    if (records.length === 0) {
      toast('暂无记录可导出')
      return
    }
    downloadBackup(records)
  }

  const handleImportFile = async (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file) return
    setImporting(true)
    try {
      const text = await file.text()
      const records = backupToRecords(JSON.parse(text))
      const res = await importHistory(records)
      invalidate()
      toast(`导入完成：新增 ${res.added} 条，跳过重复 ${res.skipped} 条`)
    } catch {
      toast('导入失败：备份文件格式不正确')
    } finally {
      setImporting(false)
    }
  }

  return (
    <>
      <TopBar title="历史记录" />

      {isLoading ? (
        <div className="placeholder">加载中…</div>
      ) : records.length === 0 ? (
        <>
          <div className="placeholder">
            暂无排盘记录
            <br />
            <Link to="/input" style={{ color: 'var(--red)' }}>去排一次盘</Link>
          </div>
        </>
      ) : (
        <Card>
          <CardTitle hint={`共 ${records.length} 条`}>排盘记录</CardTitle>
          {records.map((record) => (
            <div className="row" key={record.id} onClick={() => openRecord(record.request, record.result)}>
              <div style={{ minWidth: 0 }}>
                <div style={{ fontWeight: 600 }}>{record.result.lunarText}</div>
                <div className="meta-line" style={{ marginTop: 3 }}>
                  {record.result.solarText} · 生肖 {record.result.shengXiao} ·{' '}
                  {new Date(record.createdAt).toLocaleString('zh-CN', { month: 'numeric', day: 'numeric', hour: '2-digit', minute: '2-digit' })}
                </div>
              </div>
              <button
                type="button"
                aria-label="删除记录"
                onClick={(e) => {
                  e.stopPropagation()
                  void handleDelete(record.id as number)
                }}
                style={{ border: '1px solid var(--line)', background: 'var(--card)', borderRadius: 8, width: 30, height: 30, color: 'var(--ink-3)' }}
              >
                ×
              </button>
            </div>
          ))}
        </Card>
      )}

      {records.length > 0 && (
        <ButtonRow>
          <Button variant="primary" onClick={handleExport}>导出全部</Button>
          <Button onClick={() => fileRef.current?.click()} disabled={importing}>
            {importing ? '导入中…' : '导入备份'}
          </Button>
          <Button onClick={() => navigate('/input')}>去排盘</Button>
        </ButtonRow>
      )}
      <input ref={fileRef} type="file" accept="application/json,.json" style={{ display: 'none' }} onChange={handleImportFile} />
      <div className="footer-note">本地历史仅保存在当前浏览器，建议定期导出备份</div>
    </>
  )
}
