import axiosClient from './axiosClient.js'

export async function listAssets({ type, status, search, signal } = {}) {
  const params = Object.fromEntries(Object.entries({ type, status, search })
    .filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== ''))
  const response = await axiosClient.get('/api/assets', { params, signal })
  return response.data
}

export async function getAssetSummary({ signal } = {}) {
  const response = await axiosClient.get('/api/assets/summary', { signal })
  return response.data
}

export async function createAsset(body) {
  const response = await axiosClient.post('/api/assets', body)
  return response.data
}

export async function getAsset(id, { signal } = {}) {
  const response = await axiosClient.get(`/api/assets/${id}`, { signal })
  return response.data
}

export async function updateAssetStatus(id, status) {
  const response = await axiosClient.patch(`/api/assets/${id}/status`, { status })
  return response.data
}

export async function returnAsset(id) {
  const response = await axiosClient.post(`/api/assets/${id}/return`)
  return response.data
}

export async function getAssetHistory(id, { signal } = {}) {
  const response = await axiosClient.get(`/api/assets/${id}/history`, { signal })
  return response.data
}
