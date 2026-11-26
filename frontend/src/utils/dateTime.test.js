import { describe, expect, it } from 'vitest'
import { formatDate, isBeforeToday, parseApiDateTime, parseDateOnly } from './dateTime.js'

describe('date helpers', () => {
  it('interprets a zone-less timestamp as UTC', () => {
    expect(parseApiDateTime('2026-01-15T12:30:00')?.toISOString()).toBe('2026-01-15T12:30:00.000Z')
  })
  it('formats a date-only value without shifting its calendar day', () => {
    const date = parseDateOnly('2026-01-02')
    expect(date.getFullYear()).toBe(2026)
    expect(date.getMonth()).toBe(0)
    expect(date.getDate()).toBe(2)
    expect(formatDate('2026-01-02', 'en-CA')).toBe('Jan 2, 2026')
  })
  it('compares a date-only value to the local current day', () => {
    const now = new Date(2026, 0, 2, 18)
    expect(isBeforeToday('2026-01-01', now)).toBe(true)
    expect(isBeforeToday('2026-01-02', now)).toBe(false)
    expect(isBeforeToday('not-a-date', now)).toBe(false)
  })
})
