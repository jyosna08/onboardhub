import { Menu } from 'lucide-react'
import Button from '@/components/common/Button.jsx'
import Logo from '@/components/common/Logo.jsx'
import UserMenu from './UserMenu.jsx'

export default function Navbar({ onMenuClick, navOpen = false }) {
  return <header className="sticky top-0 z-30 border-b border-gray-200 bg-white/95 backdrop-blur"><nav aria-label="Top navigation" className="flex h-16 items-center justify-between px-4 sm:px-6">
    <div className="flex items-center gap-3"><Button variant="ghost" size="sm" className="lg:hidden" aria-label={navOpen ? 'Close navigation menu' : 'Open navigation menu'} aria-expanded={navOpen} onClick={onMenuClick}><Menu size={20} /></Button><Logo className="lg:hidden" /></div>
    <UserMenu />
  </nav></header>
}
