import EmptyState from '@/components/common/EmptyState.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import Skeleton from '@/components/common/Skeleton.jsx'
import WelcomeBanner from '@/components/dashboard/WelcomeBanner.jsx'
import ProgressBar from '@/components/employees/ProgressBar.jsx'
import EquipmentIssuedCard from '@/components/onboarding/EquipmentIssuedCard.jsx'
import TaskList from '@/components/onboarding/TaskList.jsx'
import { useMyEmployee } from '@/hooks/useMyEmployee.js'
import { usePolling } from '@/hooks/usePolling.js'
import { useAuth } from '@/hooks/useAuth.js'
import { REFRESH_INTERVAL_MS, TASK_STATUS } from '@/utils/constants.js'
import { getErrorMessage } from '@/utils/errors.js'

export default function MyOnboardingPage() {
  const { employee, isLoading, error, refresh } = useMyEmployee()
  const { user } = useAuth()
  usePolling(refresh, REFRESH_INTERVAL_MS, Boolean(employee))
  if (isLoading && !employee) return <div className="space-y-4" role="status" aria-label="Loading onboarding"><Skeleton className="h-40"/><Skeleton className="h-24"/><Skeleton className="h-80"/></div>
  if (error?.response?.status === 404) return <EmptyState title="HR has not created your onboarding record yet" message="Your checklist will appear here once HR adds your employee record."/>
  if (!employee && error) return <ErrorBanner message={getErrorMessage(error)} onRetry={refresh}/>
  if (!employee) return null
  const tasks = employee.tasks || []
  const tasksDone = tasks.filter((task) => task.status === TASK_STATUS.DONE).length
  return <div className="space-y-7">{error && <ErrorBanner message={getErrorMessage(error)} onRetry={refresh}/>}<WelcomeBanner user={{ ...user, fullName: employee.fullName }}/><section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm"><div className="flex items-center justify-between gap-3"><h2 className="text-lg font-semibold text-gray-900">Onboarding progress</h2><span className="text-sm text-gray-500">{tasksDone} of {tasks.length} tasks complete</span></div><ProgressBar done={tasksDone} total={tasks.length} className="mt-4"/></section><TaskList tasks={tasks} readOnly/><EquipmentIssuedCard tasks={tasks}/></div>
}
