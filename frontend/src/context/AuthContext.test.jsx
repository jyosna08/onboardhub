import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi.js'
import { AuthProvider } from './AuthContext.jsx'
import { clearSession, getSession, saveSession } from '@/utils/authStorage.js'
import { useAuth } from '@/hooks/useAuth.js'

vi.mock('@/api/authApi.js', () => ({ login: vi.fn(), register: vi.fn() }))

function Consumer() {
  const { user, isAuthenticated, logout } = useAuth()
  return <div><span>{isAuthenticated ? user?.fullName : 'Signed out'}</span><button onClick={logout}>Log out</button></div>
}
function renderProvider() { return render(<AuthProvider><Consumer /></AuthProvider>) }
function token(exp) { return `header.${btoa(JSON.stringify({ exp }))}.signature` }

describe('AuthContext', () => {
  beforeEach(() => { clearSession(); authApi.login.mockReset(); authApi.register.mockReset() })
  it('starts logged out and clears expired stored sessions', () => {
    saveSession({ token: token(1), userId: 1, fullName: 'Expired', role: 'HR' })
    renderProvider()
    expect(screen.getByText('Signed out')).toBeInTheDocument()
    expect(getSession()).toBeNull()
  })
  it('logout clears the session storage', async () => {
    saveSession({ token: token(4102444800), userId: 1, fullName: 'Ravi', role: 'HR' })
    renderProvider()
    await userEvent.click(screen.getByRole('button', { name: 'Log out' }))
    expect(screen.getByText('Signed out')).toBeInTheDocument()
    expect(getSession()).toBeNull()
  })
})
