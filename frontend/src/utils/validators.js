import { EMAIL_MAX_LENGTH, FORM_LIMITS, PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH } from './constants.js'

const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export function validateLogin({ email = '', password = '' } = {}) {
  const errors = {}
  if (!email.trim()) errors.email = 'Email is required.'
  else if (email.length > EMAIL_MAX_LENGTH) errors.email = `Email must be ${EMAIL_MAX_LENGTH} characters or fewer.`
  else if (!emailPattern.test(email.trim())) errors.email = 'Enter a valid email address.'
  if (!password) errors.password = 'Password is required.'
  else if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) errors.password = `Password must be ${PASSWORD_MIN_LENGTH}–${PASSWORD_MAX_LENGTH} characters.`
  return errors
}

export function validateRegister({ fullName = '', email = '', password = '' } = {}) {
  const errors = {}
  if (!fullName.trim()) errors.fullName = 'Full name is required.'
  else if (fullName.trim().length > FORM_LIMITS.fullName.max) errors.fullName = `Name must be ${FORM_LIMITS.fullName.max} characters or fewer.`
  if (!email.trim()) errors.email = 'Email is required.'
  else if (email.length > EMAIL_MAX_LENGTH) errors.email = `Email must be ${EMAIL_MAX_LENGTH} characters or fewer.`
  else if (!emailPattern.test(email.trim())) errors.email = 'Enter a valid email address.'
  if (!password) errors.password = 'Password is required.'
  else if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) errors.password = `Password must be ${PASSWORD_MIN_LENGTH}–${PASSWORD_MAX_LENGTH} characters.`
  return errors
}
