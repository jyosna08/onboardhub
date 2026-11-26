import { describe, expect, it } from 'vitest'
import { decodeJwtPayload, isTokenExpired } from './jwt.js'

function token(payload) {
  const body = btoa(JSON.stringify(payload)).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_')
  return `header.${body}.signature`
}

describe('JWT helpers', () => {
  it('decodes a base64url payload', () => { expect(decodeJwtPayload(token({ exp: 2000, role: 'HR' }))).toMatchObject({ role: 'HR' }) })
  it('treats malformed and expired tokens as expired', () => {
    expect(decodeJwtPayload('bad')).toBeNull()
    expect(isTokenExpired(token({ exp: 100 }), 100000)).toBe(true)
    expect(isTokenExpired(token({ exp: 200 }), 100000)).toBe(false)
  })
})
