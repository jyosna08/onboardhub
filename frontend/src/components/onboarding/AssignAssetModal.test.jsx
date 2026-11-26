import { beforeEach, describe, expect, it, vi } from 'vitest'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import AssignAssetModal from './AssignAssetModal.jsx'
import { assignAsset } from '@/api/taskApi.js'
import { ASSET_TYPE } from '@/utils/constants.js'

const refresh = vi.fn()
vi.mock('@/hooks/useAvailableAssets.js', () => ({ useAvailableAssets: () => ({ assets: [{ id: 7, type: 'LAPTOP', status: 'AVAILABLE', assetTag: 'LAP-0007', make: 'Dell', model: 'Latitude', serialNumber: 'S-7' }], isLoading: false, error: null, refresh }) }))
vi.mock('@/api/taskApi.js', () => ({ assignAsset: vi.fn() }))

const task = { id: 41, assetType: ASSET_TYPE.LAPTOP }

describe('AssignAssetModal', () => {
  beforeEach(() => { vi.clearAllMocks() })

  it('lists the matching asset and closes and refreshes after assigning', async () => {
    const onClose = vi.fn(); const onAssigned = vi.fn()
    assignAsset.mockResolvedValue({ id: 41 })
    render(<AssignAssetModal isOpen task={task} onClose={onClose} onAssigned={onAssigned}/>);
    expect(screen.getByText('LAP-0007')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText(/LAP-0007/))
    fireEvent.click(screen.getByRole('button', { name: 'Assign asset' }))
    await waitFor(() => expect(assignAsset).toHaveBeenCalledWith(41, 7))
    expect(onClose).toHaveBeenCalled()
    expect(onAssigned).toHaveBeenCalled()
  })

  it('shows conflict message and refreshes assets and employee', async () => {
    const onConflict = vi.fn()
    assignAsset.mockRejectedValue({ response: { status: 409, data: { message: 'Asset LAP-0007 is not available' } } })
    render(<AssignAssetModal isOpen task={task} onClose={vi.fn()} onConflict={onConflict}/>);
    fireEvent.click(screen.getByLabelText(/LAP-0007/))
    fireEvent.click(screen.getByRole('button', { name: 'Assign asset' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Asset LAP-0007 is not available')
    await waitFor(() => expect(refresh).toHaveBeenCalled())
    expect(onConflict).toHaveBeenCalled()
  })

  it('shows the service unavailable message', async () => {
    assignAsset.mockRejectedValue({ response: { status: 503 } })
    render(<AssignAssetModal isOpen task={task} onClose={vi.fn()}/>);
    fireEvent.click(screen.getByLabelText(/LAP-0007/))
    fireEvent.click(screen.getByRole('button', { name: 'Assign asset' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Asset service is unavailable, please try again')
  })
})
