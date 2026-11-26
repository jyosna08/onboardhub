import { NavLink } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth.js'
import { ROLES } from '@/utils/constants.js'

const managementLinks = [
  { to: '/dashboard', label: 'Dashboard' },
  { to: '/employees', label: 'Employees' },
  { to: '/assets', label: 'Assets' },
]

export default function SideNav({ open = false, onNavigate }) {
  const { user } = useAuth()
  const links = user?.role === ROLES.EMPLOYEE
    ? [{ to: '/my-onboarding', label: 'My onboarding' }]
    : [...managementLinks, ...(user?.role === ROLES.ADMIN ? [{ to: '/admin/team', label: 'Team' }] : [])]
  return <aside className={`${open ? 'block' : 'hidden'} absolute inset-x-0 top-16 z-20 border-b border-gray-200 bg-white p-3 lg:static lg:block lg:min-h-[calc(100vh-4rem)] lg:w-60 lg:shrink-0 lg:border-b-0 lg:border-r lg:p-4`}>
    <nav aria-label="Primary navigation" className="space-y-1">{links.map((link) => <NavLink key={link.to} to={link.to} onClick={onNavigate} className={({ isActive }) => `block rounded-lg px-3 py-2.5 text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 ${isActive ? 'bg-brand-50 text-brand-700' : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'}`}>{link.label}</NavLink>)}</nav>
  </aside>
}
