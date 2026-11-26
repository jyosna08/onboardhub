import { act, renderHook, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { listAssets } from '@/api/assetApi.js'
import { useAssets } from './useAssets.js'

vi.mock('@/api/assetApi.js', () => ({ listAssets: vi.fn() }))

describe('useAssets', () => {
  beforeEach(() => vi.clearAllMocks())
  it('aborts superseded requests and uses the newest response', async () => {
    let resolveFirst
    listAssets.mockImplementationOnce(({ signal }) => { expect(signal).toBeInstanceOf(AbortSignal); return new Promise((resolve) => { resolveFirst = resolve }) }).mockResolvedValueOnce([{ id: 2 }])
    const { result, rerender } = renderHook(({ search }) => useAssets({ search }), { initialProps: { search: 'first' } })
    await waitFor(() => expect(listAssets).toHaveBeenCalledTimes(1))
    const firstSignal = listAssets.mock.calls[0][0].signal
    rerender({ search: 'second' })
    await waitFor(() => expect(result.current.assets).toEqual([{ id: 2 }]))
    expect(firstSignal.aborted).toBe(true)
    await act(async () => resolveFirst([{ id: 1 }]))
    expect(result.current.assets).toEqual([{ id: 2 }])
  })
  it('keeps data when a background refresh fails', async () => {
    listAssets.mockResolvedValueOnce([{ id: 1 }]).mockRejectedValueOnce(new Error('offline'))
    const { result } = renderHook(() => useAssets())
    await waitFor(() => expect(result.current.assets).toEqual([{ id: 1 }]))
    await act(async () => result.current.refresh())
    expect(result.current.assets).toEqual([{ id: 1 }])
    expect(result.current.error).toBeTruthy()
  })
})
