import axiosClient from './axiosClient.js'

export async function getDashboard({ signal } = {}) {
  const response = await axiosClient.get('/api/onboarding/dashboard', { signal })
  return response.data
}
