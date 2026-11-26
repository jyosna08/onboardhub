import { ASSET_STATUS, ASSET_TYPE, DEPARTMENTS, EMPLOYEE_STATUS, OWNER_ROLE, ROLES, TASK_STATUS } from './constants.js'

export const STATUS_DISPLAY = Object.freeze({
  [EMPLOYEE_STATUS.PENDING]: { label: 'Pending', className: 'bg-gray-100 text-gray-700' },
  [EMPLOYEE_STATUS.ONBOARDING]: { label: 'Onboarding', className: 'bg-amber-100 text-amber-800' },
  [EMPLOYEE_STATUS.ACTIVE]: { label: 'Active', className: 'bg-emerald-100 text-emerald-800' },
  [TASK_STATUS.TODO]: { label: 'To do', className: 'bg-gray-100 text-gray-700' },
  [TASK_STATUS.IN_PROGRESS]: { label: 'In progress', className: 'bg-blue-100 text-blue-800' },
  [TASK_STATUS.DONE]: { label: 'Done', className: 'bg-emerald-100 text-emerald-800' },
  [ASSET_STATUS.AVAILABLE]: { label: 'Available', className: 'bg-emerald-100 text-emerald-800' },
  [ASSET_STATUS.ASSIGNED]: { label: 'Assigned', className: 'bg-indigo-100 text-indigo-800' },
  [ASSET_STATUS.IN_REPAIR]: { label: 'In repair', className: 'bg-amber-100 text-amber-800' },
  [ASSET_STATUS.RETIRED]: { label: 'Retired', className: 'bg-gray-100 text-gray-700' },
})

export const ROLE_LABELS = Object.freeze({ [ROLES.ADMIN]: 'Administrator', [ROLES.HR]: 'HR', [ROLES.IT]: 'IT', [ROLES.EMPLOYEE]: 'Employee' })
export const OWNER_BADGE_CLASSES = Object.freeze({ [OWNER_ROLE.HR]: 'bg-violet-100 text-violet-800', [OWNER_ROLE.IT]: 'bg-sky-100 text-sky-800' })
export const ASSET_TYPE_LABELS = Object.freeze({ [ASSET_TYPE.LAPTOP]: 'Laptop', [ASSET_TYPE.MONITOR]: 'Monitor', [ASSET_TYPE.KEYBOARD]: 'Keyboard', [ASSET_TYPE.MOUSE]: 'Mouse', [ASSET_TYPE.HEADSET]: 'Headset', [ASSET_TYPE.PHONE]: 'Phone' })
export const DEPARTMENT_LABELS = Object.freeze(Object.fromEntries(Object.values(DEPARTMENTS).map((department) => [department, department[0] + department.slice(1).toLowerCase()])))

export function getStatusDisplay(status) { return STATUS_DISPLAY[status] || { label: String(status || ''), className: 'bg-gray-100 text-gray-700' } }
