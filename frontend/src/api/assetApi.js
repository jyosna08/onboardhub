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
