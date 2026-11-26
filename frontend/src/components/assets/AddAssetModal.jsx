import { useState } from 'react'
import Button from '@/components/common/Button.jsx'
import ErrorBanner from '@/components/common/ErrorBanner.jsx'
import FormField from '@/components/common/FormField.jsx'
import Modal from '@/components/common/Modal.jsx'
import { createAsset } from '@/api/assetApi.js'
import { useSubmitGuard } from '@/hooks/useSubmitGuard.js'
import { ASSET_FIELD_LIMITS, ASSET_TYPE } from '@/utils/constants.js'
import { ASSET_TYPE_LABELS } from '@/utils/display.js'
import { getErrorMessage } from '@/utils/errors.js'
import { validateCreateAsset } from '@/utils/validators.js'

const initialValues = { type: ASSET_TYPE.LAPTOP, make: '', model: '', serialNumber: '', purchaseDate: '' }
function localDateInputValue(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }

export default function AddAssetModal({ isOpen, onClose, onCreated }) {
  const [values, setValues] = useState(initialValues)
  const [errors, setErrors] = useState({})
  const [apiError, setApiError] = useState('')
  const { isSubmitting, runOnce } = useSubmitGuard()
  function updateField(field, value) { setValues((current) => ({ ...current, [field]: value })); setErrors((current) => ({ ...current, [field]: '' })); setApiError('') }
  async function handleSubmit(event) {
    event.preventDefault()
    const validation = validateCreateAsset(values)
    setErrors(validation); setApiError('')
    if (Object.keys(validation).length) return
    await runOnce(async () => {
      try { const asset = await createAsset({ ...values, make: values.make.trim(), model: values.model.trim(), serialNumber: values.serialNumber.trim() }); onCreated?.(asset); setValues(initialValues) }
      catch (error) { if (error?.response?.status === 409) setErrors((current) => ({ ...current, serialNumber: 'An asset with this serial number already exists.' })); else setApiError(getErrorMessage(error)) }
    })
  }
  return <Modal isOpen={isOpen} onClose={onClose} title="Add asset" closeDisabled={isSubmitting}><form onSubmit={handleSubmit} noValidate className="space-y-4">{apiError && <ErrorBanner message={apiError} onDismiss={() => setApiError('')}/>}<div className="grid gap-4 sm:grid-cols-2"><FormField id="asset-type" label="Type" error={errors.type}><select value={values.type} onChange={(event) => updateField('type', event.target.value)}>{Object.values(ASSET_TYPE).map((type) => <option key={type} value={type}>{ASSET_TYPE_LABELS[type]}</option>)}</select></FormField><FormField id="asset-make" label="Make" error={errors.make}><input value={values.make} maxLength={ASSET_FIELD_LIMITS.make} onChange={(event) => updateField('make', event.target.value)}/></FormField><FormField id="asset-model" label="Model" error={errors.model}><input value={values.model} maxLength={ASSET_FIELD_LIMITS.model} onChange={(event) => updateField('model', event.target.value)}/></FormField><FormField id="asset-serial" label="Serial number" error={errors.serialNumber}><input value={values.serialNumber} maxLength={ASSET_FIELD_LIMITS.serialNumber} onChange={(event) => updateField('serialNumber', event.target.value)}/></FormField><FormField id="asset-purchase-date" label="Purchase date (optional)" error={errors.purchaseDate}><input type="date" value={values.purchaseDate} max={localDateInputValue(new Date())} onChange={(event) => updateField('purchaseDate', event.target.value)}/></FormField></div><div className="flex justify-end gap-2 border-t border-gray-100 pt-4"><Button type="button" variant="secondary" disabled={isSubmitting} onClick={onClose}>Cancel</Button><Button type="submit" loading={isSubmitting}>Create asset</Button></div></form></Modal>
}
