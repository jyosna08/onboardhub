import { ASSET_FIELD_LIMITS, EMAIL_MAX_LENGTH, FORM_LIMITS, PASSWORD_MAX_LENGTH, PASSWORD_MIN_LENGTH } from './constants.js'

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

export function validateCreateEmployee({ fullName = '', email = '', jobTitle = '', joiningDate = '' } = {}) {
  const errors = {}
  if (!fullName.trim()) errors.fullName = 'Full name is required.'
  else if (fullName.trim().length > 100) errors.fullName = 'Name must be 100 characters or fewer.'
  if (!email.trim()) errors.email = 'Email is required.'
  else if (email.length > EMAIL_MAX_LENGTH) errors.email = `Email must be ${EMAIL_MAX_LENGTH} characters or fewer.`
  else if (!emailPattern.test(email.trim())) errors.email = 'Enter a valid email address.'
  if (!jobTitle.trim()) errors.jobTitle = 'Job title is required.'
  else if (jobTitle.trim().length > 100) errors.jobTitle = 'Job title must be 100 characters or fewer.'
  if (!joiningDate) errors.joiningDate = 'Joining date is required.'
  return errors
}

export function validateCreateAsset({ type = '', make = '', model = '', serialNumber = '', purchaseDate = '' } = {}, now = new Date()) {
  const errors = {}
  if (!type) errors.type = 'Asset type is required.'
  if (!make.trim()) errors.make = 'Make is required.'
  else if (make.trim().length > ASSET_FIELD_LIMITS.make) errors.make = `Make must be ${ASSET_FIELD_LIMITS.make} characters or fewer.`
  if (!model.trim()) errors.model = 'Model is required.'
  else if (model.trim().length > ASSET_FIELD_LIMITS.model) errors.model = `Model must be ${ASSET_FIELD_LIMITS.model} characters or fewer.`
  if (!serialNumber.trim()) errors.serialNumber = 'Serial number is required.'
  else if (serialNumber.trim().length > ASSET_FIELD_LIMITS.serialNumber) errors.serialNumber = `Serial number must be ${ASSET_FIELD_LIMITS.serialNumber} characters or fewer.`
  if (purchaseDate) {
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    if (purchaseDate > today) errors.purchaseDate = 'Purchase date cannot be in the future.'
  }
  return errors
}

export function validateCreateUser({ fullName = '', email = '', password = '', role = '' } = {}) {
  const errors = {}
  if (!fullName.trim()) errors.fullName = 'Full name is required.'
  else if (fullName.trim().length > FORM_LIMITS.fullName.max) errors.fullName = `Name must be ${FORM_LIMITS.fullName.max} characters or fewer.`
  if (!email.trim()) errors.email = 'Email is required.'
  else if (email.length > EMAIL_MAX_LENGTH) errors.email = `Email must be ${EMAIL_MAX_LENGTH} characters or fewer.`
  else if (!emailPattern.test(email.trim())) errors.email = 'Enter a valid email address.'
  if (!password) errors.password = 'Password is required.'
  else if (password.length < PASSWORD_MIN_LENGTH || password.length > PASSWORD_MAX_LENGTH) errors.password = `Password must be ${PASSWORD_MIN_LENGTH}–${PASSWORD_MAX_LENGTH} characters.`
  if (!role) errors.role = 'Role is required.'
  return errors
}
