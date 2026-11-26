import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from '@/components/layout/AppLayout.jsx'
import LoginPage from '@/pages/LoginPage.jsx'
import RegisterPage from '@/pages/RegisterPage.jsx'
import DashboardPage from '@/pages/DashboardPage.jsx'
import EmployeesPage from '@/pages/EmployeesPage.jsx'
import EmployeeDetailPage from '@/pages/EmployeeDetailPage.jsx'
import AssetsPage from '@/pages/AssetsPage.jsx'
import TeamPage from '@/pages/TeamPage.jsx'
import MyOnboardingPage from '@/pages/MyOnboardingPage.jsx'
import NotFoundPage from '@/pages/NotFoundPage.jsx'
import ProtectedRoute from './ProtectedRoute.jsx'
import PublicOnlyRoute from './PublicOnlyRoute.jsx'
import { useAuth } from '@/hooks/useAuth.js'
import { ROLES } from '@/utils/constants.js'
import { getHomePathForRole } from '@/utils/roleRoutes.js'

const staffRoles = [ROLES.HR, ROLES.IT, ROLES.ADMIN]

function HomeRedirect() {
  const { user } = useAuth()
  return <Navigate to={getHomePathForRole(user?.role)} replace />
}

export default function AppRoutes() {
  return <Routes>
    <Route element={<PublicOnlyRoute />}><Route path="/login" element={<LoginPage />} /><Route path="/register" element={<RegisterPage />} /></Route>
    <Route element={<ProtectedRoute />}><Route element={<AppLayout />}>
      <Route path="/dashboard" element={<ProtectedRoute allowedRoles={staffRoles}><DashboardPage /></ProtectedRoute>} />
      <Route path="/employees" element={<ProtectedRoute allowedRoles={staffRoles}><EmployeesPage /></ProtectedRoute>} />
      <Route path="/employees/:id" element={<ProtectedRoute allowedRoles={staffRoles}><EmployeeDetailPage /></ProtectedRoute>} />
      <Route path="/assets" element={<ProtectedRoute allowedRoles={staffRoles}><AssetsPage /></ProtectedRoute>} />
      <Route path="/admin/team" element={<ProtectedRoute allowedRoles={[ROLES.ADMIN]}><TeamPage /></ProtectedRoute>} />
      <Route path="/my-onboarding" element={<ProtectedRoute allowedRoles={[ROLES.EMPLOYEE]}><MyOnboardingPage /></ProtectedRoute>} />
    </Route></Route>
    <Route path="/" element={<ProtectedRoute><HomeRedirect /></ProtectedRoute>} />
    <Route path="*" element={<NotFoundPage />} />
  </Routes>
}
