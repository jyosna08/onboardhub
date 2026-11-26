import { ROLES } from './constants.js'

export function getHomePathForRole(role) {
  if (role === ROLES.EMPLOYEE) return '/my-onboarding'
  if ([ROLES.ADMIN, ROLES.HR, ROLES.IT].includes(role)) return '/dashboard'
  return '/login'
}
