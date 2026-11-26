import { describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Avatar from '@/components/common/Avatar.jsx'
import WelcomeBanner from '@/components/dashboard/WelcomeBanner.jsx'
import AssetStockStrip from '@/components/dashboard/AssetStockStrip.jsx'
import EmployeeTable from './EmployeeTable.jsx'
import { ROLES, TASK_CATEGORY, TASK_STATUS, OWNER_ROLE, EMPLOYEE_STATUS, ASSET_TYPE } from '@/utils/constants.js'
import TaskRow from '@/components/onboarding/TaskRow.jsx'

vi.mock('@/hooks/useAuth.js', () => ({ useAuth: () => ({ user: { role: ROLES.HR } }) }))

describe('employee experience components', () => {
  it('uses deterministic avatar colours and initials', () => {
    const { container, rerender } = render(<Avatar name="Ada Lovelace" />)
    const original = container.firstChild.className
    expect(container.firstChild).toHaveTextContent('AL')
    rerender(<Avatar name="Ada Lovelace" />)
    expect(container.firstChild.className).toBe(original)
  })

  it.each([[9, 'Good morning'], [13, 'Good afternoon'], [19, 'Good evening']])('greets by local hour %i', (hour, greeting) => {
    render(<WelcomeBanner user={{ fullName: 'Ada Lovelace', role: ROLES.HR }} now={new Date(2026, 0, 1, hour)} />)
    expect(screen.getByRole('heading', { name: new RegExp(greeting) })).toBeInTheDocument()
  })

  it('always shows all six asset categories', () => {
    render(<AssetStockStrip summary={{ byType: [], total: 0 }} />)
    expect(screen.getByText('Laptop')).toBeInTheDocument()
    expect(screen.getByText('Monitor')).toBeInTheDocument()
    expect(screen.getByText('Keyboard')).toBeInTheDocument()
    expect(screen.getByText('Mouse')).toBeInTheDocument()
    expect(screen.getByText('Headset')).toBeInTheDocument()
    expect(screen.getByText('Phone')).toBeInTheDocument()
  })

  it('renders employee links with status, avatar and progress', () => {
    render(<MemoryRouter><EmployeeTable employees={[{ id: 'e1', fullName: 'Ada Lovelace', email: 'ada@example.test', employeeCode: 'E-1', department: 'ENGINEERING', jobTitle: 'Engineer', joiningDate: '2026-01-02', status: EMPLOYEE_STATUS.ONBOARDING, tasksDone: 2, tasksTotal: 4 }]} /></MemoryRouter>)
    expect(screen.getAllByRole('link', { name: /Ada Lovelace/ })[0]).toHaveAttribute('href', '/employees/e1')
    expect(screen.getAllByText('Onboarding').length).toBeGreaterThan(0)
    expect(screen.getAllByText('2/4').length).toBeGreaterThan(0)
  })

  it('allows HR owners to act on non-equipment TODO tasks', () => {
    const action = vi.fn()
    render(<TaskRow task={{ id: 't1', title: 'Collect documents', ownerRole: OWNER_ROLE.HR, category: TASK_CATEGORY.DOCUMENTATION, status: TASK_STATUS.TODO, dueDate: '2026-01-01' }} onAction={action} />)
    fireEvent.click(screen.getByRole('button', { name: 'Start' }))
    expect(action).toHaveBeenCalledWith(expect.objectContaining({ id: 't1' }), TASK_STATUS.IN_PROGRESS)
    expect(screen.getByRole('button', { name: 'Mark done' })).toBeInTheDocument()
  })

  it('never offers manual completion for equipment tasks', () => {
    render(<TaskRow task={{ id: 't2', title: 'Assign laptop', ownerRole: OWNER_ROLE.HR, category: TASK_CATEGORY.EQUIPMENT, assetType: ASSET_TYPE.LAPTOP, status: TASK_STATUS.TODO, dueDate: '2026-01-01' }} />)
    expect(screen.queryByRole('button', { name: 'Mark done' })).not.toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Assign asset' })).toBeInTheDocument()
  })
})
