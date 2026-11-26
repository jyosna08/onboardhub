import { getStatusDisplay } from '@/utils/display.js'

export default function AssetStatusChip({ status }) {
  const display = getStatusDisplay(status)
  return <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${display.className}`}><span aria-hidden="true" className={`h-1.5 w-1.5 rounded-full ${display.dotClassName || 'bg-gray-400'}`} />{display.label}</span>
}
