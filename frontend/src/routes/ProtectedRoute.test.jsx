import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { AuthProvider } from '@/context/AuthContext.jsx'
import { saveSession, clearSession } from '@/utils/authStorage.js'
import { ROLES } from '@/utils/constants.js'
import ProtectedRoute from './ProtectedRoute.jsx'

function makeToken(exp = 4102444800) {
  const body = btoa(JSON.stringify({ exp })).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')
  return `header.${body}.signature`
}

function renderRoute(path) {
  return render(<MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={[path]}><AuthProvider><Routes>
    <Route path="/login" element={<p>Login screen</p>} />
    <Route path="/dashboard" element={<ProtectedRoute allowedRoles={[ROLES.HR]}><p>Dashboard screen</p></ProtectedRoute>} />
    <Route path="/my-onboarding" element={<p>Employee home</p>} />
  </Routes></AuthProvider></MemoryRouter>)
}

describe('ProtectedRoute', () => {
  beforeEach(() => clearSession())
  it('redirects a logged-out visitor to login', () => {
    renderRoute('/dashboard')
    expect(screen.getByText('Login screen')).toBeInTheDocument()
  })
  it('redirects a wrong role to its own home', () => {
    saveSession({ token: makeToken(), role: ROLES.EMPLOYEE, fullName: 'Asha', userId: 4 })
    renderRoute('/dashboard')
    expect(screen.getByText('Employee home')).toBeInTheDocument()
  })
  it('renders the route for an allowed role', () => {
    saveSession({ token: makeToken(), role: ROLES.HR, fullName: 'Ravi', userId: 5 })
    renderRoute('/dashboard')
    expect(screen.getByText('Dashboard screen')).toBeInTheDocument()
  })
})
