import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Button from '@/components/common/Button.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import Skeleton from '@/components/common/Skeleton.jsx'
import AddEmployeeModal from '@/components/employees/AddEmployeeModal.jsx'
import EmployeeFilters from '@/components/employees/EmployeeFilters.jsx'
import EmployeeTable from '@/components/employees/EmployeeTable.jsx'
import { useEmployees } from '@/hooks/useEmployees.js'
import { usePolling } from '@/hooks/usePolling.js'
import { useAuth } from '@/hooks/useAuth.js'
import { REFRESH_INTERVAL_MS, ROLES } from '@/utils/constants.js'
import { getErrorMessage } from '@/utils/errors.js'

export default function EmployeesPage() {
  const [filters, setFilters] = useState({ status: '', department: '', search: '' })
  const [modalOpen, setModalOpen] = useState(false)
  const { employees, isLoading, error, refresh } = useEmployees(filters)
  const { user } = useAuth()
  const navigate = useNavigate()
  usePolling(refresh, REFRESH_INTERVAL_MS)
  const canCreate = [ROLES.HR, ROLES.ADMIN].includes(user?.role)
  return <div className="space-y-5"><header className="flex flex-wrap items-center justify-between gap-3"><div><h1 className="text-2xl font-bold text-gray-900">Employees</h1><p className="mt-1 text-sm text-gray-500">Manage employee records and onboarding progress.</p></div>{canCreate && <Button onClick={() => setModalOpen(true)}>Add employee</Button>}</header><EmployeeFilters filters={filters} onChange={setFilters}/>{error && <ErrorBanner message={getErrorMessage(error)} onRetry={refresh}/ >}{isLoading ? <div className="space-y-3" role="status" aria-label="Loading employees">{Array.from({ length: 4 }, (_, i) => <Skeleton key={i} className="h-20"/>)}</div> : employees.length ? <EmployeeTable employees={employees}/> : <EmptyState title="No employees found" message="Try adjusting your filters or add an employee."/>}<AddEmployeeModal isOpen={modalOpen} onClose={() => setModalOpen(false)} onCreated={(employee) => { setModalOpen(false); navigate(`/employees/${employee.id}`, { state: { notice: 'Employee created successfully.' } }) }}/></div>
}
