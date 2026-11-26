import { useEffect, useRef } from 'react'

export default function SlideOver({ isOpen, onClose, title, ariaLabel, children, className = '' }) {
  const panelRef = useRef(null); const previousFocus = useRef(null); const closeRef = useRef(onClose)
  closeRef.current = onClose
  useEffect(() => {
    if (!isOpen) return undefined
    previousFocus.current = document.activeElement
    panelRef.current?.focus()
    function handleKey(event) { if (event.key === 'Escape') closeRef.current?.() }
    document.addEventListener('keydown', handleKey)
    return () => { document.removeEventListener('keydown', handleKey); if (previousFocus.current instanceof HTMLElement) previousFocus.current.focus() }
  }, [isOpen])
  if (!isOpen) return null
  return <div className="fixed inset-0 z-50" data-testid="slide-over"><button type="button" aria-label="Close dialog backdrop" onClick={onClose} className="absolute inset-0 h-full w-full cursor-default bg-gray-950/40"/><section ref={panelRef} role="dialog" aria-modal="true" aria-label={ariaLabel || title} tabIndex={-1} className={`absolute inset-y-0 right-0 flex w-full flex-col overflow-y-auto bg-white shadow-2xl md:w-[420px] ${className}`}>{children}</section></div>
}
