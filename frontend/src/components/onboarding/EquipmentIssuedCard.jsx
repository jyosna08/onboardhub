import { Headphones, Keyboard, Laptop, Monitor, Mouse, Smartphone } from 'lucide-react'
import EmptyState from '@/components/common/EmptyState.jsx'
import { ASSET_TYPE, TASK_CATEGORY, TASK_STATUS } from '@/utils/constants.js'
import { ASSET_TYPE_LABELS } from '@/utils/display.js'

const icons = { [ASSET_TYPE.LAPTOP]: Laptop, [ASSET_TYPE.MONITOR]: Monitor, [ASSET_TYPE.KEYBOARD]: Keyboard, [ASSET_TYPE.MOUSE]: Mouse, [ASSET_TYPE.HEADSET]: Headphones, [ASSET_TYPE.PHONE]: Smartphone }

export default function EquipmentIssuedCard({ tasks = [] }) {
  const issued = tasks.filter((task) => task.category === TASK_CATEGORY.EQUIPMENT && task.status === TASK_STATUS.DONE && task.assetTag)
  return <section aria-labelledby="equipment-issued-heading" className="space-y-3"><h2 id="equipment-issued-heading" className="text-lg font-semibold text-gray-900">Equipment issued</h2>{issued.length ? <div className="grid gap-3 sm:grid-cols-2">{issued.map((task) => { const Icon = icons[task.assetType]; return <article key={task.id} className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md motion-reduce:transition-none motion-reduce:transform-none"><div className="flex items-center gap-3"><span className="rounded-lg bg-brand-50 p-2.5 text-brand-600">{Icon && <Icon aria-hidden="true" size={20}/>}</span><div><p className="text-sm font-semibold text-gray-900">{task.assetTag}</p><p className="text-sm text-gray-600">{task.assetLabel || ASSET_TYPE_LABELS[task.assetType] || 'Equipment'}</p></div></div></article>})}</div> : <EmptyState title="No equipment issued yet" message="Assigned equipment will appear here."/>}</section>
}
