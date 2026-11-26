import { createContext, useCallback, useEffect, useMemo, useState } from 'react'
import * as authApi from '@/api/authApi.js'
import { setUnauthorizedHandler } from '@/api/axiosClient.js'
import { clearSession, getSession, saveSession } from '@/utils/authStorage.js'
import { isTokenExpired } from '@/utils/jwt.js'

export const AuthContext = createContext(null)

function getInitialSession() {
  const stored = getSession()
  if (stored?.token && isTokenExpired(stored.token)) { clearSession(); return null }
  return stored
}

export function AuthProvider({ children }) {
  const [session, setSession] = useState(getInitialSession)
  const logout = useCallback(() => { clearSession(); setSession(null) }, [])

  useEffect(() => {
    setUnauthorizedHandler(logout)
    return () => setUnauthorizedHandler(null)
  }, [logout])

  const login = useCallback(async (email, password) => {
    const nextSession = await authApi.login({ email, password })
    saveSession(nextSession)
    setSession(nextSession)
    return nextSession
  }, [])

  const register = useCallback(async (payload) => {
    await authApi.register({ fullName: payload.fullName, email: payload.email, password: payload.password })
    return login(payload.email, payload.password)
  }, [login])

  const value = useMemo(() => ({
    session,
    user: session ? { userId: session.userId, fullName: session.fullName, role: session.role } : null,
    isAuthenticated: Boolean(session?.token),
    login,
    register,
    logout,
  }), [session, login, register, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
