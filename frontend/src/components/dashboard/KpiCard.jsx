import Skeleton from '@/components/common/Skeleton.jsx'

const tones = {
  brand: 'bg-brand-50 text-brand-700',
  blue: 'bg-blue-50 text-blue-700',
  emerald: 'bg-emerald-50 text-emerald-700',
  amber: 'bg-amber-50 text-amber-700',
  danger: 'bg-rose-50 text-rose-700',
}

export default function KpiCard({ label, value, icon: Icon, tone = 'brand', highlighted = false, isLoading = false }) {
  if (isLoading) return <div className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm"><Skeleton className="h-4 w-28"/><Skeleton className="mt-5 h-8 w-16"/></div>
  return <article className={`rounded-xl border bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md motion-reduce:transition-none motion-reduce:transform-none ${highlighted ? 'border-rose-200 ring-1 ring-rose-100' : 'border-gray-200'}`}>
    <div className="flex items-start justify-between gap-3"><p className="text-sm font-medium text-gray-500">{label}</p>{Icon && <span className={`rounded-lg p-2 ${tones[tone] || tones.brand}`}><Icon aria-hidden="true" size={18}/></span>}</div><p className={`mt-4 text-3xl font-bold tracking-tight ${highlighted ? 'text-rose-700' : 'text-gray-900'}`}>{value ?? 0}</p>
  </article>
}
