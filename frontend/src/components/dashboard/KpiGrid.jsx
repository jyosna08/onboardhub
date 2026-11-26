import { CircleAlert, ListTodo, UserRoundCheck, UserRoundPlus, UsersRound } from 'lucide-react'
import { EMPLOYEE_STATUS } from '@/utils/constants.js'
import { STATUS_DISPLAY } from '@/utils/display.js'
import KpiCard from './KpiCard.jsx'

export default function KpiGrid({ dashboard, isLoading = false }) {
  const cards = [
    { label: STATUS_DISPLAY[EMPLOYEE_STATUS.PENDING].label, value: dashboard?.pendingEmployees, icon: UserRoundPlus, tone: 'brand' },
    { label: STATUS_DISPLAY[EMPLOYEE_STATUS.ONBOARDING].label, value: dashboard?.onboardingEmployees, icon: UsersRound, tone: 'amber' },
    { label: `${STATUS_DISPLAY[EMPLOYEE_STATUS.ACTIVE].label} employees`, value: dashboard?.activeEmployees, icon: UserRoundCheck, tone: 'emerald' },
    { label: 'Open tasks', value: dashboard?.openTasks, icon: ListTodo, tone: 'blue' },
    { label: 'Overdue tasks', value: dashboard?.overdueTasks, icon: CircleAlert, tone: dashboard?.overdueTasks > 0 ? 'danger' : 'amber', highlighted: dashboard?.overdueTasks > 0 },
  ]
  return <section aria-label="Onboarding summary" className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-5">{cards.map((card) => <KpiCard key={card.label} {...card} isLoading={isLoading}/>)}</section>
}
