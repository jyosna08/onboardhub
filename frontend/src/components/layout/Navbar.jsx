import { Menu } from 'lucide-react'
import Button from '@/components/common/Button.jsx'
import Logo from '@/components/common/Logo.jsx'
import UserMenu from './UserMenu.jsx'

export default function Navbar({ onMenuClick }) {
  return <header className="sticky top-0 z-30 border-b border-gray-200 bg-white"><nav aria-label="Top navigation" className="flex h-16 items-center justify-between px-4 sm:px-6">
    <div className="flex items-center gap-3"><Button variant="ghost" size="sm" className="lg:hidden" aria-label="Open navigation menu" onClick={onMenuClick}><Menu size={20} /></Button><Logo /></div>
    <UserMenu />
  </nav></header>
}
