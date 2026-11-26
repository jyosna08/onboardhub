import { ClipboardList, Laptop, Users } from 'lucide-react'
import { OWNER_ROLE, TASK_CATEGORY } from '@/utils/constants.js'
import TaskRow from './TaskRow.jsx'

export default function TaskList({ tasks = [], onAction, readOnly = false }) {
  const sections = [
    { title: 'HR tasks', icon: Users, rows: tasks.filter((task) => task.ownerRole === OWNER_ROLE.HR && task.category !== TASK_CATEGORY.EQUIPMENT) },
    { title: 'IT tasks', icon: ClipboardList, rows: tasks.filter((task) => task.ownerRole === OWNER_ROLE.IT && task.category !== TASK_CATEGORY.EQUIPMENT) },
    { title: 'Equipment', icon: Laptop, rows: tasks.filter((task) => task.category === TASK_CATEGORY.EQUIPMENT) },
  ]
  return <div className="space-y-6">{sections.map(({ title, icon: Icon, rows }) => <section key={title} aria-label={title}><h2 className="mb-3 flex items-center gap-2 text-lg font-semibold text-gray-900"><Icon size={19} className="text-brand-600"/>{title}<span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs text-gray-600">{rows.length}</span></h2>{rows.length ? <ol className="relative space-y-0 before:absolute before:bottom-5 before:left-[13px] before:top-5 before:w-px before:bg-gray-200">{rows.map((task) => <TaskRow key={task.id} task={task} onAction={onAction} readOnly={readOnly}/>)}</ol> : <p className="rounded-xl border border-dashed border-gray-200 bg-white p-4 text-sm text-gray-500">No tasks in this section.</p>}</section>)}</div>
}
