import { Card, CardTitle } from '@/components/Card'
import { FooterNote } from '@/components/FooterNote'
import { TopBar } from '@/components/TopBar'

export function SettingsPage() {
  return (
    <>
      <TopBar title="设置" />

      <Card>
        <CardTitle hint="八字排盘 · 文化参考工具">关于与合规</CardTitle>
        <div className="about-body">
          <p>
            「八字排盘」以传统命理框架呈现四柱、十神、大运流年等排盘信息，界面遵循「朱墨星图」视觉——宣纸为底、朱砂点睛。
          </p>
          <p>排盘数据基于农历与干支历法计算，解读均以传统命理经典为参考口径，不作科学验证主张。</p>
        </div>
      </Card>

      <Card>
        <CardTitle hint="温和参考 · 不绝对预测">解读口径</CardTitle>
        <div className="about-body">
          <p>页面中的性格倾向、运势与民俗宜忌，均为传统命理框架下的文化参考，仅供理解传统文化之用。</p>
          <p>我们不输出「一定」「必会」等确定性断言，也不提供医疗、投资、婚姻等领域的决策建议。</p>
        </div>
      </Card>

      <Card>
        <CardTitle hint="请知悉">免责声明</CardTitle>
        <div className="about-body">
          <p>本应用内容基于传统命理文化整理，仅供娱乐与参考，不构成任何人生、医疗、投资或法律建议。请理性看待，切勿沉迷或据此作出重大决策。</p>
        </div>
      </Card>

      <Card>
        <CardTitle hint="数据说明">数据与隐私</CardTitle>
        <div className="about-body">
          <p>排盘记录默认保存在本机；登录账号后可在云端同步，账号数据相互隔离。退出登录会移除本机记录，云端记录仍归属账号。</p>
        </div>
      </Card>

      <FooterNote>版本 v0.36 · 2026-08-16 · 传统文化研究参考</FooterNote>
    </>
  )
}
