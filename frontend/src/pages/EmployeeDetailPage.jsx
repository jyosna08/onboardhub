import { useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import Avatar from '@/components/common/Avatar.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import Skeleton from '@/components/common/Skeleton.jsx'
import Button from '@/components/common/Button.jsx'
import ProgressBar from '@/components/employees/ProgressBar.jsx'
import StatusChip from '@/components/employees/StatusChip.jsx'
import TaskList from '@/components/onboarding/TaskList.jsx'
import AssignAssetModal from '@/components/onboarding/AssignAssetModal.jsx'
import { updateTaskStatus } from '@/api/taskApi.js'
import { useEmployee } from '@/hooks/useEmployee.js'
import { usePolling } from '@/hooks/usePolling.js'
import { REFRESH_INTERVAL_MS } from '@/utils/constants.js'
import { DEPARTMENT_LABELS } from '@/utils/display.js'
import { formatDate } from '@/utils/dateTime.js'
import { getErrorMessage } from '@/utils/errors.js'

export default function EmployeeDetailPage() {
  const { id } = useParams()
  const { employee, isLoading, error, refresh } = useEmployee(id)
  const location = useLocation()
  const [notice, setNotice] = useState(location.state?.notice || '')
  const [actionError, setActionError] = useState(null)
  const [assignTask, setAssignTask] = useState(null)
  usePolling(refresh, REFRESH_INTERVAL_MS, Boolean(employee))
  async function handleAction(task, action) {
    if (action === 'assign') { setAssignTask(task); return }
    setActionError(null)
    try { await updateTaskStatus(task.id, action); await refresh() }
    catch (requestError) { setActionError(requestError); await refresh() }
  }
  if (isLoading && !employee) return <div className="space-y-4" role="status" aria-label="Loading employee"><Skeleton className="h-44"/><Skeleton className="h-96"/></div>
  if (error?.response?.status === 404) return <EmptyState title="Employee not found" message="This employee could not be found." action={<Link to="/employees" className="text-brand-700 underline">Back to employees</Link>}/>
  if (!employee && error) return <ErrorBanner message={getErrorMessage(error)} onRetry={refresh}/>
  if (!employee) return null
  const tasks = employee.tasks || employee.onboardingTasks || []
  return <div className="space-y-6"><Link to="/employees" className="inline-flex items-center gap-2 rounded text-sm font-medium text-gray-600 hover:text-brand-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 focus-visible:ring-offset-2"><ArrowLeft size={16}/>Employees</Link>{notice && <div role="status" className="flex justify-between rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">{notice}<button type="button" aria-label="Dismiss notice" className="rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-emerald-700 focus-visible:ring-offset-2" onClick={() => setNotice('')}>×</button></div>}{error && <ErrorBanner message={getErrorMessage(error)} onRetry={refresh}/ >}{actionError && <ErrorBanner message={getErrorMessage(actionError)} onDismiss={() => setActionError(null)}/>}<section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm sm:p-7"><div className="flex flex-wrap items-start gap-4"><Avatar name={employee.fullName} size="lg"/><div className="min-w-0 flex-1"><div className="flex flex-wrap items-center gap-3"><h1 className="text-2xl font-bold text-gray-900">{employee.fullName}</h1><StatusChip status={employee.status}/></div><p className="mt-1 text-sm text-gray-500">{employee.employeeCode} · {employee.email}</p><p className="mt-3 text-sm text-gray-700">{DEPARTMENT_LABELS[employee.department] || 'Department'} · {employee.jobTitle}</p><p className="mt-1 text-sm text-gray-500">Joining {formatDate(employee.joiningDate)}</p></div></div><div className="mt-6 max-w-xl"><ProgressBar done={employee.tasksDone} total={employee.tasksTotal}/></div></section><TaskList tasks={tasks} onAction={handleAction}/><AssignAssetModal isOpen={Boolean(assignTask)} task={assignTask} onClose={() => setAssignTask(null)} onAssigned={() => { setAssignTask(null); void refresh() }} onConflict={() => refresh()}/></div>
}
