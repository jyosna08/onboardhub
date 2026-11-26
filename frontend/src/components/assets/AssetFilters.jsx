import { useEffect, useState } from 'react'
import { Search } from 'lucide-react'
import { ASSET_STATUS, ASSET_TYPE, SEARCH_DEBOUNCE_MS } from '@/utils/constants.js'
import { ASSET_TYPE_LABELS, STATUS_DISPLAY } from '@/utils/display.js'
import { useDebounce } from '@/hooks/useDebounce.js'

export default function AssetFilters({ filters, onChange }) {
  const [search, setSearch] = useState(filters.search || '')
  const debouncedSearch = useDebounce(search, SEARCH_DEBOUNCE_MS)
  useEffect(() => { onChange({ ...filters, search: debouncedSearch }) }, [debouncedSearch])
  useEffect(() => { if (filters.search !== search) setSearch(filters.search || '') }, [filters.search])
  return <section aria-label="Filter assets" className="grid gap-3 rounded-xl border border-gray-200 bg-white p-4 shadow-sm md:grid-cols-[minmax(14rem,1fr)_12rem_12rem]">
    <label className="relative block"><span className="sr-only">Search assets</span><Search aria-hidden="true" size={17} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/><input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search tag, make, model or serial" className="block w-full rounded-lg border border-gray-300 bg-white py-2.5 pl-9 pr-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100"/></label>
    <label><span className="sr-only">Filter by asset type</span><select aria-label="Filter by asset type" value={filters.type || ''} onChange={(event) => onChange({ ...filters, type: event.target.value })} className="block w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100"><option value="">All types</option>{Object.values(ASSET_TYPE).map((type) => <option key={type} value={type}>{ASSET_TYPE_LABELS[type]}</option>)}</select></label>
    <label><span className="sr-only">Filter by asset status</span><select aria-label="Filter by asset status" value={filters.status || ''} onChange={(event) => onChange({ ...filters, status: event.target.value })} className="block w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100"><option value="">All statuses</option>{Object.values(ASSET_STATUS).map((status) => <option key={status} value={status}>{STATUS_DISPLAY[status].label}</option>)}</select></label>
  </section>
}
