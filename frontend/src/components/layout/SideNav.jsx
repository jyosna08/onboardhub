import { NavLink } from 'react-router-dom'
import { useAuth } from '@/hooks/useAuth.js'
import { ROLES } from '@/utils/constants.js'
import Logo from '@/components/common/Logo.jsx'

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
  return <aside className={`${open ? 'block' : 'hidden'} absolute inset-x-0 top-16 z-20 border-b border-slate-700 bg-slate-900 p-3 lg:sticky lg:top-16 lg:block lg:min-h-[calc(100vh-4rem)] lg:w-60 lg:shrink-0 lg:border-b-0 lg:border-r lg:p-4`}>
    <div className="mb-7 hidden px-2 pt-2 lg:block"><Logo inverse /></div><nav aria-label="Primary navigation" className="space-y-1">{links.map((link) => <NavLink key={link.to} to={link.to} onClick={onNavigate} className={({ isActive }) => `relative block rounded-lg px-3 py-2.5 text-sm font-medium transition-colors before:absolute before:inset-y-2 before:left-0 before:w-1 before:rounded-r before:content-[''] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-400 ${isActive ? 'bg-slate-800 text-white before:bg-brand-400' : 'text-slate-300 hover:bg-slate-800 hover:text-white before:bg-transparent'}`}>{link.label}</NavLink>)}</nav>
  </aside>
}
