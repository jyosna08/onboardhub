export function getInitials(fullName = '') {
  return String(fullName).trim().split(/\s+/).filter(Boolean).slice(0, 2).map((part) => part[0]).join('').toUpperCase() || '?'
}

export function formatProgress(done = 0, total = 0) { return `${done}/${total}` }
