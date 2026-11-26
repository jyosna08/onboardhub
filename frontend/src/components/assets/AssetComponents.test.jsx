import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AssetFilters from './AssetFilters.jsx'
import AssetTable from './AssetTable.jsx'
import AddAssetModal from './AddAssetModal.jsx'
import { createAsset } from '@/api/assetApi.js'
import { ASSET_TYPE, ASSET_STATUS } from '@/utils/constants.js'

vi.mock('@/api/assetApi.js', () => ({ createAsset: vi.fn() }))

function fillAssetForm() {
  fireEvent.change(screen.getByLabelText('Make'), { target: { value: 'Dell' } })
  fireEvent.change(screen.getByLabelText('Model'), { target: { value: 'Latitude' } })
  fireEvent.change(screen.getByLabelText('Serial number'), { target: { value: 'SN-7' } })
}

describe('asset components', () => {
  beforeEach(() => { vi.clearAllMocks(); vi.useRealTimers() })
  it('debounces asset search by 400ms', async () => {
    vi.useFakeTimers()
    const onChange = vi.fn()
    render(<AssetFilters filters={{ type: '', status: '', search: '' }} onChange={onChange}/>);
    fireEvent.change(screen.getByRole('searchbox', { name: 'Search assets' }), { target: { value: 'dell' } })
    await act(async () => { vi.advanceTimersByTime(399) })
    expect(onChange).not.toHaveBeenCalledWith(expect.objectContaining({ search: 'dell' }))
    await act(async () => { vi.advanceTimersByTime(1) })
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ search: 'dell' }))
  })
  it('renders asset details in its table and mobile cards', () => {
    const onSelect = vi.fn()
    render(<AssetTable assets={[{ id: 7, assetTag: 'LAP-0007', type: ASSET_TYPE.LAPTOP, make: 'Dell', model: 'Latitude', serialNumber: 'SN-7', status: ASSET_STATUS.AVAILABLE }]} onSelect={onSelect}/>);
    expect(screen.getAllByText('LAP-0007')).toHaveLength(2)
    expect(screen.getAllByText(/Dell Latitude/)).toHaveLength(2)
    expect(screen.getAllByText('Available')).toHaveLength(2)
    fireEvent.click(screen.getByRole('button', { name: 'View LAP-0007' }))
    expect(onSelect).toHaveBeenCalledWith(expect.objectContaining({ id: 7 }))
  })
  it('validates missing fields and rejects future purchase dates', () => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-01-01T12:00:00'))
    render(<AddAssetModal isOpen onClose={vi.fn()}/>);
    fillAssetForm()
    fireEvent.change(screen.getByLabelText('Purchase date (optional)'), { target: { value: '2026-01-02' } })
    fireEvent.click(screen.getByRole('button', { name: 'Create asset' }))
    expect(screen.getByText('Purchase date cannot be in the future.')).toBeInTheDocument()
    expect(createAsset).not.toHaveBeenCalled()
    vi.useRealTimers()
  })
  it('shows duplicate serial errors and guards duplicate submits', async () => {
    createAsset.mockRejectedValue({ response: { status: 409 } })
    render(<AddAssetModal isOpen onClose={vi.fn()}/>);
    fillAssetForm()
    const form = screen.getByRole('button', { name: 'Create asset' }).closest('form')
    fireEvent.submit(form); fireEvent.submit(form)
    expect(await screen.findByText('An asset with this serial number already exists.')).toBeInTheDocument()
    expect(createAsset).toHaveBeenCalledTimes(1)
  })
})
