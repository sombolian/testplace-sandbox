import { Flag } from '../components/Flag'
import { formatArea, formatNumber } from '@/lib/format'
import type { CountryMeta } from '@/lib/types'
import { OVERLAYS } from '@/data/overlays'

interface CompareRowProps {
  label: string
  a: string
  b: string
  highlight?: 'a' | 'b' | null
}

function CompareRow({ label, a, b, highlight }: CompareRowProps) {
  return (
    <div className="grid grid-cols-[1fr_auto_1fr] items-center gap-2 py-1.5 border-b border-white/5 last:border-b-0">
      <div
        className={`text-sm text-right ${highlight === 'a' ? 'text-accent-400 font-semibold' : 'text-slate-200'}`}
      >
        {a}
      </div>
      <div className="text-[10px] uppercase tracking-wider text-slate-500 px-2">{label}</div>
      <div
        className={`text-sm ${highlight === 'b' ? 'text-orange-300 font-semibold' : 'text-slate-200'}`}
      >
        {b}
      </div>
    </div>
  )
}

function chooseHigher(a: number | null | undefined, b: number | null | undefined): 'a' | 'b' | null {
  if (a == null || b == null) return null
  if (a > b) return 'a'
  if (b > a) return 'b'
  return null
}

interface ComparePanelProps {
  a: CountryMeta
  b: CountryMeta
  onSwap: () => void
  onClear: () => void
  onClose: () => void
}

export function ComparePanel({ a, b, onSwap, onClear, onClose }: ComparePanelProps) {
  const sharedMemberships = OVERLAYS.filter(
    (o) => o.members.includes(a.iso3) && o.members.includes(b.iso3),
  )

  return (
    <div className="bg-space-900/90 backdrop-blur-xl border border-white/10 rounded-2xl shadow-2xl flex flex-col h-full">
      <div className="flex items-center justify-between px-5 py-4 border-b border-white/5">
        <div className="text-sm font-semibold text-slate-200 flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-accent-500" /> vs <span className="w-2 h-2 rounded-full bg-orange-500" />
          Compare
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={onSwap}
            className="text-xs text-slate-400 hover:text-slate-100 px-2 py-1 rounded hover:bg-white/5"
          >
            Swap
          </button>
          <button
            onClick={onClear}
            className="text-xs text-slate-400 hover:text-slate-100 px-2 py-1 rounded hover:bg-white/5"
          >
            Clear
          </button>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-slate-100 text-lg leading-none px-1"
            aria-label="Close compare"
          >
            ×
          </button>
        </div>
      </div>

      <div className="px-5 py-4 grid grid-cols-2 gap-4 border-b border-white/5">
        <div className="flex items-center gap-2 min-w-0">
          <Flag iso2={a.iso2} className="!w-9 !h-6 shrink-0 ring-1 ring-white/10" />
          <div className="min-w-0">
            <div className="text-[10px] uppercase text-accent-400 tracking-wider">Country A</div>
            <div className="text-base font-semibold truncate">{a.name}</div>
          </div>
        </div>
        <div className="flex items-center gap-2 min-w-0">
          <Flag iso2={b.iso2} className="!w-9 !h-6 shrink-0 ring-1 ring-white/10" />
          <div className="min-w-0">
            <div className="text-[10px] uppercase text-orange-300 tracking-wider">Country B</div>
            <div className="text-base font-semibold truncate">{b.name}</div>
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-y-auto px-5 py-3">
        <CompareRow label="Capital" a={a.capital ?? '—'} b={b.capital ?? '—'} />
        <CompareRow
          label="Population"
          a={formatNumber(a.population)}
          b={formatNumber(b.population)}
          highlight={chooseHigher(a.population, b.population)}
        />
        <CompareRow
          label="Area"
          a={formatArea(a.area)}
          b={formatArea(b.area)}
          highlight={chooseHigher(a.area, b.area)}
        />
        <CompareRow label="Region" a={a.region} b={b.region} />
        <CompareRow label="Subregion" a={a.subregion ?? '—'} b={b.subregion ?? '—'} />
        <CompareRow label="Languages" a={`${a.languages.length}`} b={`${b.languages.length}`} highlight={chooseHigher(a.languages.length, b.languages.length)} />
        <CompareRow
          label="Currencies"
          a={a.currencies.map((c) => c.code).join(', ') || '—'}
          b={b.currencies.map((c) => c.code).join(', ') || '—'}
        />
        <CompareRow label="Demonym" a={a.demonym ?? '—'} b={b.demonym ?? '—'} />
        <CompareRow label="UN member" a={a.unMember ? 'Yes' : 'No'} b={b.unMember ? 'Yes' : 'No'} />
        <CompareRow label="Landlocked" a={a.landlocked ? 'Yes' : 'No'} b={b.landlocked ? 'Yes' : 'No'} />
        <CompareRow label="ISO α-3" a={a.iso3} b={b.iso3} />
        <CompareRow label="FIFA" a={a.fifa ?? '—'} b={b.fifa ?? '—'} />

        {sharedMemberships.length > 0 && (
          <div className="mt-4 pt-3 border-t border-white/5">
            <div className="text-[10px] uppercase tracking-wider text-slate-400 mb-1.5">
              Shared memberships
            </div>
            <div className="flex flex-wrap gap-1.5">
              {sharedMemberships.map((m) => (
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
          </div>
        )}
      </div>
    </div>
  )
}
