import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AssetDrawer from './AssetDrawer.jsx'
import { returnAsset, updateAssetStatus } from '@/api/assetApi.js'
import { ASSET_STATUS, ASSET_TYPE, ROLES } from '@/utils/constants.js'

const { mockRole } = vi.hoisted(() => ({ mockRole: { value: 'IT' } }))
const sample = { id: 7, assetTag: 'LAP-0007', type: ASSET_TYPE.LAPTOP, make: 'Dell', model: 'Latitude', serialNumber: 'SN-7', purchaseDate: '2026-01-02', status: ASSET_STATUS.AVAILABLE }
vi.mock('@/hooks/useAuth.js', () => ({ useAuth: () => ({ user: { role: mockRole.value } }) }))
vi.mock('@/hooks/useAsset.js', () => ({ useAsset: () => ({ asset: sample, isLoading: false, error: null, refresh: vi.fn() }) }))
vi.mock('@/hooks/useAssetHistory.js', () => ({ useAssetHistory: () => ({ history: [
  { id: 1, employeeName: 'Asha', assignedAt: '2026-01-01T09:00:00', assignedByName: 'Ravi', returnedAt: null },
  { id: 2, employeeName: 'Mira', assignedAt: '2026-02-01T09:00:00', assignedByName: 'Ravi', returnedAt: '2026-02-05T10:00:00', returnedByName: 'Asha' },
], isLoading: false, error: null, refresh: vi.fn() }) }))
vi.mock('@/api/assetApi.js', () => ({ returnAsset: vi.fn(), updateAssetStatus: vi.fn() }))

describe('AssetDrawer', () => {
  beforeEach(() => { vi.clearAllMocks(); mockRole.value = ROLES.IT; sample.status = ASSET_STATUS.AVAILABLE })
  it('offers non-assigned status actions to IT and refreshes after a change', async () => {
    const onChanged = vi.fn()
    render(<AssetDrawer asset={sample} isOpen onClose={vi.fn()} onChanged={onChanged}/>);
    fireEvent.click(screen.getByRole('button', { name: 'Mark in repair' }))
    await waitFor(() => expect(updateAssetStatus).toHaveBeenCalledWith(sample.id, ASSET_STATUS.IN_REPAIR))
    expect(onChanged).toHaveBeenCalled()
    expect(screen.getByText('Mira').compareDocumentPosition(screen.getByText('Asha')) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(screen.getByText('In use')).toBeInTheDocument()
  })
  it('shows only Return asset for assigned assets', () => {
    sample.status = ASSET_STATUS.ASSIGNED
    render(<AssetDrawer asset={sample} isOpen onClose={vi.fn()}/>);
    expect(screen.getByRole('button', { name: 'Return asset' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Retire' })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Mark in repair' })).not.toBeInTheDocument()
  })
  it('shows no action buttons for HR while keeping details and history', () => {
    mockRole.value = ROLES.HR
    render(<AssetDrawer asset={sample} isOpen onClose={vi.fn()}/>);
    expect(screen.getByText('LAP-0007')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Assignment history' })).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Asset actions' })).not.toBeInTheDocument()
  })
  it('allows ADMIN to return assigned assets', async () => {
    mockRole.value = ROLES.ADMIN; sample.status = ASSET_STATUS.ASSIGNED
    render(<AssetDrawer asset={sample} isOpen onClose={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button', { name: 'Return asset' }))
    await waitFor(() => expect(returnAsset).toHaveBeenCalledWith(sample.id))
  })
  it('allows ADMIN to change an available asset status', async () => {
    mockRole.value = ROLES.ADMIN
    render(<AssetDrawer asset={sample} isOpen onClose={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button', { name: 'Retire' }))
    await waitFor(() => expect(updateAssetStatus).toHaveBeenCalledWith(sample.id, ASSET_STATUS.RETIRED))
  })
  it('shows backend action errors in an alert', async () => {
    updateAssetStatus.mockRejectedValue({ response: { status: 403, data: { message: 'Not allowed' } } })
    render(<AssetDrawer asset={sample} isOpen onClose={vi.fn()}/>);
    fireEvent.click(screen.getByRole('button', { name: 'Retire' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Not allowed')
  })
})
