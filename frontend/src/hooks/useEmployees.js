import { useCallback, useEffect, useRef, useState } from 'react'
import { listEmployees } from '@/api/employeeApi.js'
import { isRequestCanceled } from '@/utils/errors.js'

export function useEmployees(filters = {}) {
  const { status, department, search } = filters
  const [employees, setEmployees] = useState([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState(null)
  const requestId = useRef(0)
  const controller = useRef(null)
  const hasLoaded = useRef(false)

  const refresh = useCallback(async () => {
    controller.current?.abort()
    const nextController = new AbortController()
    controller.current = nextController
    const id = ++requestId.current
    if (!hasLoaded.current) setIsLoading(true)
    try {
      const results = await listEmployees({ status, department, search, signal: nextController.signal })
      if (id !== requestId.current) return
      setEmployees(results || [])
      setError(null)
      hasLoaded.current = true
    } catch (requestError) {
      if (id !== requestId.current || isRequestCanceled(requestError)) return
      setError(requestError)
      hasLoaded.current = true
    } finally {
      if (id === requestId.current) setIsLoading(false)
    }
  }, [status, department, search])

  useEffect(() => {
    void refresh()
    return () => { requestId.current += 1; controller.current?.abort() }
  }, [refresh])

  return { employees, isLoading, error, refresh }
}
