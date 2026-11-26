import { useState } from 'react'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import Skeleton from '@/components/common/Skeleton.jsx'
import WelcomeBanner from '@/components/dashboard/WelcomeBanner.jsx'
import KpiGrid from '@/components/dashboard/KpiGrid.jsx'
import AssetStockStrip from '@/components/dashboard/AssetStockStrip.jsx'
import AttentionList from '@/components/dashboard/AttentionList.jsx'
import { useDashboardData } from '@/hooks/useDashboardData.js'
import { useAuth } from '@/hooks/useAuth.js'
import { getErrorMessage } from '@/utils/errors.js'

export default function DashboardPage() {
  const { user } = useAuth()
  const { dashboard, assetSummary, employees, isLoading, error, refresh } = useDashboardData()
  const [dismissed, setDismissed] = useState(false)
  return <div className="space-y-7">{error && !dismissed && <ErrorBanner message={getErrorMessage(error)} onRetry={refresh} onDismiss={() => setDismissed(true)}/>}<WelcomeBanner user={user}/>{isLoading ? <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">{Array.from({ length: 5 }, (_, i) => <Skeleton key={i} className="h-32"/>)}</div> : <KpiGrid dashboard={dashboard} isLoading={false}/>}<AssetStockStrip summary={assetSummary} isLoading={isLoading}/><AttentionList employees={employees}/></div>
}
