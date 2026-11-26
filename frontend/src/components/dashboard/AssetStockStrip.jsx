import { Headphones, Keyboard, Laptop, Monitor, Mouse, Smartphone } from 'lucide-react'
import { ASSET_TYPE } from '@/utils/constants.js'
import { ASSET_TYPE_LABELS } from '@/utils/display.js'
import Skeleton from '@/components/common/Skeleton.jsx'

const icons = {
  [ASSET_TYPE.LAPTOP]: Laptop,
  [ASSET_TYPE.MONITOR]: Monitor,
  [ASSET_TYPE.KEYBOARD]: Keyboard,
  [ASSET_TYPE.MOUSE]: Mouse,
  [ASSET_TYPE.HEADSET]: Headphones,
  [ASSET_TYPE.PHONE]: Smartphone,
}

export default function AssetStockStrip({ summary, isLoading = false }) {
  const breakdown = new Map((summary?.byType || []).map((item) => [item.type, item]))
  return <section aria-labelledby="asset-stock-heading" className="space-y-3"><div className="flex items-end justify-between"><div><p className="text-xs font-semibold uppercase tracking-wide text-gray-500">Inventory</p><h2 id="asset-stock-heading" className="mt-1 text-lg font-semibold text-gray-900">Asset stock</h2></div><p className="text-sm text-gray-500">{summary?.total ?? 0} assets</p></div>
    <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">{Object.values(ASSET_TYPE).map((type) => {
      const record = breakdown.get(type) || { available: 0, assigned: 0 }
      const Icon = icons[type]
      const total = record.available + record.assigned
      const availableWidth = total ? record.available / total * 100 : 0
      return <article key={type} className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md motion-reduce:transition-none motion-reduce:transform-none">
        {isLoading ? <><Skeleton className="h-4 w-24"/><Skeleton className="mt-4 h-3 w-full"/></> : <>
          <div className="flex items-center gap-2 text-sm font-semibold text-gray-800"><span className="rounded-lg bg-brand-50 p-2 text-brand-600"><Icon aria-hidden="true" size={17}/></span>{ASSET_TYPE_LABELS[type]}</div>
          <svg role="img" aria-label={`${record.available} available, ${record.assigned} assigned`} viewBox="0 0 100 6" preserveAspectRatio="none" className="mt-4 h-1.5 w-full overflow-hidden rounded-full bg-gray-100"><rect x="0" y="0" width={availableWidth} height="6" fill="#10b981"/><rect x={availableWidth} y="0" width={100 - availableWidth} height="6" fill="#6366f1"/></svg>
          <div className="mt-3 flex justify-between text-xs text-gray-500"><span><i aria-hidden="true" className="mr-1 inline-block h-2 w-2 rounded-full bg-emerald-500"/>Available <strong className="text-gray-800">{record.available}</strong></span><span><i aria-hidden="true" className="mr-1 inline-block h-2 w-2 rounded-full bg-indigo-500"/>Assigned <strong className="text-gray-800">{record.assigned}</strong></span></div>
        </>}
      </article>
    })}</div>
  </section>
}
