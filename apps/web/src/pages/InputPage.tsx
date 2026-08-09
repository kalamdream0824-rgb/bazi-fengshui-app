import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/Button'
import { Card, CardTitle } from '@/components/Card'
import { DateTimePicker } from '@/components/DateTimePicker'
import { FooterNote } from '@/components/FooterNote'
import { SegControl } from '@/components/SegControl'
import { RegionSelect } from '@/components/RegionSelect'
import { Switch } from '@/components/Switch'
import { TopBar } from '@/components/TopBar'
import { getBaziApi } from '@/services/baziApi'
import { addHistory } from '@/services/historyStore'
import { useBaziStore } from '@/store/useBaziStore'
import { useSettingsStore } from '@/store/useSettingsStore'
import { useToastStore } from '@/store/useToastStore'
import type { Gender, PaipanRequest } from '@/types/bazi'

export function InputPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const setResult = useBaziStore((s) => s.setResult)
  const toast = useToastStore((s) => s.show)
  const trueSolarDefault = useSettingsStore((s) => s.trueSolarDefault)
  const api = getBaziApi()

  const [name, setName] = useState('')
  const [gender, setGender] = useState<Gender>('male')
  const [datetime, setDatetime] = useState('1995-10-08T14:30')
  const [birthPlace, setBirthPlace] = useState('')
  const [trueSolar, setTrueSolar] = useState(trueSolarDefault)

  const mutation = useMutation({
    mutationFn: (req: PaipanRequest) => api.paipan(req),
    onSuccess: (result, req) => {
      setResult(req, result)
      addHistory(req, result).catch(() => undefined)
      void queryClient.invalidateQueries({ queryKey: ['history'] })
      navigate('/chart')
    },
    onError: (err: Error) => toast(err.message || '排盘失败，请重试'),
  })

  const submit = () => {
    if (!datetime) {
      toast('请选择出生时间')
      return
    }
    if (trueSolar && !birthPlace) {
      toast('开启真太阳时请先选择出生地（省/市）')
      return
    }
    mutation.mutate({
      name: name.trim() || undefined,
      gender,
      solarDateTime: datetime,
      birthPlace: birthPlace.trim() || undefined,
      trueSolarTime: trueSolar,
    })
  }

  return (
    <>
      <TopBar title="排盘输入" />

      <Card>
        <CardTitle>基本信息</CardTitle>
        <div className="field">
          <div className="lbl">姓名（选填）</div>
          <input
            className="input-box"
            value={name}
            placeholder="请输入姓名"
            onChange={(e) => setName(e.target.value)}
          />
        </div>
        <div className="field">
          <div className="lbl">性别</div>
          <SegControl
            two
            options={[
              { label: '男 · 乾造', value: 'male' },
              { label: '女 · 坤造', value: 'female' },
            ]}
            value={gender}
            onChange={(v) => setGender(v as Gender)}
          />
        </div>
      </Card>

      <Card>
        <CardTitle>出生时间</CardTitle>
        <div className="field">
          <div className="lbl">公历出生日期与时间</div>
          <DateTimePicker value={datetime} onChange={setDatetime} />
        </div>
        <div className="field">
          <div className="note">出生时间越精确，排盘与运势推算越准。</div>
        </div>
      </Card>

      <Card>
        <CardTitle hint="用于真太阳时">出生地</CardTitle>
        <div className="field">
          <div className="lbl">省 / 市</div>
          <RegionSelect value={birthPlace} onChange={setBirthPlace} />
        </div>
        <Switch
          checked={trueSolar}
          onChange={setTrueSolar}
          title="真太阳时"
          desc="按出生地经度校正，结果可能与本地时间不同"
        />
      </Card>

      <div style={{ padding: '0 14px 12px' }}>
        <Button variant="primary" block onClick={submit} disabled={mutation.isPending}>
          {mutation.isPending ? '排盘中…' : '开始排盘'}
        </Button>
      </div>
      <FooterNote>排盘数据仅供传统文化研究参考</FooterNote>
    </>
  )
}
