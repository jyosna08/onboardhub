import { useNavigate } from 'react-router-dom'
import Badge from '@/components/common/Badge.jsx'
import Avatar from '@/components/common/Avatar.jsx'
import Button from '@/components/common/Button.jsx'
import { useAuth } from '@/hooks/useAuth.js'
import { ROLE_LABELS } from '@/utils/display.js'

export default function UserMenu() {
  const { user, logout } = useAuth(); const navigate = useNavigate()
  function handleLogout() { logout(); navigate('/login', { replace: true }) }
  return <div className="flex items-center gap-2 sm:gap-3"><Avatar name={user?.fullName} size="sm" /><div className="hidden min-w-0 sm:block"><p className="max-w-40 truncate text-sm font-medium text-gray-900">{user?.fullName}</p><Badge>{ROLE_LABELS[user?.role]}</Badge></div><Button variant="ghost" size="sm" onClick={handleLogout}>Log out</Button></div>
}
