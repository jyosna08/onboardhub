import { useCallback, useEffect, useRef, useState } from 'react'
import { getAsset } from '@/api/assetApi.js'
import { isRequestCanceled } from '@/utils/errors.js'

export function useAsset(id, enabled = true) {
  const [asset, setAsset] = useState(null)
  const [isLoading, setIsLoading] = useState(Boolean(enabled))
  const [error, setError] = useState(null)
  const requestId = useRef(0)
  const controller = useRef(null)
  const hasLoaded = useRef(false)
  const refresh = useCallback(async () => {
    if (!enabled || !id) return
    controller.current?.abort()
    const nextController = new AbortController(); controller.current = nextController
    const request = ++requestId.current
    if (!hasLoaded.current) setIsLoading(true)
    try {
      const result = await getAsset(id, { signal: nextController.signal })
      if (request !== requestId.current) return
      setAsset(result); setError(null); hasLoaded.current = true
    } catch (requestError) {
      if (request !== requestId.current || isRequestCanceled(requestError)) return
      setError(requestError); hasLoaded.current = true
    } finally { if (request === requestId.current) setIsLoading(false) }
  }, [id, enabled])
  useEffect(() => {
    if (!enabled) { setAsset(null); setError(null); setIsLoading(false); hasLoaded.current = false; return undefined }
    hasLoaded.current = false; void refresh()
    return () => { requestId.current += 1; controller.current?.abort() }
  }, [enabled, refresh])
  return { asset, isLoading, error, refresh }
}
