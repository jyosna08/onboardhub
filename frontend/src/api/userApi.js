import axiosClient from './axiosClient.js'

export async function createUser({ fullName, email, password, role }) {
  const response = await axiosClient.post('/api/users', { fullName, email, password, role })
  return response.data
}

export async function listUsers({ role, signal } = {}) {
  const params = role ? { role } : {}
  const response = await axiosClient.get('/api/users', { params, signal })
  return response.data
}
