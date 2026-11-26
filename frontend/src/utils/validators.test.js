import { describe, expect, it } from 'vitest'
import { validateLogin, validateRegister } from './validators.js'

describe('account validators', () => {
  it('requires a valid login email and password', () => {
    expect(validateLogin({})).toMatchObject({ email: 'Email is required.', password: 'Password is required.' })
    expect(validateLogin({ email: 'bad', password: 'short' })).toHaveProperty('email')
    expect(validateLogin({ email: 'a@example.com', password: 'Password@123' })).toEqual({})
  })

  it('enforces registration name, email, and password limits', () => {
    expect(validateRegister({ fullName: ' ', email: 'x@example.com', password: 'Password@123' })).toHaveProperty('fullName')
    expect(validateRegister({ fullName: 'A'.repeat(101), email: 'x@example.com', password: 'Password@123' })).toHaveProperty('fullName')
    expect(validateRegister({ fullName: 'Asha Rao', email: 'x@example.com', password: '1234567' })).toHaveProperty('password')
    expect(validateRegister({ fullName: 'Asha Rao', email: 'asha@example.com', password: 'Password@123' })).toEqual({})
  })
})
