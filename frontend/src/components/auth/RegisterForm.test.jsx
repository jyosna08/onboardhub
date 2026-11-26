import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as authApi from '@/api/authApi.js'
import { AuthProvider } from '@/context/AuthContext.jsx'
import { clearSession } from '@/utils/authStorage.js'
import RegisterForm from './RegisterForm.jsx'

vi.mock('@/api/authApi.js', () => ({ login: vi.fn(), register: vi.fn() }))

function CurrentPath() { const location = useLocation(); return <output>{location.pathname}</output> }
function RegisterHarness() {
  return <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }} initialEntries={['/register']}><AuthProvider><Routes><Route path="*" element={<><RegisterForm /><CurrentPath /></>} /></Routes></AuthProvider></MemoryRouter>
}

describe('RegisterForm', () => {
  beforeEach(() => { clearSession(); authApi.register.mockReset(); authApi.login.mockReset() })
  it('registers with only the three public fields then redirects to employee home', async () => {
    authApi.register.mockResolvedValue({ id: 4 })
    authApi.login.mockResolvedValue({ token: 'test-token', userId: 4, fullName: 'Asha Rao', role: 'EMPLOYEE' })
    const user = userEvent.setup(); render(<RegisterHarness />)
    await user.type(screen.getByLabelText('Full name'), 'Asha Rao')
    await user.type(screen.getByLabelText('Email'), 'asha@example.com')
    await user.type(screen.getByLabelText('Password'), 'Password@123')
    expect(screen.getByText('HR and IT accounts are created by an administrator.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByText('/my-onboarding')).toBeInTheDocument()
    expect(authApi.register).toHaveBeenCalledWith({ fullName: 'Asha Rao', email: 'asha@example.com', password: 'Password@123' })
  })
  it('shows a duplicate email error under the email field', async () => {
    authApi.register.mockRejectedValue({ response: { status: 409, data: {} } })
    const user = userEvent.setup(); render(<RegisterHarness />)
    await user.type(screen.getByLabelText('Full name'), 'Asha Rao')
    await user.type(screen.getByLabelText('Email'), 'asha@example.com')
    await user.type(screen.getByLabelText('Password'), 'Password@123')
    await user.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByText('An account with this email already exists.')).toBeInTheDocument()
    expect(authApi.login).not.toHaveBeenCalled()
  })
})
