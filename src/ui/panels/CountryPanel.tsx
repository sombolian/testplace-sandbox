import { useMemo } from 'react'
import { Flag } from '../components/Flag'
import { Stat } from '../components/Stat'
import { formatArea, formatList, formatNumber } from '@/lib/format'
import type { CountryMeta } from '@/lib/types'
import { OVERLAYS } from '@/data/overlays'
import { useGlobeStore } from '@/state/useGlobeStore'

interface CountryPanelProps {
  meta: CountryMeta
  onClose: () => void
  onFlyTo: () => void
  onCompare: () => void
  isCompare?: boolean
}

export function CountryPanel({ meta, onClose, onFlyTo, onCompare, isCompare }: CountryPanelProps) {
  const compareMode = useGlobeStore((s) => s.compareMode)
  const memberships = useMemo(
    () => OVERLAYS.filter((o) => o.members.includes(meta.iso3)),
    [meta.iso3],
  )

  return (
    <div
      className={`flex h-full flex-col gap-4 overflow-hidden bg-space-900/85 backdrop-blur-xl border ${
        isCompare ? 'border-orange-500/30' : 'border-accent-500/20'
      } rounded-2xl shadow-2xl`}
    >
      <div className="relative px-5 pt-5">
        <button
          onClick={onClose}
          className="absolute right-3 top-3 text-slate-400 hover:text-slate-100 text-lg leading-none"
          aria-label="Close country panel"
        >
          ×
        </button>
        <div className="flex items-center gap-3">
          <Flag iso2={meta.iso2} className="!w-10 !h-7 shadow-md ring-1 ring-white/10" />
          <div className="min-w-0">
            <div className="text-xs text-slate-400 truncate">{meta.officialName}</div>
            <h2 className="text-xl font-semibold leading-tight truncate">{meta.name}</h2>
          </div>
        </div>
        <div className="mt-3 flex flex-wrap gap-1.5">
          <span className="px-2 py-0.5 rounded-full text-[10px] uppercase tracking-wider bg-space-700 text-slate-300">
            {meta.region}
          </span>
          {meta.subregion && (
            <span className="px-2 py-0.5 rounded-full text-[10px] uppercase tracking-wider bg-space-700/60 text-slate-400">
              {meta.subregion}
            </span>
          )}
          {meta.unMember && (
            <span className="px-2 py-0.5 rounded-full text-[10px] uppercase tracking-wider bg-blue-900/50 text-blue-200">
              UN member
            </span>
          )}
          {meta.landlocked && (
            <span className="px-2 py-0.5 rounded-full text-[10px] uppercase tracking-wider bg-amber-900/40 text-amber-200">
              Landlocked
            </span>
          )}
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-5 pb-4 space-y-4">
        <section className="grid grid-cols-2 gap-3">
          <Stat label="Capital" value={meta.capital ?? '—'} />
          <Stat label="Population" value={formatNumber(meta.population)} hint={meta.population.toLocaleString()} />
          <Stat label="Area" value={formatArea(meta.area)} />
          <Stat label="Demonym" value={meta.demonym ?? '—'} />
        </section>

        <section>
          <div className="text-[10px] uppercase tracking-wider text-slate-400 mb-1">Languages</div>
          <div className="text-sm text-slate-200">{formatList(meta.languages, 6)}</div>
        </section>

        <section>
          <div className="text-[10px] uppercase tracking-wider text-slate-400 mb-1">Currencies</div>
          <div className="text-sm text-slate-200">
            {meta.currencies.length
              ? meta.currencies.map((c) => `${c.name}${c.symbol ? ` (${c.symbol})` : ''}`).join(', ')
              : '—'}
          </div>
        </section>

        <section>
          <div className="text-[10px] uppercase tracking-wider text-slate-400 mb-1">Time zones</div>
          <div className="text-sm text-slate-200">{formatList(meta.timezones, 4)}</div>
        </section>

        {memberships.length > 0 && (
          <section>
            <div className="text-[10px] uppercase tracking-wider text-slate-400 mb-1.5">Memberships</div>
            <div className="flex flex-wrap gap-1.5">
              {memberships.map((m) => (
                <span
                  key={m.id}
                  className="px-2 py-0.5 rounded-full text-[11px]"
                  style={{
                    background: `${m.color}22`,
                    color: m.color,
                    border: `1px solid ${m.color}55`,
                  }}
                >
                  {m.label}
                </span>
              ))}
            </div>
          </section>
        )}

        <section>
          <div className="text-[10px] uppercase tracking-wider text-slate-400 mb-1">Codes</div>
          <div className="grid grid-cols-3 gap-3">
            <Stat label="ISO α-2" value={meta.iso2} />
            <Stat label="ISO α-3" value={meta.iso3} />
            <Stat label="FIFA" value={meta.fifa ?? '—'} />
          </div>
        </section>
      </div>

      <div className="px-5 pb-5 pt-3 flex gap-2 border-t border-white/5">
        <button
          onClick={onFlyTo}
          className="flex-1 text-sm font-medium bg-accent-600 hover:bg-accent-500 text-white py-2 rounded-lg transition"
        >
          Fly here
        </button>
        {!isCompare && (
          <button
            onClick={onCompare}
            className={`flex-1 text-sm font-medium py-2 rounded-lg transition border ${
              compareMode
                ? 'bg-orange-500/15 border-orange-500/40 text-orange-200 hover:bg-orange-500/25'
                : 'bg-white/5 border-white/10 text-slate-200 hover:bg-white/10'
            }`}
          >
            {compareMode ? 'Pick comparison' : 'Compare'}
          </button>
        )}
      </div>
    </div>
  )
}
