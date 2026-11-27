import { beforeEach, describe, expect, it } from 'vitest'
import { clearSession, getSession, saveSession } from './authStorage.js'
import { SESSION_STORAGE_KEY } from './constants.js'

describe('auth storage', () => {
  beforeEach(() => clearSession())
  it('saves, reads, and clears the session', () => {
    const session = { token: 'abc', userId: 4 }
    expect(saveSession(session)).toBe(true)
    expect(getSession()).toEqual(session)
    clearSession()
    expect(getSession()).toBeNull()
  })
  it('clears corrupt JSON safely', () => {
    window.localStorage.setItem(SESSION_STORAGE_KEY, '{broken')
    expect(getSession()).toBeNull()
    expect(window.localStorage.getItem(SESSION_STORAGE_KEY)).toBeNull()
  })
})
