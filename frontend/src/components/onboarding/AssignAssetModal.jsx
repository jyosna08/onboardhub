import { useEffect, useState } from 'react'
import Button from '@/components/common/Button.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import Modal from '@/components/common/Modal.jsx'
import { useAvailableAssets } from '@/hooks/useAvailableAssets.js'
import { useSubmitGuard } from '@/hooks/useSubmitGuard.js'
import { assignAsset } from '@/api/taskApi.js'
import { ASSET_TYPE_LABELS } from '@/utils/display.js'
import { getErrorMessage } from '@/utils/errors.js'

export default function AssignAssetModal({ isOpen, onClose, task, onAssigned, onConflict }) {
  const [selectedId, setSelectedId] = useState('')
  const [apiError, setApiError] = useState('')
  const { assets, isLoading, error, refresh } = useAvailableAssets(task?.assetType, isOpen)
  const { isSubmitting, runOnce } = useSubmitGuard()
  useEffect(() => { setSelectedId(''); setApiError('') }, [task?.id, isOpen])
  async function handleAssign() {
    if (!selectedId) return
    await runOnce(async () => {
      try { await assignAsset(task.id, selectedId); onClose?.(); onAssigned?.() }
      catch (errorResponse) { const status = errorResponse?.response?.status; setApiError(status === 503 ? 'Asset service is unavailable, please try again' : getErrorMessage(errorResponse)); if (status === 409) { void refresh(); onConflict?.() } }
    })
  }
  return <Modal isOpen={isOpen} onClose={onClose} title="Assign asset" closeDisabled={isSubmitting}>
    <div className="space-y-4"><p className="text-sm text-gray-600">Choose an available {ASSET_TYPE_LABELS[task?.assetType] || 'asset'} for this employee.</p>{apiError && <ErrorBanner message={apiError} onDismiss={() => setApiError('')}/ >}{error && <ErrorBanner message={getErrorMessage(error)} onRetry={refresh}/ >}{isLoading ? <p role="status" className="text-sm text-gray-500">Loading available assets…</p> : assets.length ? <fieldset className="max-h-72 space-y-2 overflow-y-auto"><legend className="sr-only">Available assets</legend>{assets.map((asset) => <label key={asset.id} className="flex cursor-pointer items-start gap-3 rounded-lg border border-gray-200 p-3 hover:border-brand-400"><input type="radio" name="asset" value={asset.id} checked={selectedId === asset.id} onChange={() => setSelectedId(asset.id)} className="mt-1 accent-brand-600"/><span className="text-sm"><strong className="text-gray-900">{asset.assetTag}</strong><span className="block text-gray-600">{asset.make} {asset.model} · {asset.serialNumber}</span></span></label>)}</fieldset> : <EmptyState title={`No available ${task?.assetType || ''} in stock`}/ >}<div className="flex justify-end gap-2 border-t border-gray-100 pt-4"><Button variant="secondary" onClick={onClose} disabled={isSubmitting}>Cancel</Button><Button onClick={handleAssign} loading={isSubmitting} disabled={!selectedId || isLoading}>Assign asset</Button></div></div>
  </Modal>
}
