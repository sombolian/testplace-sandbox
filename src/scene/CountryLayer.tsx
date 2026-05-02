import { ThreeEvent } from '@react-three/fiber'
import { useEffect, useMemo, useRef } from 'react'
import {
  BufferGeometry,
  Color,
  DoubleSide,
  Group,
  LineBasicMaterial,
  LineSegments,
  Mesh,
  MeshBasicMaterial,
} from 'three'
import { featureToGeoms } from '@/lib/geo'
import { useGlobeStore } from '@/state/useGlobeStore'
import { OVERLAYS_BY_ID } from '@/data/overlays'

export interface CountryEntry {
  iso3: string
  name: string
  fill: BufferGeometry | null
  border: BufferGeometry | null
  centroid: [number, number]
}

interface CountryLayerProps {
  geojson: GeoJSON.FeatureCollection
  onPick?: (iso: string | null) => void
}

const HIDDEN_COLOR = '#0c1326'
const SELECTED_COLOR = '#7dd3fc'
const COMPARE_COLOR = '#fb923c'
const HOVER_COLOR = '#ffffff'

export function CountryLayer({ geojson, onPick }: CountryLayerProps) {
  const groupRef = useRef<Group>(null!)
  const meshRefs = useRef<Map<string, Mesh>>(new Map())
  const lineRefs = useRef<Map<string, LineSegments>>(new Map())

  const entries = useMemo<CountryEntry[]>(() => {
    return geojson.features
      .map((f) => {
        const id = String(f.id)
        if (!id || id.startsWith('__')) return null
        const name = (f.properties as { name?: string } | null)?.name ?? id
        const { fill, border, centroid } = featureToGeoms(f, 1.001, 1.0014)
        if (!fill && !border) return null
        return { iso3: id, name, fill, border, centroid }
      })
      .filter((x): x is CountryEntry => !!x)
  }, [geojson])

  const selectedISO = useGlobeStore((s) => s.selectedISO)
  const compareISO = useGlobeStore((s) => s.compareISO)
  const hoveredISO = useGlobeStore((s) => s.hoveredISO)
  const activeOverlays = useGlobeStore((s) => s.activeOverlays)
  const layerMode = useGlobeStore((s) => s.layerMode)

  // Apply colours / opacities reactively
  useEffect(() => {
    const overlayColors = new Map<string, Color>()
    const overlayCounts = new Map<string, number>()
    for (const id of activeOverlays) {
      const o = OVERLAYS_BY_ID[id]
      if (!o) continue
      const c = new Color(o.color)
      for (const iso of o.members) {
        const existing = overlayColors.get(iso)
        const cnt = overlayCounts.get(iso) ?? 0
        if (!existing) overlayColors.set(iso, c.clone())
        else existing.lerp(c, 1 / (cnt + 2)) // average colours when multiple overlays overlap
        overlayCounts.set(iso, cnt + 1)
      }
    }

    for (const e of entries) {
      const mesh = meshRefs.current.get(e.iso3)
      const line = lineRefs.current.get(e.iso3)
      if (!mesh) continue

      const mat = mesh.material as MeshBasicMaterial
      const overlayColor = overlayColors.get(e.iso3)
      const isSelected = e.iso3 === selectedISO
      const isCompare = e.iso3 === compareISO
      const isHovered = e.iso3 === hoveredISO

      let color: Color
      let opacity: number

      if (isSelected) {
        color = new Color(SELECTED_COLOR)
        opacity = 0.55
      } else if (isCompare) {
        color = new Color(COMPARE_COLOR)
        opacity = 0.55
      } else if (overlayColor) {
        color = overlayColor
        opacity = 0.55
      } else if (isHovered) {
        color = new Color(HOVER_COLOR)
        opacity = 0.18
      } else {
        color = new Color(HIDDEN_COLOR)
        opacity = 0
      }

      mat.color.copy(color)
      mat.opacity = opacity
      mat.transparent = true
      mat.depthWrite = false

      if (line) {
        const lmat = line.material as LineBasicMaterial
        if (isSelected || isCompare) {
          lmat.color.copy(color).multiplyScalar(1.5)
          lmat.opacity = 0.95
        } else if (overlayColor) {
          lmat.color.copy(overlayColor).multiplyScalar(1.4)
          lmat.opacity = 0.85
        } else if (isHovered) {
          lmat.color.set('#ffffff')
          lmat.opacity = 0.9
        } else {
          // Subtle base border whose visibility depends on layer mode
          lmat.color.set(layerMode === 'physical' ? '#86efac' : '#9ec6ff')
          lmat.opacity = layerMode === 'physical' ? 0.18 : 0.32
        }
        lmat.transparent = true
        lmat.depthWrite = false
      }
    }
  }, [entries, selectedISO, compareISO, hoveredISO, activeOverlays, layerMode])

  const handlePointerOver = (iso: string) => (e: ThreeEvent<PointerEvent>) => {
    e.stopPropagation()
    useGlobeStore.getState().setHovered(iso)
    document.body.style.cursor = 'pointer'
  }
  const handlePointerOut = () => {
    useGlobeStore.getState().setHovered(null)
    document.body.style.cursor = ''
  }
  const handleClick = (iso: string) => (e: ThreeEvent<MouseEvent>) => {
    e.stopPropagation()
    onPick?.(iso)
  }

  return (
    <group ref={groupRef}>
      {entries.map((e) => (
        <group key={e.iso3}>
          {e.fill && (
            <mesh
              ref={(m) => {
                if (m) meshRefs.current.set(e.iso3, m)
              }}
              geometry={e.fill}
              renderOrder={2}
              onPointerOver={handlePointerOver(e.iso3)}
              onPointerOut={handlePointerOut}
              onClick={handleClick(e.iso3)}
              userData={{ iso3: e.iso3 }}
            >
              <meshBasicMaterial
                color={HIDDEN_COLOR}
                transparent
                opacity={0}
                depthWrite={false}
                side={DoubleSide}
              />
            </mesh>
          )}
          {e.border && (
            <lineSegments
              ref={(l) => {
                if (l) lineRefs.current.set(e.iso3, l as LineSegments)
              }}
              geometry={e.border}
              renderOrder={4}
            >
              <lineBasicMaterial
                color="#9ec6ff"
                transparent
                opacity={0.32}
                depthWrite={false}
              />
            </lineSegments>
          )}
        </group>
      ))}
    </group>
  )
}

/**
 * Lightweight centroid index — does not triangulate, just averages
 * coordinates of the largest ring per country.  Used for fly-to.
 */
export function makeCountryIndex(geojson: GeoJSON.FeatureCollection): Map<string, [number, number]> {
  const map = new Map<string, [number, number]>()
  for (const f of geojson.features) {
    const id = String(f.id)
    if (!id || id.startsWith('__')) continue
    if (!f.geometry) continue
    const polys: number[][][][] =
      f.geometry.type === 'Polygon'
        ? [f.geometry.coordinates as number[][][]]
        : f.geometry.type === 'MultiPolygon'
          ? (f.geometry.coordinates as number[][][][])
          : []
    let best: { area: number; lat: number; lon: number } | null = null
    for (const poly of polys) {
      const ring = poly[0]
      if (!ring || !ring.length) continue
      let minLon = Infinity,
        maxLon = -Infinity,
        minLat = Infinity,
        maxLat = -Infinity,
        sumLon = 0,
        sumLat = 0
      for (const [lon, lat] of ring) {
        if (lon < minLon) minLon = lon
        if (lon > maxLon) maxLon = lon
        if (lat < minLat) minLat = lat
        if (lat > maxLat) maxLat = lat
        sumLon += lon
        sumLat += lat
      }
      const area = (maxLon - minLon) * (maxLat - minLat)
      if (!best || area > best.area) {
        best = { area, lat: sumLat / ring.length, lon: sumLon / ring.length }
      }
    }
    if (best) map.set(id, [best.lat, best.lon])
  }
  return map
}
