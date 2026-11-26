import { useCallback, useEffect, useRef, useState } from 'react'
import { getEmployee } from '@/api/employeeApi.js'
import { isRequestCanceled } from '@/utils/errors.js'

export function useEmployee(id) {
  const [employee, setEmployee] = useState(null)
  const [isLoading, setIsLoading] = useState(Boolean(id))
  const [error, setError] = useState(null)
  const requestId = useRef(0)
  const controller = useRef(null)
  const hasLoaded = useRef(false)

  const refresh = useCallback(async () => {
    if (!id) return
    controller.current?.abort()
    const nextController = new AbortController()
    controller.current = nextController
    const request = ++requestId.current
    if (!hasLoaded.current) setIsLoading(true)
    try {
      const result = await getEmployee(id, { signal: nextController.signal })
      if (request !== requestId.current) return
      setEmployee(result)
      setError(null)
      hasLoaded.current = true
    } catch (requestError) {
      if (request !== requestId.current || isRequestCanceled(requestError)) return
      setError(requestError)
      hasLoaded.current = true
    } finally {
      if (request === requestId.current) setIsLoading(false)
    }
  }, [id])

  useEffect(() => {
    hasLoaded.current = false
    setEmployee(null)
    setError(null)
    setIsLoading(Boolean(id))
    void refresh()
    return () => { requestId.current += 1; controller.current?.abort() }
  }, [refresh, id])

  return { employee, isLoading, error, refresh }
}
