# Atlas3D — Interactive 3D Earth

A draggable, high-fidelity 3D Earth for teaching **geography, politics, geopolitics, and vexillology**.
Built with Vite + React 18 + TypeScript + react-three-fiber.

## Features

- Realistic Earth with day/night blending driven by the current sub-solar point, specular oceans, normal-mapped relief, drifting cloud layer, atmospheric fresnel glow and a Milky Way starfield.
- Click any country to open a panel with flag, capital, population, languages, currencies, demonym, time zones, ISO codes, and FIFA code.
- Search by name / capital / ISO code (⌘K) with smooth fly-to camera animation.
- Layer modes: Political / Physical / Night.
- Geopolitics overlays: NATO, EU, BRICS+, Commonwealth, OPEC+, G7, G20, ASEAN, African Union, Mercosur, Arab League — multiple can be combined.
- Two-country compare mode with side-by-side stats and shared-membership detection.
- Toggleable clouds, atmosphere and auto-rotation.

## Stack

- `vite`, `react`, `typescript`
- `@react-three/fiber`, `@react-three/drei`, `three`
- `zustand` (state), `fuse.js` (search), `tailwindcss` (styling), `flag-icons` (SVG flags)
- `earcut`, `topojson-client`, `d3-geo` (geometry)

## Getting started

```bash
npm install
npm run dev          # http://localhost:5173
npm run build        # production bundle in dist/
npm run preview      # serve the built bundle
```

`scripts/build-data.ts` is the build-time data pipeline that joins Natural Earth admin-0 1:50m boundaries (via `world-atlas` TopoJSON) with a REST Countries snapshot. It writes:

- `public/data/countries.geojson` — feature-id-keyed GeoJSON with ISO α-3 codes
- `public/data/countries-meta.json` — typed country metadata
- `public/data/iso-index.json` — sorted list of ISO α-3 codes

Run it with:

```bash
npx tsx scripts/build-data.ts
```

## Controls

- **Drag** to rotate.
- **Scroll / pinch** to zoom.
- **⌘K** to focus the search bar.
- **Click** a country, then **Compare** to enter compare mode and click a second country.

## Data sources / credits

- Earth, cloud, normal, specular and night textures: [three.js examples](https://github.com/mrdoob/three.js/tree/dev/examples/textures/planets) (originally NASA / Tom Patterson).
- Country boundaries: [Natural Earth](https://www.naturalearthdata.com/) via the [`world-atlas`](https://github.com/topojson/world-atlas) TopoJSON.
- Country metadata: [REST Countries](https://restcountries.com/) snapshot.
- Flag SVGs: [`flag-icons`](https://github.com/lipis/flag-icons) by Panayiotis Lipiridis.
- Milky Way panorama: ESO / three.js examples.
