import { beforeEach, describe, expect, it, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import AppRoutes from './AppRoutes.jsx'
import { AuthProvider } from '@/context/AuthContext.jsx'
import { clearSession, saveSession } from '@/utils/authStorage.js'
import { ROLES } from '@/utils/constants.js'

vi.mock('@/pages/DashboardPage.jsx', () => ({ default: () => 'Dashboard route content' }))
vi.mock('@/pages/EmployeesPage.jsx', () => ({ default: () => 'Employees route content' }))
vi.mock('@/pages/EmployeeDetailPage.jsx', () => ({ default: () => 'Employee detail route content' }))
vi.mock('@/pages/AssetsPage.jsx', () => ({ default: () => 'Assets route content' }))
vi.mock('@/pages/TeamPage.jsx', () => ({ default: () => 'Admin team route content' }))
vi.mock('@/pages/MyOnboardingPage.jsx', () => ({ default: () => 'My onboarding route content' }))

function token() {
  const payload = btoa(JSON.stringify({ exp: 4102444800 })).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')
  return `header.${payload}.signature`
}
function renderPath(path, role) {
  saveSession({ token: token(), userId: '1', fullName: 'Test User', role })
  return render(<MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={[path]}><AuthProvider><AppRoutes/></AuthProvider></MemoryRouter>)
}

describe('application route access', () => {
  beforeEach(() => clearSession())
  it.each(['/dashboard', '/employees', '/assets', '/admin/team'])('redirects an employee away from %s to their own home', (path) => {
    renderPath(path, ROLES.EMPLOYEE)
    expect(screen.getByText('My onboarding route content')).toBeInTheDocument()
  })
  it('allows only ADMIN to open the team route', () => {
    renderPath('/admin/team', ROLES.HR)
    expect(screen.getByText('Dashboard route content')).toBeInTheDocument()
  })
  it('renders the admin team route for ADMIN', () => {
    renderPath('/admin/team', ROLES.ADMIN)
    expect(screen.getByText('Admin team route content')).toBeInTheDocument()
  })
  it('allows HR to open the assets route', () => {
    renderPath('/assets', ROLES.HR)
    expect(screen.getByText('Assets route content')).toBeInTheDocument()
  })
})
