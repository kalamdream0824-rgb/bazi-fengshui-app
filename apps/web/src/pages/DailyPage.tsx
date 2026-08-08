import { TopBar } from '@/components/TopBar'

export function DailyPage() {
  return (
    <>
      <TopBar title="每日运势" />
      <div className="placeholder">
        每日运势模块开发中（M0.8）
        <br />
        将支持当日干支、宜忌、幸运色与方位
      </div>
      <div className="footer-note">运势内容以实际算法输出为准</div>
    </>
  )
}
