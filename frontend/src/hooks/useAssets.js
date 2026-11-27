import { useCallback, useEffect, useRef, useState } from 'react'
import { listAssets } from '@/api/assetApi.js'
import { isRequestCanceled } from '@/utils/errors.js'

export function useAssets(filters = {}) {
  const { type, status, search } = filters
  const [assets, setAssets] = useState([])
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
      const result = await listAssets({ type, status, search, signal: nextController.signal })
      if (id !== requestId.current) return
      setAssets(result || [])
      setError(null)
      hasLoaded.current = true
    } catch (requestError) {
      if (id !== requestId.current || isRequestCanceled(requestError)) return
      setError(requestError)
      hasLoaded.current = true
    } finally {
      if (id === requestId.current) setIsLoading(false)
    }
  }, [type, status, search])
  useEffect(() => {
    void refresh()
    return () => { requestId.current += 1; controller.current?.abort() }
  }, [refresh])
  return { assets, isLoading, error, refresh }
}
