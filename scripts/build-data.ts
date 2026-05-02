/**
 * Build-time data pipeline.
 *
 * Reads:
 *   - /tmp/world-50m.json        (TopoJSON of countries, 1:50m simplified)
 *   - /tmp/restcountries.json    (REST Countries snapshot, batch 1)
 *   - /tmp/restcountries2.json   (REST Countries snapshot, batch 2)
 *
 * Produces:
 *   - public/data/countries.geojson   GeoJSON FeatureCollection, feature.id = ISO3
 *   - public/data/countries-meta.json metadata keyed by ISO3
 *   - public/data/iso-index.json      list of ISO3 codes (search index)
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { feature } from 'topojson-client'
import type { Topology, GeometryCollection } from 'topojson-specification'

interface RestCountry1 {
  name: { common: string; official: string }
  currencies?: Record<string, { name: string; symbol?: string }>
  languages?: Record<string, string>
  cca2: string
  cca3: string
  ccn3?: string
  capital?: string[]
  region: string
  subregion?: string
  population: number
}

interface RestCountry2 {
  cca3: string
  area?: number
  latlng?: [number, number]
  flag?: string
  demonyms?: { eng?: { f?: string; m?: string } }
  timezones?: string[]
  independent?: boolean
  unMember?: boolean
  landlocked?: boolean
  fifa?: string
}

const ROOT = new URL('..', import.meta.url).pathname
const OUT = `${ROOT}public/data`
mkdirSync(OUT, { recursive: true })

const topo = JSON.parse(readFileSync('/tmp/world-50m.json', 'utf8')) as Topology<{
  countries: GeometryCollection<{ name: string }>
}>
const rc1 = JSON.parse(readFileSync('/tmp/restcountries.json', 'utf8')) as RestCountry1[]
const rc2 = JSON.parse(readFileSync('/tmp/restcountries2.json', 'utf8')) as RestCountry2[]

const ccn3ToIso3 = new Map<string, string>()
const rc1ByIso3 = new Map<string, RestCountry1>()
const rc2ByIso3 = new Map<string, RestCountry2>()
for (const c of rc1) {
  if (c.ccn3) ccn3ToIso3.set(c.ccn3, c.cca3)
  rc1ByIso3.set(c.cca3, c)
}
for (const c of rc2) rc2ByIso3.set(c.cca3, c)

// Hardcoded fixes for territories TopoJSON includes that REST Countries lists
// under different codes (Kosovo, etc.).
const ccn3Aliases: Record<string, string> = {
  '-99': 'XKX', // Kosovo lacks an official UN M.49 code
}

const fc = feature(topo, topo.objects.countries)
const features: GeoJSON.Feature[] = []
const seen = new Set<string>()

for (const f of fc.features as GeoJSON.Feature[]) {
  const ccn3 = String(f.id ?? '')
  const iso3 = ccn3ToIso3.get(ccn3) ?? ccn3Aliases[ccn3]
  if (!iso3) {
    // Unmapped territory — use synthetic id from name
    const name = (f.properties as { name?: string })?.name ?? `unknown-${ccn3}`
    f.id = `__${name.replace(/\s+/g, '_').toUpperCase()}`
  } else {
    f.id = iso3
  }
  if (seen.has(String(f.id))) continue
  seen.add(String(f.id))
  features.push(f)
}

const geojson: GeoJSON.FeatureCollection = {
  type: 'FeatureCollection',
  features,
}
writeFileSync(`${OUT}/countries.geojson`, JSON.stringify(geojson))
console.log(`wrote countries.geojson: ${features.length} features`)

// Metadata keyed by ISO3
type Meta = {
  iso3: string
  iso2: string
  name: string
  officialName: string
  capital: string | null
  region: string
  subregion: string | null
  population: number
  area: number | null
  latlng: [number, number] | null
  languages: string[]
  currencies: { code: string; name: string; symbol?: string }[]
  flagEmoji: string
  demonym: string | null
  timezones: string[]
  independent: boolean
  unMember: boolean
  landlocked: boolean
  fifa: string | null
}

const meta: Record<string, Meta> = {}
for (const f of features) {
  const id = String(f.id)
  const r1 = rc1ByIso3.get(id)
  if (!r1) continue // Skip unmapped territories
  const r2 = rc2ByIso3.get(id)
  meta[id] = {
    iso3: id,
    iso2: r1.cca2,
    name: r1.name.common,
    officialName: r1.name.official,
    capital: r1.capital?.[0] ?? null,
    region: r1.region,
    subregion: r1.subregion ?? null,
    population: r1.population,
    area: r2?.area ?? null,
    latlng: r2?.latlng ?? null,
    languages: r1.languages ? Object.values(r1.languages) : [],
    currencies: r1.currencies
      ? Object.entries(r1.currencies).map(([code, v]) => ({ code, name: v.name, symbol: v.symbol }))
      : [],
    flagEmoji: r2?.flag ?? '',
    demonym: r2?.demonyms?.eng?.m ?? null,
    timezones: r2?.timezones ?? [],
    independent: r2?.independent ?? false,
    unMember: r2?.unMember ?? false,
    landlocked: r2?.landlocked ?? false,
    fifa: r2?.fifa ?? null,
  }
}

writeFileSync(`${OUT}/countries-meta.json`, JSON.stringify(meta))
console.log(`wrote countries-meta.json: ${Object.keys(meta).length} entries`)

writeFileSync(`${OUT}/iso-index.json`, JSON.stringify(Object.keys(meta).sort()))
console.log(`wrote iso-index.json`)
