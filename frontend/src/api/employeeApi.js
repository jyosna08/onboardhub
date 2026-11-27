import axiosClient from './axiosClient.js'

export async function listEmployees({ status, department, search, signal } = {}) {
  const params = Object.fromEntries(Object.entries({ status, department, search })
    .filter(([, value]) => value !== undefined && value !== null && String(value).trim() !== ''))
  const response = await axiosClient.get('/api/employees', { params, signal })
  return response.data
}

export async function getEmployee(id, { signal } = {}) {
  const response = await axiosClient.get(`/api/employees/${id}`, { signal })
  return response.data
}

export async function getMyEmployee({ signal } = {}) {
  const response = await axiosClient.get('/api/employees/me', { signal })
  return response.data
}

export async function createEmployee(body) {
  const response = await axiosClient.post('/api/employees', body)
  return response.data
}

export async function updateEmployee(id, body) {
  const response = await axiosClient.patch(`/api/employees/${id}`, body)
  return response.data
}
