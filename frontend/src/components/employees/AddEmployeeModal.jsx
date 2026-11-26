import { useState } from 'react'
import Button from '@/components/common/Button.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import FormField from '@/components/common/FormField.jsx'
import Modal from '@/components/common/Modal.jsx'
import { ASSET_TYPE, DEPARTMENTS, NAME_MAX_LENGTH, EMAIL_MAX_LENGTH } from '@/utils/constants.js'
import { ASSET_TYPE_LABELS, DEPARTMENT_LABELS } from '@/utils/display.js'
import { createEmployee } from '@/api/employeeApi.js'
import { getErrorMessage } from '@/utils/errors.js'
import { validateCreateEmployee } from '@/utils/validators.js'
import { useSubmitGuard } from '@/hooks/useSubmitGuard.js'

const initialValues = { fullName: '', email: '', department: '', jobTitle: '', joiningDate: '' }

export default function AddEmployeeModal({ isOpen, onClose, onCreated }) {
  const [values, setValues] = useState(initialValues)
  const [assetTypes, setAssetTypes] = useState([ASSET_TYPE.LAPTOP])
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const { isSubmitting, runOnce } = useSubmitGuard()

  function updateField(field, value) {
    setValues((current) => ({ ...current, [field]: value }))
    setErrors((current) => ({ ...current, [field]: '' }))
    setApiError('')
  }

  function toggleAssetType(type) {
    setAssetTypes((current) => current.includes(type) ? current.filter((item) => item !== type) : [...current, type])
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const validation = validateCreateEmployee(values)
    setErrors(validation)
    setApiError('')
    if (Object.keys(validation).length) return
    await runOnce(async () => {
      try {
        const employee = await createEmployee({ ...values, fullName: values.fullName.trim(), email: values.email.trim().toLowerCase(), jobTitle: values.jobTitle.trim(), requiredAssetTypes: assetTypes })
        onCreated?.(employee)
        setValues(initialValues)
        setAssetTypes([ASSET_TYPE.LAPTOP])
      } catch (error) {
        if (error?.response?.status === 409) setErrors((current) => ({ ...current, email: 'An employee with this email already exists.' }))
        else setApiError(getErrorMessage(error))
      }
    })
  }

  return <Modal isOpen={isOpen} onClose={onClose} title="Add employee" closeDisabled={isSubmitting}>
    <form onSubmit={handleSubmit} noValidate className="space-y-4">
      {apiError && <ErrorBanner message={apiError} onDismiss={() => setApiError('')}/>}
      <div className="grid gap-4 sm:grid-cols-2">
        <FormField id="employee-name" label="Full name" error={errors.fullName}><input maxLength={NAME_MAX_LENGTH} autoComplete="name" value={values.fullName} onChange={(event) => updateField('fullName', event.target.value)}/></FormField>
        <FormField id="employee-email" label="Email" error={errors.email}><input type="email" maxLength={EMAIL_MAX_LENGTH} autoComplete="email" value={values.email} onChange={(event) => updateField('email', event.target.value)}/></FormField>
        <FormField id="employee-department" label="Department" error={errors.department}><select value={values.department} onChange={(event) => updateField('department', event.target.value)}><option value="">Choose department</option>{Object.values(DEPARTMENTS).map((department) => <option key={department} value={department}>{DEPARTMENT_LABELS[department]}</option>)}</select></FormField>
        <FormField id="employee-job-title" label="Job title" error={errors.jobTitle}><input maxLength={100} value={values.jobTitle} onChange={(event) => updateField('jobTitle', event.target.value)}/></FormField>
        <FormField id="employee-joining-date" label="Joining date" error={errors.joiningDate}><input type="date" value={values.joiningDate} onChange={(event) => updateField('joiningDate', event.target.value)}/></FormField>
      </div>
      <fieldset><legend className="text-sm font-medium text-gray-700">Required equipment</legend><div className="mt-2 flex flex-wrap gap-2">{Object.values(ASSET_TYPE).map((type) => <button key={type} type="button" aria-pressed={assetTypes.includes(type)} onClick={() => toggleAssetType(type)} className={`rounded-full border px-3 py-1.5 text-sm font-medium transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 motion-reduce:transition-none ${assetTypes.includes(type) ? 'border-brand-600 bg-brand-50 text-brand-700' : 'border-gray-300 bg-white text-gray-600 hover:border-gray-400'}`}>{ASSET_TYPE_LABELS[type]}</button>)}</div></fieldset>
      <div className="flex justify-end gap-2 border-t border-gray-100 pt-4"><Button type="button" variant="secondary" onClick={onClose} disabled={isSubmitting}>Cancel</Button><Button type="submit" loading={isSubmitting}>Create employee</Button></div>
    </form>
  </Modal>
}
