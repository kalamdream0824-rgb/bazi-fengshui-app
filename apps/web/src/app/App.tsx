import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom'
import { TabBar } from '@/components/TabBar'
import { Toast } from '@/components/Toast'
import { ChartPage } from '@/pages/ChartPage'
import { CompPage } from '@/pages/CompPage'
import { DailyPage } from '@/pages/DailyPage'
import { HomePage } from '@/pages/HomePage'
import { HistoryPage } from '@/pages/HistoryPage'
import { InputPage } from '@/pages/InputPage'
import { ProfilePage } from '@/pages/ProfilePage'
import { ProPage } from '@/pages/ProPage'
import { ReportPage } from '@/pages/ReportPage'

const TAB_PATHS = ['/', '/input', '/profile']

function Shell() {
  const { pathname } = useLocation()
  const showTab = TAB_PATHS.includes(pathname)
  return (
    <div className="app-shell">
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/input" element={<InputPage />} />
        <Route path="/history" element={<HistoryPage />} />
        <Route path="/chart" element={<ChartPage />} />
        <Route path="/chart/pro" element={<ProPage />} />
        <Route path="/comp" element={<CompPage />} />
        <Route path="/daily" element={<DailyPage />} />
        <Route path="/report" element={<ReportPage />} />
        <Route path="/profile" element={<ProfilePage />} />
        <Route path="*" element={<HomePage />} />
      </Routes>
      {showTab ? <TabBar /> : null}
      <Toast />
    </div>
  )
}

export default function App() {
  return (
    <BrowserRouter>
      <Shell />
    </BrowserRouter>
  )
}
