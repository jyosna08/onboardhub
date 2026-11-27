import { useAuth } from '@/hooks/useAuth.js'
import { OWNER_ROLE, ROLES, TASK_CATEGORY, TASK_STATUS } from '@/utils/constants.js'
import { OWNER_BADGE_CLASSES, ROLE_LABELS } from '@/utils/display.js'
import { formatDate } from '@/utils/dateTime.js'
import StatusChip from '@/components/employees/StatusChip.jsx'
import Button from '@/components/common/Button.jsx'

export default function TaskRow({ task, onAction, readOnly = false }) {
  const { user } = useAuth()
  const canAct = !readOnly && (user?.role === ROLES.ADMIN || (task.ownerRole === OWNER_ROLE.HR && user?.role === ROLES.HR) || (task.ownerRole === OWNER_ROLE.IT && user?.role === ROLES.IT))
  const equipment = task.category === TASK_CATEGORY.EQUIPMENT
  const done = task.status === TASK_STATUS.DONE
  return <li className="relative flex gap-4 pb-6 last:pb-0">
    <span aria-hidden="true" className={`z-10 mt-1 flex h-7 w-7 shrink-0 items-center justify-center rounded-full border-2 ${done ? 'border-emerald-500 bg-emerald-500 text-white' : task.status === TASK_STATUS.IN_PROGRESS ? 'border-blue-500 bg-blue-50' : 'border-gray-300 bg-white'}`}>{done ? '✓' : task.status === TASK_STATUS.IN_PROGRESS ? <span className="h-2 w-2 rounded-full bg-blue-500" /> : null}</span>
    <div className="min-w-0 flex-1 rounded-xl border border-gray-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md motion-reduce:transition-none motion-reduce:transform-none">
      <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-semibold text-gray-900">{task.title}</h3><div className="mt-2 flex flex-wrap items-center gap-2"><span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${OWNER_BADGE_CLASSES[task.ownerRole] || 'bg-gray-100 text-gray-700'}`}>{ROLE_LABELS[task.ownerRole] || 'Team'}</span><StatusChip status={task.status}/></div></div><span className={`text-sm ${task.overdue && !done ? 'font-semibold text-rose-600' : 'text-gray-500'}`}>Due {formatDate(task.dueDate)}</span></div>
      {done && equipment && task.assetTag && <p className="mt-3 text-sm text-gray-600">{task.assetTag}{task.assetLabel ? ` · ${task.assetLabel}` : ''}</p>}
      {canAct && !done && <div className="mt-4 flex flex-wrap gap-2">{equipment ? <Button size="sm" onClick={() => onAction?.(task, 'assign')}>Assign asset</Button> : <>{task.status === TASK_STATUS.TODO ? <Button size="sm" onClick={() => onAction?.(task, TASK_STATUS.IN_PROGRESS)}>Start</Button> : <Button size="sm" variant="secondary" onClick={() => onAction?.(task, TASK_STATUS.TODO)}>Back to To do</Button>}<Button size="sm" variant="secondary" onClick={() => onAction?.(task, TASK_STATUS.DONE)}>Mark done</Button></>}</div>}
    </div>
  </li>
}
