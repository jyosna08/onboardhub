import { act, renderHook } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { useSubmitGuard } from './useSubmitGuard.js'

function deferred() {
  let resolve
  const promise = new Promise((done) => { resolve = done })
  return { promise, resolve }
}

describe('useSubmitGuard', () => {
  it('runs one submission for same-tick calls and clears the guard in finally', async () => {
    const { result } = renderHook(() => useSubmitGuard()); const pending = deferred(); const submit = vi.fn(() => pending.promise)
    let first; let second
    act(() => { first = result.current.runOnce(submit); second = result.current.runOnce(submit) })
    expect(submit).toHaveBeenCalledTimes(1)
    await expect(second).resolves.toBeUndefined()
    await act(async () => { pending.resolve('ok'); await first })
    expect(result.current.isSubmitting).toBe(false)
    await act(async () => { await result.current.runOnce(async () => 'again') })
    expect(result.current.isSubmitting).toBe(false)
  })

  it('does not update component state after unmount', async () => {
    const { result, unmount } = renderHook(() => useSubmitGuard()); const pending = deferred()
    let submission
    act(() => { submission = result.current.runOnce(() => pending.promise) })
    unmount()
    await act(async () => { pending.resolve(); await submission })
  })
})
