import { useEffect, useState } from 'react'
import { Search } from 'lucide-react'
import { DEPARTMENTS, EMPLOYEE_STATUS, SEARCH_DEBOUNCE_MS } from '@/utils/constants.js'
import { DEPARTMENT_LABELS, STATUS_DISPLAY } from '@/utils/display.js'
import { useDebounce } from '@/hooks/useDebounce.js'

export default function EmployeeFilters({ filters, onChange }) {
  const [search, setSearch] = useState(filters.search || '')
  const debouncedSearch = useDebounce(search, SEARCH_DEBOUNCE_MS)
  useEffect(() => { onChange({ ...filters, search: debouncedSearch }) }, [debouncedSearch])
  useEffect(() => { if (filters.search !== search) setSearch(filters.search || '') }, [filters.search])

  return <section aria-label="Filter employees" className="grid gap-3 rounded-xl border border-gray-200 bg-white p-4 shadow-sm md:grid-cols-[minmax(14rem,1fr)_12rem_12rem]">
    <label className="relative block"><span className="sr-only">Search employees</span><Search aria-hidden="true" size={17} className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"/><input type="search" value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Search name, email or code" className="block w-full rounded-lg border border-gray-300 bg-white py-2.5 pl-9 pr-3 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100"/></label>
    <label className="block"><span className="sr-only">Filter by employee status</span><select aria-label="Filter by employee status" value={filters.status || ''} onChange={(event) => onChange({ ...filters, status: event.target.value })} className="block w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100"><option value="">All statuses</option>{Object.values(EMPLOYEE_STATUS).map((status) => <option key={status} value={status}>{STATUS_DISPLAY[status].label}</option>)}</select></label>
    <label className="block"><span className="sr-only">Filter by department</span><select aria-label="Filter by department" value={filters.department || ''} onChange={(event) => onChange({ ...filters, department: event.target.value })} className="block w-full rounded-lg border border-gray-300 bg-white px-3 py-2.5 text-sm focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-100"><option value="">All departments</option>{Object.values(DEPARTMENTS).map((department) => <option key={department} value={department}>{DEPARTMENT_LABELS[department]}</option>)}</select></label>
  </section>
}
