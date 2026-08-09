import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/Button'
import { Card, CardTitle } from '@/components/Card'
import { FooterNote } from '@/components/FooterNote'
import { SegControl } from '@/components/SegControl'
import { TopBar } from '@/components/TopBar'
import { login, register } from '@/services/authApi'
import { syncLocalHistoryToCloud } from '@/services/cloudSync'
import { useAuthStore } from '@/store/useAuthStore'
import { useToastStore } from '@/store/useToastStore'

type Mode = 'login' | 'register'

export function AuthPage() {
  const navigate = useNavigate()
  const toast = useToastStore((s) => s.show)
  const setAuth = useAuthStore((s) => s.setAuth)
  const [mode, setMode] = useState<Mode>('login')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const submit = async () => {
    if (!username.trim() || password.length < 6) {
      toast('请输入用户名和至少 6 位密码')
      return
    }
    setSubmitting(true)
    try {
      const res = mode === 'login' ? await login(username.trim(), password) : await register(username.trim(), password)
      setAuth(res.token, res.username)
      const sync = await syncLocalHistoryToCloud()
      toast(mode === 'login' ? `欢迎回来，${res.username}` : `注册成功，${res.username}（已同步 ${sync.uploaded} 条本地记录）`)
      navigate('/profile')
    } catch (err) {
      toast((err as Error).message || '操作失败，请重试')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <>
      <TopBar title="账号" />

      <Card>
        <CardTitle hint="本地排盘记录将在登录后自动同步">登录 / 注册</CardTitle>
        <SegControl
          two
          options={[
            { label: '登录', value: 'login' },
            { label: '注册', value: 'register' },
          ]}
          value={mode}
          onChange={(v) => setMode(v as Mode)}
        />
        <div className="field">
          <div className="lbl">用户名</div>
          <input className="input-box" value={username} onChange={(e) => setUsername(e.target.value)} placeholder="请输入用户名" />
        </div>
        <div className="field">
          <div className="lbl">密码（至少 6 位）</div>
          <input className="input-box" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="请输入密码" />
        </div>
        <div style={{ padding: '0 16px 14px' }}>
          <Button variant="primary" block onClick={submit} disabled={submitting}>
            {submitting ? '处理中…' : mode === 'login' ? '登录' : '注册'}
          </Button>
        </div>
      </Card>
      <FooterNote>账号用于云端同步排盘记录，仅供研究参考</FooterNote>
    </>
  )
}
