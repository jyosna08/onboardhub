import axiosClient from './axiosClient.js'

export async function updateTaskStatus(taskId, status) {
  const response = await axiosClient.patch(`/api/onboarding/tasks/${taskId}/status`, { status })
  return response.data
}

export async function assignAsset(taskId, assetId) {
  const response = await axiosClient.post(`/api/onboarding/tasks/${taskId}/assign-asset`, { assetId })
  return response.data
}
