import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import AddEmployeeModal from './AddEmployeeModal.jsx'
import { createEmployee } from '@/api/employeeApi.js'
import { ASSET_TYPE } from '@/utils/constants.js'

vi.mock('@/api/employeeApi.js', () => ({ createEmployee: vi.fn() }))

function fillForm() {
  fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Asha Rao' } })
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'asha@example.com' } })
  fireEvent.change(screen.getByLabelText('Department'), { target: { value: 'ENGINEERING' } })
  fireEvent.change(screen.getByLabelText('Job title'), { target: { value: 'Engineer' } })
  fireEvent.change(screen.getByLabelText('Joining date'), { target: { value: '2026-12-02' } })
}

describe('AddEmployeeModal', () => {
  beforeEach(() => vi.clearAllMocks())

  it('validates required fields before submitting', async () => {
    render(<AddEmployeeModal isOpen onClose={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button', { name: 'Create employee' }))
    expect(await screen.findByText('Full name is required.')).toBeInTheDocument()
    expect(createEmployee).not.toHaveBeenCalled()
  })

  it('preselects LAPTOP, creates once on repeated clicks and calls onCreated', async () => {
    const created = { id: 17 }
    createEmployee.mockResolvedValue(created)
    const onCreated = vi.fn()
    render(<AddEmployeeModal isOpen onClose={vi.fn()} onCreated={onCreated}/>);
    expect(screen.getByRole('button', { name: 'Laptop' })).toHaveAttribute('aria-pressed', 'true')
    fillForm()
    const form = screen.getByRole('button', { name: /Create employee/ }).closest('form')
    fireEvent.submit(form)
    fireEvent.submit(form)
    await waitFor(() => expect(onCreated).toHaveBeenCalledWith(created))
    expect(createEmployee).toHaveBeenCalledTimes(1)
    expect(createEmployee).toHaveBeenCalledWith(expect.objectContaining({ requiredAssetTypes: [ASSET_TYPE.LAPTOP] }))
  })

  it('shows duplicate email errors under the email field', async () => {
    createEmployee.mockRejectedValue({ response: { status: 409 } })
    render(<AddEmployeeModal isOpen onClose={vi.fn()}/>);
    fillForm()
    fireEvent.click(screen.getByRole('button', { name: 'Create employee' }))
    expect(await screen.findByText('An employee with this email already exists.')).toBeInTheDocument()
  })
})
