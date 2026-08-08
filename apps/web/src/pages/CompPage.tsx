import { TopBar } from '@/components/TopBar'

export function CompPage() {
  return (
    <>
      <TopBar title="八字合婚" />
      <div className="placeholder">
        合婚模块开发中（M0.8）
        <br />
        将支持双人四柱对比、五行互补、冲合分析
      </div>
      <div className="footer-note">合婚结论以实际算法输出为准</div>
    </>
  )
}
