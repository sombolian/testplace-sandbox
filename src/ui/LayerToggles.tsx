import { useGlobeStore } from '@/state/useGlobeStore'
import type { LayerMode } from '@/lib/types'

const MODES: { id: LayerMode; label: string; icon: string }[] = [
  { id: 'political', label: 'Political', icon: '🗺️' },
  { id: 'physical', label: 'Physical', icon: '⛰️' },
  { id: 'night', label: 'Night', icon: '🌃' },
]

export function LayerToggles() {
  const layerMode = useGlobeStore((s) => s.layerMode)
  const setLayer = useGlobeStore((s) => s.setLayer)
  const showClouds = useGlobeStore((s) => s.showClouds)
  const showAtmosphere = useGlobeStore((s) => s.showAtmosphere)
  const autoRotate = useGlobeStore((s) => s.autoRotate)
  const toggleClouds = useGlobeStore((s) => s.toggleClouds)
  const toggleAtmosphere = useGlobeStore((s) => s.toggleAtmosphere)
  const toggleAutoRotate = useGlobeStore((s) => s.toggleAutoRotate)

  return (
    <div className="bg-space-900/80 backdrop-blur-md border border-white/10 rounded-xl p-3 space-y-3">
      <div>
        <div className="text-[10px] uppercase tracking-wider text-slate-400 mb-1.5">Layer</div>
        <div className="grid grid-cols-3 gap-1">
          {MODES.map((m) => (
            <button
              key={m.id}
              onClick={() => setLayer(m.id)}
              className={`text-xs py-1.5 rounded-lg transition border ${
                layerMode === m.id
                  ? 'bg-accent-500/20 border-accent-500/50 text-accent-400'
                  : 'bg-white/5 border-white/5 text-slate-300 hover:bg-white/10'
              }`}
            >
              <div className="text-base leading-none mb-0.5">{m.icon}</div>
              {m.label}
            </button>
          ))}
        </div>
      </div>

      <div className="flex flex-col gap-1.5">
        <Toggle label="Clouds" on={showClouds} onChange={toggleClouds} />
        <Toggle label="Atmosphere" on={showAtmosphere} onChange={toggleAtmosphere} />
        <Toggle label="Auto-rotate" on={autoRotate} onChange={toggleAutoRotate} />
      </div>
    </div>
  )
}

function Toggle({ label, on, onChange }: { label: string; on: boolean; onChange: () => void }) {
  return (
    <button
      onClick={onChange}
      className="flex items-center justify-between text-xs text-slate-300 hover:text-slate-100 transition"
    >
      <span>{label}</span>
      <span
        className={`relative inline-flex w-8 h-4 rounded-full transition ${
          on ? 'bg-accent-600' : 'bg-white/10'
        }`}
      >
        <span
          className={`absolute top-0.5 w-3 h-3 bg-white rounded-full shadow transition-all ${
            on ? 'left-4' : 'left-0.5'
          }`}
        />
      </span>
    </button>
  )
}
