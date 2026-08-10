import { Button, ButtonRow } from '@/components/Button'
import { copyText, downloadImage, hasNativeShare, shareImageFile } from '@/lib/share'
import { useToastStore } from '@/store/useToastStore'

interface ShareSheetProps {
  open: boolean
  imageUrl: string
  text: string
  filename: string
  onClose: () => void
}

export function ShareSheet({ open, imageUrl, text, filename, onClose }: ShareSheetProps) {
  const toast = useToastStore((s) => s.show)
  if (!open) {
    return null
  }

  const handleSave = () => {
    downloadImage(imageUrl, filename)
    toast('图片已保存')
  }

  const handleCopy = async () => {
    toast((await copyText(text)) ? '文案已复制' : '复制失败，请重试')
  }

  const handleNative = async () => {
    if (await shareImageFile(imageUrl, filename)) {
      toast('已唤起系统分享')
    } else {
      toast('当前环境不支持分享图片，可保存后手动发送')
    }
  }

  return (
    <div className="picker-mask" onClick={onClose}>
      <div className="picker-panel" onClick={(e) => e.stopPropagation()}>
        <div className="picker-head">
          <button type="button" onClick={onClose}>关闭</button>
          <span>保存命盘 / 求解读</span>
          <span style={{ width: 28 }} />
        </div>
        <div style={{ padding: '10px 20px 4px' }}>
          <img className="share-img" src={imageUrl} alt="排盘分享卡片" />
          <div className="meta-line" style={{ textAlign: 'center', marginBottom: 12 }}>
            保存图片发给懂行的人求解读，或复制含生辰的文案
          </div>
        </div>
        <ButtonRow>
          <Button variant="primary" onClick={handleSave}>保存图片</Button>
          <Button onClick={handleCopy}>复制文案</Button>
          {hasNativeShare() ? <Button onClick={handleNative}>系统分享</Button> : null}
        </ButtonRow>
        <div style={{ padding: '0 14px 16px', textAlign: 'center', fontSize: 10, letterSpacing: 1, color: 'var(--ink-3)' }}>
          排盘结果仅供传统文化研究参考
        </div>
      </div>
    </div>
  )
}
