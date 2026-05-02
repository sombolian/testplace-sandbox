import { create } from 'zustand'
import type { LayerMode } from '@/lib/types'

export type CompareMode = boolean

interface GlobeState {
  // Selection
  selectedISO: string | null
  compareISO: string | null
  hoveredISO: string | null
  compareMode: CompareMode

  // Layers / overlays
  layerMode: LayerMode
  activeOverlays: Set<string>
  showClouds: boolean
  showAtmosphere: boolean
  autoRotate: boolean

  // Pending fly-to (consumed by CameraRig)
  flyToTarget: { lat: number; lon: number; zoom?: number; nonce: number } | null

  // Actions
  setHovered: (iso: string | null) => void
  selectCountry: (iso: string | null) => void
  setCompareMode: (on: boolean) => void
  clearCompare: () => void
  setLayer: (m: LayerMode) => void
  toggleOverlay: (id: string) => void
  toggleClouds: () => void
  toggleAtmosphere: () => void
  toggleAutoRotate: () => void
  flyTo: (lat: number, lon: number, zoom?: number) => void
  swapCompare: () => void
}

export const useGlobeStore = create<GlobeState>((set, get) => ({
  selectedISO: null,
  compareISO: null,
  hoveredISO: null,
  compareMode: false,
  layerMode: 'political',
  activeOverlays: new Set<string>(),
  showClouds: true,
  showAtmosphere: true,
  autoRotate: false,
  flyToTarget: null,

  setHovered: (iso) => set({ hoveredISO: iso }),

  selectCountry: (iso) => {
    const { compareMode, selectedISO, compareISO } = get()
    if (!iso) {
      set({ selectedISO: null, compareISO: null })
      return
    }
    if (compareMode) {
      if (!selectedISO) {
        set({ selectedISO: iso })
      } else if (iso === selectedISO) {
        // re-clicking primary while compare exists: clear compare, keep primary
        if (compareISO) set({ compareISO: null })
      } else if (iso === compareISO) {
        // re-clicking compare slot: clear it
        set({ compareISO: null })
      } else {
        // either fill compare slot, or replace existing one
        set({ compareISO: iso })
      }
    } else {
      set({ selectedISO: iso, compareISO: null })
    }
  },

  setCompareMode: (on) => {
    if (!on) set({ compareMode: false, compareISO: null })
    else set({ compareMode: true })
  },

  clearCompare: () => set({ compareISO: null }),

  setLayer: (m) => set({ layerMode: m }),

  toggleOverlay: (id) => {
    const next = new Set(get().activeOverlays)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    set({ activeOverlays: next })
  },

  toggleClouds: () => set((s) => ({ showClouds: !s.showClouds })),
  toggleAtmosphere: () => set((s) => ({ showAtmosphere: !s.showAtmosphere })),
  toggleAutoRotate: () => set((s) => ({ autoRotate: !s.autoRotate })),

  flyTo: (lat, lon, zoom) =>
    set({ flyToTarget: { lat, lon, zoom, nonce: Date.now() + Math.random() } }),

  swapCompare: () => {
    const { selectedISO, compareISO } = get()
    set({ selectedISO: compareISO, compareISO: selectedISO })
  },
}))
