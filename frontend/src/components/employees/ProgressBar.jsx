import { useId } from 'react'
import { formatProgress } from '@/utils/formatters.js'

export default function ProgressBar({ done = 0, total = 0, className = '' }) {
  const gradientId = useId()
  const percentage = total > 0 ? Math.min(100, Math.max(0, done / total * 100)) : 0
  return <div className={`flex items-center gap-3 ${className}`}>
    <div role="progressbar" aria-label="Checklist progress" aria-valuemin="0" aria-valuemax={total} aria-valuenow={Math.min(done, total)} className="h-2 min-w-16 flex-1 overflow-hidden rounded-full bg-gray-100">
      <svg aria-hidden="true" viewBox="0 0 100 8" preserveAspectRatio="none" className="h-full w-full"><defs><linearGradient id={gradientId} x1="0" x2="1"><stop offset="0%" stopColor="#6366f1"/><stop offset="100%" stopColor="#8b5cf6"/></linearGradient></defs><rect x="0" y="0" width={percentage} height="8" rx="4" fill={`url(#${gradientId})`} /></svg>
    </div>
    <span className="shrink-0 text-xs font-medium text-gray-600">{formatProgress(done, total)}</span>
  </div>
}
