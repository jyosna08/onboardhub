import { useState } from 'react'
import { X } from 'lucide-react'
import Button from '@/components/common/Button.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import Skeleton from '@/components/common/Skeleton.jsx'
import SlideOver from '@/components/common/SlideOver.jsx'
import AssetStatusChip from './AssetStatusChip.jsx'
import AssetHistory from './AssetHistory.jsx'
import { updateAssetStatus, returnAsset } from '@/api/assetApi.js'
import { useAsset } from '@/hooks/useAsset.js'
import { useAuth } from '@/hooks/useAuth.js'
import { ASSET_STATUS, ROLES } from '@/utils/constants.js'
import { ASSET_TYPE_LABELS, ROLE_LABELS } from '@/utils/display.js'
import { formatDate, parseApiDateTime } from '@/utils/dateTime.js'
import { getErrorMessage } from '@/utils/errors.js'
import { useSubmitGuard } from '@/hooks/useSubmitGuard.js'

function formatTimestamp(value) {
  const date = parseApiDateTime(value)
  return date ? new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(date) : ''
}

export default function AssetDrawer({ asset: selectedAsset, isOpen, onClose, onChanged }) {
  const { asset: detail, isLoading, error: loadError, refresh } = useAsset(selectedAsset?.id, isOpen)
  const { user } = useAuth()
  const [actionError, setActionError] = useState(null)
  const [historyKey, setHistoryKey] = useState(0)
  const { isSubmitting, runOnce } = useSubmitGuard()
  const asset = detail || selectedAsset
  const canEdit = [ROLES.IT, ROLES.ADMIN].includes(user?.role)
  async function performAction(action, status) {
    setActionError(null)
    await runOnce(async () => {
      try { if (action === 'return') await returnAsset(asset.id); else await updateAssetStatus(asset.id, status); await refresh(); setHistoryKey((key) => key + 1); onChanged?.() }
      catch (requestError) { setActionError(requestError); await refresh(); setHistoryKey((key) => key + 1); onChanged?.() }
    })
  }
  const availableActions = asset?.status !== ASSET_STATUS.ASSIGNED
    ? [{ label: 'Mark in repair', status: ASSET_STATUS.IN_REPAIR, show: asset?.status === ASSET_STATUS.AVAILABLE }, { label: 'Retire', status: ASSET_STATUS.RETIRED, show: asset?.status !== ASSET_STATUS.RETIRED }, { label: 'Make available', status: ASSET_STATUS.AVAILABLE, show: asset?.status !== ASSET_STATUS.AVAILABLE }].filter((item) => item.show)
    : []
  return <SlideOver isOpen={isOpen} onClose={onClose} title="Asset details" className="p-5 sm:p-6">
    <div className="flex items-center justify-between border-b border-gray-100 pb-4"><h2 className="text-lg font-semibold text-gray-900">Asset details</h2><Button variant="ghost" size="sm" aria-label="Close asset details" onClick={onClose}><X size={18}/></Button></div>
    {loadError && <div className="mt-4"><ErrorBanner message={getErrorMessage(loadError)} onRetry={refresh}/></div>}{actionError && <div className="mt-4"><ErrorBanner message={getErrorMessage(actionError)} onDismiss={() => setActionError(null)}/></div>}
    {isLoading && !detail ? <div role="status" className="mt-5 space-y-3"><Skeleton className="h-24"/><Skeleton className="h-32"/></div> : asset && <div className="mt-5 space-y-6"><section className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm"><div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xl font-bold text-gray-900">{asset.assetTag}</p><p className="mt-1 text-sm text-gray-600">{ASSET_TYPE_LABELS[asset.type] || 'Asset'} · {asset.make} {asset.model}</p></div><AssetStatusChip status={asset.status}/></div><dl className="mt-4 space-y-3 text-sm"><div><dt className="text-xs text-gray-500">Serial number</dt><dd className="text-gray-800">{asset.serialNumber}</dd></div><div><dt className="text-xs text-gray-500">Purchase date</dt><dd className="text-gray-800">{formatDate(asset.purchaseDate) || '—'}</dd></div>{asset.status === ASSET_STATUS.ASSIGNED && <><div><dt className="text-xs text-gray-500">Assigned to</dt><dd className="text-gray-800">{asset.assignedEmployeeName || '—'}</dd></div><div><dt className="text-xs text-gray-500">Assigned date</dt><dd className="text-gray-800">{formatTimestamp(asset.assignedAt) || '—'}</dd></div></>}</dl></section>
      {canEdit && <section aria-label="Asset actions" className="flex flex-wrap gap-2">{asset.status === ASSET_STATUS.ASSIGNED ? <Button variant="secondary" disabled={isSubmitting} onClick={() => performAction('return')}>Return asset</Button> : availableActions.map((item) => <Button key={item.status} variant="secondary" disabled={isSubmitting} onClick={() => performAction('status', item.status)}>{item.label}</Button>)}</section>}
      <AssetHistory key={asset.id} assetId={asset.id} refreshKey={historyKey}/></div>}
  </SlideOver>
}
