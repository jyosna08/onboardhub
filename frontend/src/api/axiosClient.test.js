import { afterEach, describe, expect, it, vi } from 'vitest'
import axiosClient, { setUnauthorizedHandler } from './axiosClient.js'
import { clearSession, saveSession } from '@/utils/authStorage.js'

describe('axios client', () => {
  afterEach(() => { clearSession(); setUnauthorizedHandler(null) })

  it('adds the stored bearer token to requests', async () => {
    saveSession({ token: 'signed-token' })
    let authorization
    await axiosClient.get('/probe', { adapter: async (config) => { authorization = config.headers.Authorization; return { config, data: {}, status: 200, statusText: 'OK', headers: {} } } })
    expect(authorization).toBe('Bearer signed-token')
  })

  it('calls the unauthorized handler for 401 except when skipped', async () => {
    const handler = vi.fn(); setUnauthorizedHandler(handler)
    const adapter = async (config) => Promise.reject(Object.assign(new Error('unauthorized'), { config, response: { status: 401, config } }))
    await expect(axiosClient.get('/private', { adapter })).rejects.toThrow('unauthorized')
    await expect(axiosClient.get('/login', { adapter, skipAuthRedirect: true })).rejects.toThrow('unauthorized')
    expect(handler).toHaveBeenCalledTimes(1)
  })
})
