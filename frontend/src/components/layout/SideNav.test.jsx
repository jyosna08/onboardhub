import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it } from 'vitest'
import { AuthProvider } from '@/context/AuthContext.jsx'
import { clearSession, saveSession } from '@/utils/authStorage.js'
import { ROLES } from '@/utils/constants.js'
import SideNav from './SideNav.jsx'

const token = `header.${btoa(JSON.stringify({ exp: 4102444800 })).replace(/=/g, '')}.signature`

function renderNav(role) {
  saveSession({ token, role, userId: 1, fullName: 'Test Person' })
  return render(<MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}><AuthProvider><SideNav open /></AuthProvider></MemoryRouter>)
}

describe('SideNav', () => {
  beforeEach(() => clearSession())
  it('shows management links for HR and IT and Team only for ADMIN', () => {
    const { unmount } = renderNav(ROLES.HR)
    expect(screen.getByRole('link', { name: 'Dashboard' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Employees' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Assets' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Team' })).not.toBeInTheDocument()
    unmount()
    renderNav(ROLES.ADMIN)
    expect(screen.getByRole('link', { name: 'Team' })).toBeInTheDocument()
  })
  it('shows only My onboarding for employees', () => {
    renderNav(ROLES.EMPLOYEE)
    expect(screen.getByRole('link', { name: 'My onboarding' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Assets' })).not.toBeInTheDocument()
  })
})
