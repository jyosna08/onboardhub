import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CreateUserForm from './CreateUserForm.jsx'
import UserTable from './UserTable.jsx'
import { createUser } from '@/api/userApi.js'
import { ROLES } from '@/utils/constants.js'

vi.mock('@/api/userApi.js', () => ({ createUser: vi.fn() }))

function fillForm() {
  fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Ravi Kumar' } })
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'ravi@example.test' } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'Password@123' } })
}

describe('admin components', () => {
  beforeEach(() => vi.clearAllMocks())
  it('validates form fields before sending a request', async () => {
    render(<CreateUserForm/>);
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByText('Full name is required.')).toBeInTheDocument()
    expect(createUser).not.toHaveBeenCalled()
  })
  it('shows a duplicate email error', async () => {
    createUser.mockRejectedValue({ response: { status: 409 } })
    render(<CreateUserForm/>); fillForm()
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByText('An account with this email already exists.')).toBeInTheDocument()
  })
  it('creates once on repeated submit and clears fields', async () => {
    createUser.mockResolvedValue({ id: 3, fullName: 'Ravi Kumar', email: 'ravi@example.test', role: ROLES.HR })
    const onCreated = vi.fn()
    render(<CreateUserForm onCreated={onCreated}/>); fillForm()
    const form = screen.getByRole('button', { name: 'Create account' }).closest('form')
    fireEvent.submit(form); fireEvent.submit(form)
    await waitFor(() => expect(onCreated).toHaveBeenCalledTimes(1))
    expect(createUser).toHaveBeenCalledTimes(1)
    expect(screen.getByLabelText('Full name')).toHaveValue('')
    expect(screen.getByLabelText('Email')).toHaveValue('')
  })
  it('renders avatars and orders users newest first', () => {
    render(<UserTable users={[{ id: 1, fullName: 'Older User', email: 'old@example.test', role: ROLES.IT, createdAt: '2025-01-01' }, { id: 2, fullName: 'Newer User', email: 'new@example.test', role: ROLES.HR, createdAt: '2026-01-01' }]}/>);
    expect(screen.getAllByText('Newer User')[0].compareDocumentPosition(screen.getAllByText('Older User')[0]) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(screen.getAllByLabelText(/initials/).length).toBeGreaterThan(0)
  })
})
