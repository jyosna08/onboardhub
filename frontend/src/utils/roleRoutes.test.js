import { describe, expect, it } from 'vitest'
import { ROLES } from './constants.js'
import { getHomePathForRole } from './roleRoutes.js'

describe('role home routes', () => {
  it('sends each role to its home', () => {
    expect(getHomePathForRole(ROLES.EMPLOYEE)).toBe('/my-onboarding')
    expect(getHomePathForRole(ROLES.ADMIN)).toBe('/dashboard')
    expect(getHomePathForRole(ROLES.HR)).toBe('/dashboard')
    expect(getHomePathForRole(ROLES.IT)).toBe('/dashboard')
    expect(getHomePathForRole('unknown')).toBe('/login')
  })
})
