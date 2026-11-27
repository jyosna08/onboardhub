import { formatDate } from '@/utils/dateTime.js'
import { ROLE_LABELS } from '@/utils/display.js'

export function getGreeting(hour) {
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}

export default function WelcomeBanner({ user, now = new Date() }) {
  const firstName = user?.fullName?.trim().split(/\s+/)[0] || 'there'
  return <section className="rounded-2xl bg-gradient-to-r from-brand-600 via-brand-500 to-violet-500 p-6 text-white shadow-sm sm:p-8">
    <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"><div><p className="text-sm font-medium text-indigo-100">{ROLE_LABELS[user?.role] || ''}</p><h1 className="mt-1 text-2xl font-bold tracking-tight sm:text-3xl">{getGreeting(now.getHours())}, {firstName}</h1><p className="mt-2 max-w-xl text-sm text-indigo-100">Here’s what’s happening across your onboarding workspace today.</p></div><p className="text-sm font-medium text-indigo-100">{formatDate(now)}</p></div>
  </section>
}
