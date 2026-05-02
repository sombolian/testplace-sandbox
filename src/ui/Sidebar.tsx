import { useGlobeStore } from '@/state/useGlobeStore'
import { LayerToggles } from './LayerToggles'
import { OverlayToggles } from './OverlayToggles'

export function Sidebar() {
  const compareMode = useGlobeStore((s) => s.compareMode)
  const setCompareMode = useGlobeStore((s) => s.setCompareMode)

  return (
    <aside className="hidden md:flex w-64 flex-col gap-3 pointer-events-auto">
      <header className="bg-space-900/80 backdrop-blur-md border border-white/10 rounded-xl p-3">
        <div className="flex items-center gap-2">
          <div className="w-7 h-7 rounded-full bg-gradient-to-br from-accent-400 to-blue-700 ring-1 ring-accent-400/40 shadow" />
          <div>
            <div className="font-semibold tracking-tight">Atlas3D</div>
            <div className="text-[11px] text-slate-400 -mt-0.5">Interactive 3D Earth</div>
          </div>
        </div>
        <p className="mt-2 text-[11px] text-slate-400 leading-snug">
          Drag to rotate. Scroll/pinch to zoom. Click any country for details, or pair the geopolitics overlays with compare mode.
        </p>
      </header>

      <LayerToggles />
      <OverlayToggles />

      <button
        onClick={() => setCompareMode(!compareMode)}
        className={`text-sm font-medium py-2.5 rounded-xl border transition ${
          compareMode
            ? 'bg-orange-500/20 border-orange-500/40 text-orange-200'
            : 'bg-white/5 border-white/10 text-slate-200 hover:bg-white/10'
        }`}
      >
        {compareMode ? 'Compare mode: ON' : 'Compare two countries'}
      </button>
    </aside>
  )
}
