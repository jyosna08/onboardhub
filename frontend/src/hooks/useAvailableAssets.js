import { useCallback, useEffect, useRef, useState } from 'react'
import { listAssets } from '@/api/assetApi.js'
import { ASSET_STATUS } from '@/utils/constants.js'
import { isRequestCanceled } from '@/utils/errors.js'

export function useAvailableAssets(type, enabled = true) {
  const [assets, setAssets] = useState([])
  const [isLoading, setIsLoading] = useState(Boolean(enabled))
  const [error, setError] = useState(null)
  const requestId = useRef(0)
  const controller = useRef(null)
  const hasLoaded = useRef(false)

  const refresh = useCallback(async () => {
    if (!enabled || !type) return
    controller.current?.abort()
    const nextController = new AbortController()
    controller.current = nextController
    const request = ++requestId.current
    if (!hasLoaded.current) setIsLoading(true)
    try {
      const result = await listAssets({ type, status: ASSET_STATUS.AVAILABLE, signal: nextController.signal })
      if (request !== requestId.current) return
      setAssets((result || []).filter((asset) => asset.type === type && asset.status === ASSET_STATUS.AVAILABLE))
      setError(null)
      hasLoaded.current = true
    } catch (requestError) {
      if (request !== requestId.current || isRequestCanceled(requestError)) return
      setError(requestError)
      hasLoaded.current = true
    } finally {
      if (request === requestId.current) setIsLoading(false)
    }
  }, [type, enabled])

  useEffect(() => {
    if (!enabled) { setAssets([]); setError(null); setIsLoading(false); hasLoaded.current = false; return undefined }
    hasLoaded.current = false
    setIsLoading(true)
    void refresh()
    return () => { requestId.current += 1; controller.current?.abort() }
  }, [refresh, enabled])

  return { assets, isLoading, error, refresh }
}
