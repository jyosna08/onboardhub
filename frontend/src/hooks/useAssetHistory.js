import { useCallback, useEffect, useRef, useState } from 'react'
import { getAssetHistory } from '@/api/assetApi.js'
import { isRequestCanceled } from '@/utils/errors.js'

export function useAssetHistory(assetId, enabled = true) {
  const [history, setHistory] = useState([])
  const [isLoading, setIsLoading] = useState(Boolean(enabled))
  const [error, setError] = useState(null)
  const requestId = useRef(0)
  const controller = useRef(null)
  const refresh = useCallback(async () => {
    if (!enabled || !assetId) return
    controller.current?.abort()
    const nextController = new AbortController()
    controller.current = nextController
    const id = ++requestId.current
    setIsLoading(true)
    try {
      const entries = await getAssetHistory(assetId, { signal: nextController.signal })
      if (id !== requestId.current) return
      setHistory(entries || [])
      setError(null)
    } catch (requestError) {
      if (id !== requestId.current || isRequestCanceled(requestError)) return
      setError(requestError)
    } finally {
      if (id === requestId.current) setIsLoading(false)
    }
  }, [assetId, enabled])
  useEffect(() => {
    if (!enabled) { setHistory([]); setError(null); setIsLoading(false); return undefined }
    void refresh()
    return () => { requestId.current += 1; controller.current?.abort() }
  }, [refresh, enabled])
  return { history, isLoading, error, refresh }
}
