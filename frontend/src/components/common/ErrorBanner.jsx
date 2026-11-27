import { X } from 'lucide-react'
import Button from './Button.jsx'

export default function ErrorBanner({ message, onDismiss, onRetry }) {
  if (!message) return null
  return <div role="alert" className="flex flex-col gap-3 rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800 sm:flex-row sm:items-center sm:justify-between">
    <p>{message}</p><div className="flex gap-2">{onRetry && <Button variant="secondary" size="sm" onClick={onRetry}>Retry</Button>}{onDismiss && <Button variant="ghost" size="sm" onClick={onDismiss} aria-label="Dismiss error"><X size={16} />Dismiss</Button>}</div>
  </div>
}
