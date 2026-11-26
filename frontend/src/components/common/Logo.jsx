export default function Logo({ className = '' }) {
  return <span className={`inline-flex items-center gap-2.5 ${className}`}><svg viewBox="0 0 40 40" aria-hidden="true" className="h-9 w-9"><rect width="40" height="40" rx="11" fill="currentColor" className="text-brand-600"/><path d="M10 29 20 9l10 20h-6l-4-9-4 9z" fill="white"/><path d="M17 23h6" stroke="#e0e7ff" strokeWidth="2" /></svg><span className="text-lg font-bold tracking-tight text-gray-900">Onboard<span className="text-brand-600">Hub</span></span></span>
}
