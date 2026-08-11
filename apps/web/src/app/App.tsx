import { lazy, Suspense } from 'react'
import { BrowserRouter, Route, Routes, useLocation } from 'react-router-dom'
import { TabBar } from '@/components/TabBar'
import { Toast } from '@/components/Toast'

const HomePage = lazy(() => import('@/pages/HomePage').then((m) => ({ default: m.HomePage })))
const AuthPage = lazy(() => import('@/pages/AuthPage').then((m) => ({ default: m.AuthPage })))
const HistoryPage = lazy(() => import('@/pages/HistoryPage').then((m) => ({ default: m.HistoryPage })))
const InputPage = lazy(() => import('@/pages/InputPage').then((m) => ({ default: m.InputPage })))
const ChartPage = lazy(() => import('@/pages/ChartPage').then((m) => ({ default: m.ChartPage })))
const ProPage = lazy(() => import('@/pages/ProPage').then((m) => ({ default: m.ProPage })))
const CompPage = lazy(() => import('@/pages/CompPage').then((m) => ({ default: m.CompPage })))
const DayPickerPage = lazy(() => import('@/pages/DayPickerPage').then((m) => ({ default: m.DayPickerPage })))
const DailyPage = lazy(() => import('@/pages/DailyPage').then((m) => ({ default: m.DailyPage })))
const ReportPage = lazy(() => import('@/pages/ReportPage').then((m) => ({ default: m.ReportPage })))
const ProfilePage = lazy(() => import('@/pages/ProfilePage').then((m) => ({ default: m.ProfilePage })))
const MembershipPage = lazy(() => import('@/pages/MembershipPage').then((m) => ({ default: m.MembershipPage })))

const TAB_PATHS = ['/', '/input', '/profile', '/membership', '/history', '/report']

function Shell() {
  const { pathname } = useLocation()
  const showTab = TAB_PATHS.includes(pathname)
  return (
    <div className="app-shell">
      <Suspense fallback={<div className="placeholder">加载中…</div>}>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/auth" element={<AuthPage />} />
          <Route path="/input" element={<InputPage />} />
          <Route path="/history" element={<HistoryPage />} />
          <Route path="/chart" element={<ChartPage />} />
          <Route path="/chart/pro" element={<ProPage />} />
          <Route path="/comp" element={<CompPage />} />
          <Route path="/day-picker" element={<DayPickerPage />} />
          <Route path="/daily" element={<DailyPage />} />
          <Route path="/report" element={<ReportPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/membership" element={<MembershipPage />} />
          <Route path="*" element={<HomePage />} />
        </Routes>
      </Suspense>
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
