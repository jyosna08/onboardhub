import { useState } from 'react'
import Button from '@/components/common/Button.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import FormField from '@/components/common/FormField.jsx'
import { createUser } from '@/api/userApi.js'
import { ADMIN_MANAGED_ROLES, EMAIL_MAX_LENGTH, FORM_LIMITS } from '@/utils/constants.js'
import { ROLE_LABELS } from '@/utils/display.js'
import { getErrorMessage } from '@/utils/errors.js'
import { validateCreateUser } from '@/utils/validators.js'
import { useSubmitGuard } from '@/hooks/useSubmitGuard.js'

const initialValues = { fullName: '', email: '', password: '', role: ADMIN_MANAGED_ROLES[0] }

export default function CreateUserForm({ onCreated }) {
  const [values, setValues] = useState(initialValues)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const { isSubmitting, runOnce } = useSubmitGuard()
  function updateField(field, value) { setValues((current) => ({ ...current, [field]: value })); setErrors((current) => ({ ...current, [field]: '' })); setApiError('') }
  async function handleSubmit(event) {
    event.preventDefault()
    const validation = validateCreateUser(values)
    setErrors(validation); setApiError('')
    if (Object.keys(validation).length) return
    await runOnce(async () => {
      try { const user = await createUser({ ...values, fullName: values.fullName.trim(), email: values.email.trim().toLowerCase() }); onCreated?.(user); setValues(initialValues); setErrors({}) }
      catch (error) { if (error?.response?.status === 409) setErrors((current) => ({ ...current, email: 'An account with this email already exists.' })); else setApiError(getErrorMessage(error)) }
    })
  }
  return <section className="rounded-xl border border-gray-200 bg-white p-5 shadow-sm sm:p-6"><h2 className="text-lg font-semibold text-gray-900">Create team account</h2><p className="mt-1 text-sm text-gray-500">Create an administrator or service team account.</p><form onSubmit={handleSubmit} noValidate className="mt-5 space-y-4">{apiError && <ErrorBanner message={apiError} onDismiss={() => setApiError('')}/>}<div className="grid gap-4 sm:grid-cols-2"><FormField id="user-full-name" label="Full name" error={errors.fullName}><input autoComplete="name" maxLength={FORM_LIMITS.fullName.max} value={values.fullName} onChange={(event) => updateField('fullName', event.target.value)}/></FormField><FormField id="user-email" label="Email" error={errors.email}><input type="email" autoComplete="email" maxLength={EMAIL_MAX_LENGTH} value={values.email} onChange={(event) => updateField('email', event.target.value)}/></FormField><FormField id="user-password" label="Password" error={errors.password} hint="Use 8 to 100 characters."><input type="password" autoComplete="new-password" minLength={FORM_LIMITS.password.min} maxLength={FORM_LIMITS.password.max} value={values.password} onChange={(event) => updateField('password', event.target.value)}/></FormField><FormField id="user-role" label="Role" error={errors.role}><select value={values.role} onChange={(event) => updateField('role', event.target.value)}>{ADMIN_MANAGED_ROLES.map((role) => <option key={role} value={role}>{ROLE_LABELS[role]}</option>)}</select></FormField></div><div className="flex justify-end"><Button type="submit" loading={isSubmitting}>Create account</Button></div></form></section>
}
