import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi.js'
import { AuthProvider } from '@/context/AuthContext.jsx'
import { clearSession } from '@/utils/authStorage.js'
import LoginForm from './LoginForm.jsx'

vi.mock('@/api/authApi.js', () => ({ login: vi.fn(), register: vi.fn() }))

function CurrentPath() { const location = useLocation(); return <output>{location.pathname}</output> }
function LoginHarness() {
  return <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/login']}><AuthProvider><Routes><Route path="*" element={<><LoginForm /><CurrentPath /></>} /></Routes></AuthProvider></MemoryRouter>
}

describe('LoginForm', () => {
  beforeEach(() => { clearSession(); authApi.login.mockReset() })
  it('shows validation errors and accessible field state', async () => {
    render(<LoginHarness />)
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }))
    expect(screen.getByText('Email is required.')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByLabelText('Password')).toHaveAttribute('aria-describedby', 'login-password-error')
  })
  it('redirects to the signed-in role home after success', async () => {
    authApi.login.mockResolvedValue({ token: 'test-token', userId: 8, fullName: 'Ravi HR', role: 'HR' })
    const user = userEvent.setup(); render(<LoginHarness />)
    await user.type(screen.getByLabelText('Email'), 'ravi@example.com')
    await user.type(screen.getByLabelText('Password'), 'Password@123')
    await user.click(screen.getByRole('button', { name: 'Log in' }))
    expect(await screen.findByText('/dashboard')).toBeInTheDocument()
  })
  it('shows a friendly credential banner for 401', async () => {
    authApi.login.mockRejectedValue({ response: { status: 401, data: { message: 'Unauthorized' } } })
    const user = userEvent.setup(); render(<LoginHarness />)
    await user.type(screen.getByLabelText('Email'), 'ravi@example.com')
    await user.type(screen.getByLabelText('Password'), 'Password@123')
    await user.click(screen.getByRole('button', { name: 'Log in' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password.')
  })
})
