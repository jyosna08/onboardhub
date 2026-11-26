import { useEffect, useId, useRef } from 'react'
import { X } from 'lucide-react'
import Button from './Button.jsx'

const sizes = { sm: 'max-w-md', md: 'max-w-xl', lg: 'max-w-2xl' }

export default function Modal({ isOpen, onClose, title, children, closeDisabled = false, size = 'md' }) {
  const titleId = useId(); const dialogRef = useRef(null); const previousFocus = useRef(null); const closeRef = useRef(onClose)
  closeRef.current = onClose
  useEffect(() => {
    if (!isOpen) return undefined
    previousFocus.current = document.activeElement
    const target = dialogRef.current?.querySelector('button:not(:disabled), input:not(:disabled), select:not(:disabled), a[href]')
    ;(target || dialogRef.current)?.focus()
    function handleKey(event) {
      if (event.key === 'Escape' && !closeDisabled) closeRef.current?.()
      if (event.key !== 'Tab') return
      const items = dialogRef.current?.querySelectorAll('button:not(:disabled), input:not(:disabled), select:not(:disabled), textarea:not(:disabled), a[href], [tabindex]:not([tabindex="-1"])')
      if (!items?.length) { event.preventDefault(); dialogRef.current?.focus(); return }
      if (event.shiftKey && document.activeElement === items[0]) { event.preventDefault(); items[items.length - 1].focus() }
      else if (!event.shiftKey && document.activeElement === items[items.length - 1]) { event.preventDefault(); items[0].focus() }
    }
    document.addEventListener('keydown', handleKey)
    return () => { document.removeEventListener('keydown', handleKey); if (previousFocus.current instanceof HTMLElement) previousFocus.current.focus() }
  }, [isOpen, closeDisabled])
  if (!isOpen) return null
  return <div className="fixed inset-0 z-[60] flex items-center justify-center overflow-y-auto p-4" onMouseDown={(event) => { if (event.target === event.currentTarget && !closeDisabled) onClose?.() }}>
    <div aria-hidden="true" className="fixed inset-0 bg-gray-950/50" />
    <section ref={dialogRef} role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1} className={`relative z-10 max-h-[calc(100vh-2rem)] w-full overflow-y-auto rounded-2xl border border-gray-200 bg-white shadow-2xl ${sizes[size] || sizes.md}`}>
      <header className="flex items-center justify-between border-b border-gray-100 px-5 py-4"><h2 id={titleId} className="text-lg font-semibold text-gray-900">{title}</h2><Button type="button" variant="ghost" size="sm" aria-label="Close dialog" disabled={closeDisabled} onClick={onClose}><X size={18} /></Button></header>
      <div className="p-5">{children}</div>
    </section>
  </div>
}
