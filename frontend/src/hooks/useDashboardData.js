import { useCallback, useEffect, useRef, useState } from 'react'
import { listEmployees } from '@/api/employeeApi.js'
import { getAssetSummary } from '@/api/assetApi.js'
import { getDashboard } from '@/api/onboardingApi.js'
import { isRequestCanceled } from '@/utils/errors.js'
import { REFRESH_INTERVAL_MS } from '@/utils/constants.js'
import { usePolling } from './usePolling.js'

export function useDashboardData() {
  const [dashboard, setDashboard] = useState(null)
  const [assetSummary, setAssetSummary] = useState(null)
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
    const request = ++requestId.current
    if (!hasLoaded.current) setIsLoading(true)
    try {
      const [nextDashboard, nextSummary, nextEmployees] = await Promise.all([
        getDashboard({ signal: nextController.signal }),
        getAssetSummary({ signal: nextController.signal }),
        listEmployees({ signal: nextController.signal }),
      ])
      if (request !== requestId.current) return
      setDashboard(nextDashboard)
      setAssetSummary(nextSummary)
      setEmployees(nextEmployees || [])
      setError(null)
      hasLoaded.current = true
    } catch (requestError) {
      if (request !== requestId.current || isRequestCanceled(requestError)) return
      setError(requestError)
      hasLoaded.current = true
    } finally {
      if (request === requestId.current) setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
    return () => { requestId.current += 1; controller.current?.abort() }
  }, [refresh])
  usePolling(refresh, REFRESH_INTERVAL_MS)

  return { dashboard, assetSummary, employees, isLoading, error, refresh }
}
