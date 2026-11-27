import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import Navbar from './Navbar.jsx'
import SideNav from './SideNav.jsx'

export default function AppLayout() {
  const [navOpen, setNavOpen] = useState(false)
  return <div className="min-h-screen bg-slate-50"><Navbar navOpen={navOpen} onMenuClick={() => setNavOpen((open) => !open)} /><div className="relative flex"><SideNav open={navOpen} onNavigate={() => setNavOpen(false)} /><main className="min-h-[calc(100vh-4rem)] min-w-0 flex-1 px-4 py-6 sm:px-6 lg:px-8"><div className="mx-auto max-w-7xl"><Outlet /></div></main></div></div>
}
