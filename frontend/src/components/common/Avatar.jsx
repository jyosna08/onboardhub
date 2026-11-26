import { getInitials } from '@/utils/formatters.js'

const palette = [
  'bg-indigo-100 text-indigo-700',
  'bg-violet-100 text-violet-700',
  'bg-emerald-100 text-emerald-700',
  'bg-amber-100 text-amber-800',
  'bg-rose-100 text-rose-700',
  'bg-sky-100 text-sky-700',
]
const sizes = { sm: 'h-8 w-8 text-xs', md: 'h-10 w-10 text-sm', lg: 'h-16 w-16 text-xl' }

function colorIndex(name = '') {
  const normalized = String(name).trim().toLowerCase()
  return [...normalized].reduce((hash, character, index) => (hash + character.charCodeAt(0) * (index + 1)) % palette.length, 0)
}

export default function Avatar({ name = '', size = 'md', className = '' }) {
  return <span aria-label={`${name || 'Unknown user'} initials`} className={`inline-flex shrink-0 items-center justify-center rounded-full font-semibold ${palette[colorIndex(name)]} ${sizes[size] || sizes.md} ${className}`}>{getInitials(name)}</span>
}
