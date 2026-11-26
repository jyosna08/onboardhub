const API_DATE_TIME_PATTERN = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2})(\.\d+)?)?)?(Z|[+-]\d{2}:?\d{2})?$/i

export function parseApiDateTime(value) {
  if (typeof value !== 'string' || !value.trim()) return null
  const match = value.trim().match(API_DATE_TIME_PATTERN)
  if (!match || !isValid(match)) return null
  const [, year, month, day, hour, minute, second, fraction, zone] = match
  const time = hour === undefined ? 'T00:00:00' : `T${hour}:${minute}:${second ?? '00'}${normalizeFraction(fraction)}`
  const normalizedZone = zone ? (zone.toUpperCase() === 'Z' ? 'Z' : zone.includes(':') ? zone : `${zone.slice(0, 3)}:${zone.slice(3)}`) : 'Z'
  const parsed = new Date(`${year}-${month}-${day}${time}${normalizedZone}`)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

export function parseDateOnly(value) {
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null
  const [year, month, day] = value.split('-').map(Number)
  const date = new Date(year, month - 1, day)
  if (date.getFullYear() !== year || date.getMonth() !== month - 1 || date.getDate() !== day) return null
  return date
}

export function formatDate(value, locale = undefined) {
  const date = typeof value === 'string' ? parseDateOnly(value) : value
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return ''
  return new Intl.DateTimeFormat(locale, { year: 'numeric', month: 'short', day: 'numeric' }).format(date)
}

export function isBeforeToday(dateOnly, now = new Date()) {
  const date = parseDateOnly(dateOnly)
  if (!date || !(now instanceof Date) || Number.isNaN(now.getTime())) return false
  return date < new Date(now.getFullYear(), now.getMonth(), now.getDate())
}

function normalizeFraction(fraction) { return fraction ? `.${fraction.slice(1, 4).padEnd(3, '0')}` : '' }

function isValid(match) {
  const [, y, m, d, h, minute, second, , zone] = match
  const year = Number(y); const month = Number(m); const day = Number(d)
  const leap = year % 4 === 0 && (year % 100 !== 0 || year % 400 === 0)
  const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31][month - 1]
  if (month < 1 || month > 12 || day < 1 || day > days) return false
  if (h !== undefined && (Number(h) > 23 || Number(minute) > 59 || Number(second ?? 0) > 59)) return false
  if (zone && zone.toUpperCase() !== 'Z') {
    const digits = zone.slice(1).replace(':', '')
    if (Number(digits.slice(0, 2)) > 23 || Number(digits.slice(2, 4)) > 59) return false
  }
  return true
}
