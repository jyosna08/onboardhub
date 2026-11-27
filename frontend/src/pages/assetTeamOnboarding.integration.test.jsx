import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AssetsPage from './AssetsPage.jsx'
import TeamPage from './TeamPage.jsx'
import MyOnboardingPage from './MyOnboardingPage.jsx'
import { ASSET_STATUS, ASSET_TYPE, OWNER_ROLE, ROLES, TASK_CATEGORY, TASK_STATUS } from '@/utils/constants.js'
import { createUser } from '@/api/userApi.js'

const state = vi.hoisted(() => ({ role: 'IT', assets: [], assetLoading: false, assetError: null, summary: { total: 0, byType: [] }, users: [], userLoading: false, userError: null, employee: null, employeeLoading: false, employeeError: null, refreshAssets: vi.fn(), refreshSummary: vi.fn(), refreshUsers: vi.fn(), refreshEmployee: vi.fn() }))
vi.mock('@/hooks/useAssets.js', () => ({ useAssets: () => ({ assets: state.assets, isLoading: state.assetLoading, error: state.assetError, refresh: state.refreshAssets }) }))
vi.mock('@/hooks/useAssetSummary.js', () => ({ useAssetSummary: () => ({ summary: state.summary, isLoading: false, error: null, refresh: state.refreshSummary }) }))
vi.mock('@/hooks/useUsers.js', () => ({ useUsers: () => ({ users: state.users, isLoading: state.userLoading, error: state.userError, refresh: state.refreshUsers }) }))
vi.mock('@/hooks/useMyEmployee.js', () => ({ useMyEmployee: () => ({ employee: state.employee, isLoading: state.employeeLoading, error: state.employeeError, refresh: state.refreshEmployee }) }))
vi.mock('@/hooks/useAuth.js', () => ({ useAuth: () => ({ user: { role: state.role, fullName: 'Preview User' } }) }))
vi.mock('@/hooks/usePolling.js', () => ({ usePolling: vi.fn() }))
vi.mock('@/api/userApi.js', () => ({ createUser: vi.fn() }))

describe('asset, team and onboarding pages', () => {
  beforeEach(() => {
    Object.assign(state, { role: ROLES.IT, assets: [{ id: 7, assetTag: 'LAP-0007', type: ASSET_TYPE.LAPTOP, make: 'Dell', model: 'Latitude', serialNumber: 'SN-7', status: ASSET_STATUS.AVAILABLE }], assetLoading: false, assetError: null, summary: { total: 1, byType: [] }, users: [{ id: 4, fullName: 'Ravi Kumar', email: 'ravi@example.test', role: ROLES.HR }], userLoading: false, userError: null, employee: { fullName: 'Asha Rao', tasks: [{ id: 1, title: 'Collect documents', category: TASK_CATEGORY.DOCUMENTATION, ownerRole: OWNER_ROLE.HR, status: TASK_STATUS.TODO, dueDate: '2026-12-01' }] }, employeeLoading: false, employeeError: null })
    vi.clearAllMocks()
  })
  it('loads asset inventory and allows IT to add assets', () => {
    render(<AssetsPage/>);
    expect(screen.getAllByText('LAP-0007').length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: 'Add asset' })).toBeInTheDocument()
  })
  it('shows error retry and keeps HR inventory read-only', () => {
    state.role = ROLES.HR
    state.assetError = new Error('offline')
    render(<AssetsPage/>);
    expect(screen.queryByRole('button', { name: 'Add asset' })).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(state.refreshAssets).toHaveBeenCalled()
  })
  it('loads users and announces successful account creation', async () => {
    createUser.mockResolvedValue({ id: 5, fullName: 'Tina IT', email: 'tina@example.test', role: ROLES.IT })
    render(<TeamPage/>);
    expect(screen.getAllByText('Ravi Kumar').length).toBeGreaterThan(0)
    fireEvent.change(screen.getByLabelText('Full name'), { target: { value: 'Tina IT' } })
    fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'tina@example.test' } })
    fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'Password@123' } })
    fireEvent.change(screen.getByLabelText('Role'), { target: { value: ROLES.IT } })
    fireEvent.click(screen.getByRole('button', { name: 'Create account' }))
    expect(await screen.findByText(/IT Tina IT created\./)).toBeInTheDocument()
    await waitFor(() => expect(state.refreshUsers).toHaveBeenCalled())
  })
  it('shows a retry action when the team list fails', () => {
    state.userError = new Error('offline')
    render(<TeamPage/>);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(state.refreshUsers).toHaveBeenCalled()
  })
  it('shows onboarding progress with a read-only checklist', () => {
    state.role = ROLES.EMPLOYEE
    render(<MyOnboardingPage/>);
    expect(screen.getByRole('heading', { name: /Good morning|Good afternoon|Good evening/ })).toBeInTheDocument()
    expect(screen.getByText('Collect documents')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Start' })).not.toBeInTheDocument()
    expect(screen.getByText('No equipment issued yet')).toBeInTheDocument()
  })
  it('shows the requested empty state when no employee record exists', () => {
    state.employee = null
    state.employeeError = { response: { status: 404 } }
    render(<MyOnboardingPage/>);
    expect(screen.getByText('HR has not created your onboarding record yet')).toBeInTheDocument()
  })
  it('shows retry when the onboarding request fails for another reason', () => {
    state.employee = null
    state.employeeError = { response: { status: 500, data: { message: 'Temporary failure' } } }
    render(<MyOnboardingPage/>);
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    expect(state.refreshEmployee).toHaveBeenCalled()
  })
})
