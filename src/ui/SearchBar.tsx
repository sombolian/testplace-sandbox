import Fuse from 'fuse.js'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Flag } from './components/Flag'
import { useGlobeStore } from '@/state/useGlobeStore'
import type { CountryMetaMap } from '@/lib/types'

interface SearchBarProps {
  meta: CountryMetaMap
  centroids: Map<string, [number, number]>
}

export function SearchBar({ meta, centroids }: SearchBarProps) {
  const [q, setQ] = useState('')
  const [open, setOpen] = useState(false)
  const [activeIdx, setActiveIdx] = useState(0)
  const inputRef = useRef<HTMLInputElement>(null)
  const containerRef = useRef<HTMLDivElement>(null)

  const fuse = useMemo(() => {
    const list = Object.values(meta).map((m) => ({
      iso3: m.iso3,
      iso2: m.iso2,
      name: m.name,
      official: m.officialName,
      capital: m.capital ?? '',
      region: m.region,
    }))
    return new Fuse(list, {
      keys: [
        { name: 'name', weight: 0.6 },
        { name: 'official', weight: 0.2 },
        { name: 'capital', weight: 0.15 },
        { name: 'iso3', weight: 0.05 },
      ],
      threshold: 0.32,
      ignoreLocation: true,
    })
  }, [meta])

  const results = useMemo(() => {
    if (!q.trim()) return []
    return fuse.search(q.trim()).slice(0, 8).map((r) => r.item)
  }, [q, fuse])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
        e.preventDefault()
        inputRef.current?.focus()
        setOpen(true)
      }
      if (e.key === 'Escape') {
        setOpen(false)
        inputRef.current?.blur()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (!containerRef.current?.contains(e.target as Node)) setOpen(false)
    }
    window.addEventListener('mousedown', onClick)
    return () => window.removeEventListener('mousedown', onClick)
  }, [])

  const select = (iso3: string) => {
    const m = meta[iso3]
    if (!m) return
    useGlobeStore.getState().selectCountry(iso3)
    const ll = m.latlng ?? centroids.get(iso3) ?? [0, 0]
    useGlobeStore.getState().flyTo(ll[0], ll[1], 2.4)
    setQ('')
    setOpen(false)
    inputRef.current?.blur()
  }

  return (
    <div ref={containerRef} className="relative w-full max-w-md">
      <div className="flex items-center gap-2 bg-space-900/80 backdrop-blur-md border border-white/10 rounded-xl px-3 py-2 focus-within:border-accent-500/50 transition">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="text-slate-400 shrink-0">
          <circle cx="11" cy="11" r="7" />
          <path d="m21 21-4.3-4.3" />
        </svg>
        <input
          ref={inputRef}
          value={q}
          onChange={(e) => {
            setQ(e.target.value)
            setOpen(true)
            setActiveIdx(0)
          }}
          onFocus={() => setOpen(true)}
          onKeyDown={(e) => {
            if (e.key === 'ArrowDown') {
              e.preventDefault()
              setActiveIdx((i) => Math.min(i + 1, results.length - 1))
            } else if (e.key === 'ArrowUp') {
              e.preventDefault()
              setActiveIdx((i) => Math.max(0, i - 1))
            } else if (e.key === 'Enter' && results[activeIdx]) {
              e.preventDefault()
              select(results[activeIdx].iso3)
            }
          }}
          placeholder="Search a country, capital, or ISO code…"
          className="flex-1 bg-transparent outline-none text-sm text-slate-100 placeholder:text-slate-500"
          aria-label="Search country"
        />
        <kbd className="hidden md:inline-block text-[10px] text-slate-500 border border-white/10 rounded px-1 py-0.5">⌘K</kbd>
      </div>

      {open && results.length > 0 && (
        <div className="absolute mt-1 w-full bg-space-900/95 backdrop-blur-xl border border-white/10 rounded-xl shadow-2xl overflow-hidden z-20 animate-fade-in">
          {results.map((r, i) => (
            <button
              key={r.iso3}
              onClick={() => select(r.iso3)}
              onMouseEnter={() => setActiveIdx(i)}
              className={`w-full flex items-center gap-3 px-3 py-2 text-left text-sm transition ${
                i === activeIdx ? 'bg-accent-500/15' : 'hover:bg-white/5'
              }`}
            >
              <Flag iso2={r.iso2} className="!w-7 !h-5 shrink-0 ring-1 ring-white/10" />
              <div className="flex-1 min-w-0">
                <div className="truncate font-medium">{r.name}</div>
                <div className="text-xs text-slate-400 truncate">
                  {r.capital ? `${r.capital} · ` : ''}
                  {r.region}
                </div>
              </div>
              <span className="text-[10px] text-slate-500">{r.iso3}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
