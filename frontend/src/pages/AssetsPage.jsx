import { useState } from 'react'
import Button from '@/components/common/Button.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import Skeleton from '@/components/common/Skeleton.jsx'
import AddAssetModal from '@/components/assets/AddAssetModal.jsx'
import AssetDrawer from '@/components/assets/AssetDrawer.jsx'
import AssetFilters from '@/components/assets/AssetFilters.jsx'
import AssetTable from '@/components/assets/AssetTable.jsx'
import AssetStockStrip from '@/components/dashboard/AssetStockStrip.jsx'
import { useAssets } from '@/hooks/useAssets.js'
import { useAssetSummary } from '@/hooks/useAssetSummary.js'
import { usePolling } from '@/hooks/usePolling.js'
import { useAuth } from '@/hooks/useAuth.js'
import { REFRESH_INTERVAL_MS, ROLES } from '@/utils/constants.js'
import { getErrorMessage } from '@/utils/errors.js'

export default function AssetsPage() {
  const [filters, setFilters] = useState({ type: '', status: '', search: '' })
  const [addOpen, setAddOpen] = useState(false)
  const [selectedAsset, setSelectedAsset] = useState(null)
  const { assets, isLoading, error, refresh } = useAssets(filters)
  const { summary, isLoading: summaryLoading, error: summaryError, refresh: refreshSummary } = useAssetSummary()
  const { user } = useAuth()
  const canAdd = [ROLES.IT, ROLES.ADMIN].includes(user?.role)
  const reload = () => { void refresh(); void refreshSummary() }
  usePolling(reload, REFRESH_INTERVAL_MS)
  return <div className="space-y-6"><header className="flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-2xl font-bold text-gray-900">Assets</h1><p className="mt-1 text-sm text-gray-500">Track inventory and assignments.</p></div>{canAdd && <Button onClick={() => setAddOpen(true)}>Add asset</Button>}</header>{summaryError && <ErrorBanner message={getErrorMessage(summaryError)} onRetry={refreshSummary}/ >}<AssetStockStrip summary={summary} isLoading={summaryLoading}/><AssetFilters filters={filters} onChange={setFilters}/>{error && <ErrorBanner message={getErrorMessage(error)} onRetry={refresh}/ >}{isLoading ? <div className="space-y-3" role="status" aria-label="Loading assets">{Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-20"/>)}</div> : assets.length ? <AssetTable assets={assets} onSelect={setSelectedAsset}/> : <EmptyState title="No assets found" message="Try adjusting your filters or add an asset to the inventory."/>}<AddAssetModal isOpen={addOpen} onClose={() => setAddOpen(false)} onCreated={() => { setAddOpen(false); reload() }}/><AssetDrawer asset={selectedAsset} isOpen={Boolean(selectedAsset)} onClose={() => setSelectedAsset(null)} onChanged={reload}/></div>
}
