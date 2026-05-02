import { Canvas } from '@react-three/fiber'
import { Suspense, useEffect, useMemo, useState } from 'react'
import { Vector3 } from 'three'
import { Atmosphere, Clouds, Globe } from './scene/Globe'
import { Stars } from './scene/Stars'
import { CountryLayer, makeCountryIndex } from './scene/CountryLayer'
import { CameraRig } from './scene/CameraRig'
import { Sidebar } from './ui/Sidebar'
import { SearchBar } from './ui/SearchBar'
import { CountryPanel } from './ui/panels/CountryPanel'
import { ComparePanel } from './ui/panels/ComparePanel'
import { HoverHUD } from './ui/HUD'
import { useGlobeStore } from './state/useGlobeStore'
import type { CountryMetaMap } from './lib/types'

interface DataBundle {
  geojson: GeoJSON.FeatureCollection
  meta: CountryMetaMap
  centroids: Map<string, [number, number]>
}

function useCountryData() {
  const [data, setData] = useState<DataBundle | null>(null)
  const [error, setError] = useState<string | null>(null)
  useEffect(() => {
    let alive = true
    const base = import.meta.env.BASE_URL
    Promise.all([
      fetch(`${base}data/countries.geojson`).then((r) => r.json()),
      fetch(`${base}data/countries-meta.json`).then((r) => r.json()),
    ])
      .then(([geojson, meta]) => {
        if (!alive) return
        const centroids = makeCountryIndex(geojson)
        setData({ geojson, meta, centroids })
      })
      .catch((e) => alive && setError(String(e)))
    return () => {
      alive = false
    }
  }, [])
  return { data, error }
}

function LoadingScreen({ message }: { message: string }) {
  return (
    <div className="absolute inset-0 flex flex-col items-center justify-center text-slate-400 gap-3 pointer-events-none">
      <div className="relative w-12 h-12">
        <div className="absolute inset-0 rounded-full bg-gradient-to-br from-accent-400/40 to-blue-900/0 animate-ping" />
        <div className="absolute inset-2 rounded-full bg-gradient-to-br from-accent-400 to-blue-700" />
      </div>
      <div className="text-sm">{message}</div>
    </div>
  )
}

export default function App() {
  const { data, error } = useCountryData()

  const sunDir = useMemo(() => {
    // Approximate current sub-solar point.  This gives a sensible
    // day/night terminator without being a real ephemeris.
    const now = new Date()
    const dayMs = 24 * 60 * 60 * 1000
    const dayFrac = ((now.getUTCHours() * 3600 + now.getUTCMinutes() * 60 + now.getUTCSeconds()) * 1000) / dayMs
    const lon = (0.5 - dayFrac) * 360 // longitude where the sun is overhead
    const dayOfYear =
      (Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()) -
        Date.UTC(now.getUTCFullYear(), 0, 0)) /
      dayMs
    const decl = 23.44 * Math.sin(((dayOfYear - 81) / 365.25) * 2 * Math.PI) // axial tilt approximation
    const lat = decl
    const phi = (lat * Math.PI) / 180
    const theta = (lon * Math.PI) / 180
    return new Vector3(
      Math.cos(phi) * Math.cos(theta),
      Math.sin(phi),
      -Math.cos(phi) * Math.sin(theta),
    ).normalize()
  }, [])

  const selectedISO = useGlobeStore((s) => s.selectedISO)
  const compareISO = useGlobeStore((s) => s.compareISO)
  const compareMode = useGlobeStore((s) => s.compareMode)
  const showClouds = useGlobeStore((s) => s.showClouds)
  const showAtmosphere = useGlobeStore((s) => s.showAtmosphere)
  const flyTo = useGlobeStore((s) => s.flyTo)
  const selectCountry = useGlobeStore((s) => s.selectCountry)
  const setCompareMode = useGlobeStore((s) => s.setCompareMode)
  const clearCompare = useGlobeStore((s) => s.clearCompare)
  const swapCompare = useGlobeStore((s) => s.swapCompare)

  const handlePick = (iso: string | null) => {
    if (!iso) return
    selectCountry(iso)
    if (data && !compareMode) {
      const ll = data.meta[iso]?.latlng ?? data.centroids.get(iso) ?? [0, 0]
      flyTo(ll[0], ll[1], 2.5)
    } else if (data && compareMode) {
      // Only fly to second-pick on compare so the layout stays steady.
      const state = useGlobeStore.getState()
      if (state.selectedISO && state.compareISO === iso) {
        const ll = data.meta[iso]?.latlng ?? data.centroids.get(iso) ?? [0, 0]
        flyTo(ll[0], ll[1], 2.5)
      }
    }
  }

  const selectedMeta = data && selectedISO ? data.meta[selectedISO] : null
  const compareMeta = data && compareISO ? data.meta[compareISO] : null
  const showCompare = !!(compareMode && selectedMeta && compareMeta)

  return (
    <div className="fixed inset-0 overflow-hidden text-slate-100">
      <Canvas
        dpr={[1, 2]}
        gl={{ antialias: true, alpha: false, powerPreference: 'high-performance' }}
        camera={{ position: [0, 0.4, 3.4], fov: 45, near: 0.05, far: 200 }}
      >
        <color attach="background" args={['#05060a']} />
        <ambientLight intensity={0.18} />
        <directionalLight position={[sunDir.x * 5, sunDir.y * 5, sunDir.z * 5]} intensity={1.4} />
        <Suspense fallback={null}>
          <Stars />
          <Globe sunDir={sunDir} />
          {showClouds && <Clouds />}
          {showAtmosphere && <Atmosphere />}
          {data && <CountryLayer geojson={data.geojson} onPick={handlePick} />}
        </Suspense>
        <CameraRig />
      </Canvas>

      {!data && !error && <LoadingScreen message="Spinning up the world…" />}
      {error && (
        <div className="absolute inset-0 flex items-center justify-center text-red-300 text-sm">
          Failed to load data: {error}
        </div>
      )}

      <div className="pointer-events-none absolute inset-0 p-4 flex flex-col gap-3">
        <div className="flex items-start justify-between gap-3">
          <div className="pointer-events-auto">
            <SearchBar
              meta={data?.meta ?? {}}
              centroids={data?.centroids ?? new Map()}
            />
          </div>

          <div className="pointer-events-auto">
            <Sidebar />
          </div>
        </div>

        <div className="flex-1 flex items-end justify-end gap-3 pointer-events-none">
          {selectedMeta && !showCompare && (
            <div className="pointer-events-auto w-[340px] max-h-[80vh] animate-fade-in">
              <CountryPanel
                meta={selectedMeta}
                onClose={() => selectCountry(null)}
                onFlyTo={() => {
                  const ll = selectedMeta.latlng ?? data?.centroids.get(selectedMeta.iso3) ?? [0, 0]
                  flyTo(ll[0], ll[1], 2.0)
                }}
                onCompare={() => {
                  setCompareMode(true)
                }}
              />
            </div>
          )}

          {showCompare && selectedMeta && compareMeta && (
            <div className="pointer-events-auto w-[480px] max-h-[80vh] animate-fade-in">
              <ComparePanel
                a={selectedMeta}
                b={compareMeta}
                onSwap={() => swapCompare()}
                onClear={() => clearCompare()}
                onClose={() => {
                  setCompareMode(false)
                  selectCountry(null)
                }}
              />
            </div>
          )}
        </div>
      </div>

      {data && <HoverHUD meta={data.meta} />}

      {compareMode && !compareMeta && selectedMeta && (
        <div className="pointer-events-none absolute top-20 left-1/2 -translate-x-1/2 bg-orange-500/15 border border-orange-500/40 text-orange-100 text-xs px-3 py-1.5 rounded-full backdrop-blur-md animate-fade-in">
          Now click a second country to compare
        </div>
      )}
    </div>
  )
}
