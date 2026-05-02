import { useGlobeStore } from '@/state/useGlobeStore'
import { OVERLAYS } from '@/data/overlays'

export function OverlayToggles() {
  const active = useGlobeStore((s) => s.activeOverlays)
  const toggle = useGlobeStore((s) => s.toggleOverlay)

  return (
    <div className="bg-space-900/80 backdrop-blur-md border border-white/10 rounded-xl p-3">
      <div className="flex items-center justify-between mb-1.5">
        <div className="text-[10px] uppercase tracking-wider text-slate-400">Geopolitics</div>
        {active.size > 0 && (
          <button
            className="text-[10px] text-slate-500 hover:text-slate-300 underline"
            onClick={() => {
              for (const id of [...active]) toggle(id)
            }}
          >
            clear
          </button>
        )}
      </div>
      <div className="flex flex-col gap-1 max-h-[260px] overflow-y-auto pr-1">
        {OVERLAYS.map((o) => {
          const on = active.has(o.id)
          return (
            <button
              key={o.id}
              onClick={() => toggle(o.id)}
              className={`flex items-center gap-2 text-xs py-1 px-2 rounded-lg transition text-left ${
                on ? 'bg-white/10' : 'hover:bg-white/5'
              }`}
              title={o.description}
            >
              <span
                className={`w-2.5 h-2.5 rounded-full shrink-0 transition ${on ? '' : 'opacity-30'}`}
                style={{ background: o.color }}
              />
              <span className={`flex-1 ${on ? 'text-slate-100' : 'text-slate-400'}`}>{o.label}</span>
              <span className="text-[10px] text-slate-500">{o.members.length}</span>
            </button>
          )
        })}
      </div>
    </div>
  )
}
