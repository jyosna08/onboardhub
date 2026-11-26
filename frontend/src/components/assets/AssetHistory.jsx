import { useEffect, useMemo } from 'react'
import { useAssetHistory } from '@/hooks/useAssetHistory.js'
import EmptyState from '@/components/common/EmptyState.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import Skeleton from '@/components/common/Skeleton.jsx'
import { getErrorMessage } from '@/utils/errors.js'
import { parseApiDateTime } from '@/utils/dateTime.js'

function formatTimestamp(value) {
  const date = parseApiDateTime(value)
  return date ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date) : ''
}

export default function AssetHistory({ assetId, refreshKey = 0 }) {
  const { history, isLoading, error, refresh } = useAssetHistory(assetId, Boolean(assetId))
  useEffect(() => { if (refreshKey > 0) void refresh() }, [refreshKey, refresh])
  const newestFirst = useMemo(() => [...history].sort((a, b) => String(b.assignedAt).localeCompare(String(a.assignedAt))), [history])
  return <section aria-labelledby="asset-history-heading" className="space-y-3"><h3 id="asset-history-heading" className="text-base font-semibold text-gray-900">Assignment history</h3>{error && <ErrorBanner message={getErrorMessage(error)} onRetry={refresh}/ >}{isLoading ? <div className="space-y-2" role="status" aria-label="Loading assignment history"><Skeleton className="h-16"/><Skeleton className="h-16"/></div> : newestFirst.length ? <ol className="space-y-3">{newestFirst.map((entry) => <li key={entry.id} className="rounded-lg border border-gray-200 bg-white p-3 text-sm"><p className="font-medium text-gray-900">{entry.employeeName}</p><p className="mt-1 text-xs text-gray-500">Assigned {formatTimestamp(entry.assignedAt)} by {entry.assignedByName}</p><p className="mt-1 text-xs text-gray-600">{entry.returnedAt ? `Returned ${formatTimestamp(entry.returnedAt)}${entry.returnedByName ? ` by ${entry.returnedByName}` : ''}` : 'In use'}</p></li>)}</ol> : <EmptyState title="No assignment history" message="This asset has not been assigned yet."/>}</section>
}
