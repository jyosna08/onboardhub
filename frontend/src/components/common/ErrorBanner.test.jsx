import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import ErrorBanner from './ErrorBanner.jsx'

describe('ErrorBanner', () => {
  it('announces errors and supports retry and dismiss actions', () => {
    const retry = vi.fn(); const dismiss = vi.fn()
    render(<ErrorBanner message="Try again" onRetry={retry} onDismiss={dismiss} />)
    expect(screen.getByRole('alert')).toHaveTextContent('Try again')
    fireEvent.click(screen.getByRole('button', { name: 'Retry' }))
    fireEvent.click(screen.getByRole('button', { name: 'Dismiss error' }))
    expect(retry).toHaveBeenCalledOnce()
    expect(dismiss).toHaveBeenCalledOnce()
  })
})
