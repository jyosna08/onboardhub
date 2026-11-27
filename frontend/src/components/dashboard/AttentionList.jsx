import { Link } from 'react-router-dom'
import Avatar from '@/components/common/Avatar.jsx'
import EmptyState from '@/components/common/EmptyState.jsx'
import { EMPLOYEE_STATUS } from '@/utils/constants.js'
import { DEPARTMENT_LABELS } from '@/utils/display.js'
import { formatDate } from '@/utils/dateTime.js'
import ProgressBar from '@/components/employees/ProgressBar.jsx'

export default function AttentionList({ employees = [] }) {
  const attention = employees.filter((employee) => employee.status !== EMPLOYEE_STATUS.ACTIVE)
    .sort((left, right) => String(left.joiningDate).localeCompare(String(right.joiningDate)))
  return <section className="space-y-3"><div><p className="text-xs font-semibold uppercase tracking-wide text-gray-500">Needs attention</p><h2 className="mt-1 text-lg font-semibold text-gray-900">Onboarding in progress</h2></div>
    {attention.length === 0 ? <EmptyState title="All caught up" message="There are no employees with open onboarding tasks."/> : <div className="overflow-hidden rounded-xl border border-gray-200 bg-white shadow-sm">{attention.map((employee) => <Link key={employee.id} to={`/employees/${employee.id}`} className="flex flex-col gap-3 border-b border-gray-100 p-4 transition last:border-b-0 hover:bg-gray-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-brand-500 sm:flex-row sm:items-center">
      <Avatar name={employee.fullName} size="md"/><div className="min-w-0 flex-1"><p className="truncate text-sm font-semibold text-gray-900">{employee.fullName}</p><p className="mt-0.5 text-xs text-gray-500">{DEPARTMENT_LABELS[employee.department] || employee.department} · Joining {formatDate(employee.joiningDate)}</p></div><div className="w-full sm:w-48"><ProgressBar done={employee.tasksDone} total={employee.tasksTotal}/></div>
    </Link>)}</div>}
  </section>
}
