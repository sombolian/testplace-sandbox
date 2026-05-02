import { useGlobeStore } from '@/state/useGlobeStore'
import type { CountryMetaMap } from '@/lib/types'
import { Flag } from './components/Flag'

export function HoverHUD({ meta }: { meta: CountryMetaMap }) {
  const hoveredISO = useGlobeStore((s) => s.hoveredISO)
  if (!hoveredISO) return null
  const m = meta[hoveredISO]
  if (!m) return null
  return (
    <div className="pointer-events-none fixed bottom-4 left-1/2 -translate-x-1/2 z-30 bg-space-900/90 backdrop-blur-md border border-white/10 rounded-full px-3 py-1.5 flex items-center gap-2 shadow-xl animate-fade-in">
      <Flag iso2={m.iso2} className="!w-5 !h-3.5 ring-1 ring-white/10" />
      <span className="text-sm font-medium">{m.name}</span>
      <span className="text-xs text-slate-400">{m.capital ?? ''}</span>
    </div>
  )
}
