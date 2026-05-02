import earcut from 'earcut'
import { BufferAttribute, BufferGeometry, Vector3 } from 'three'
import { latLonToVec3 } from './latlon'

type Ring = [number, number][]

/**
 * Splits a polygon ring at the antimeridian by shifting longitudes so
 * the ring is contiguous.  Detects "jumps" larger than 180° between
 * consecutive points and offsets the trailing portion by ±360°.
 */
function unwrapRing(ring: Ring): Ring {
  if (ring.length < 2) return ring
  const out: Ring = [ring[0]]
  let offset = 0
  for (let i = 1; i < ring.length; i++) {
    const prev = ring[i - 1][0] + offset
    const curr = ring[i][0]
    let delta = curr - (prev - offset)
    if (delta > 180) offset -= 360
    else if (delta < -180) offset += 360
    out.push([curr + offset, ring[i][1]])
  }
  return out
}

/**
 * Subdivide a ring so that no edge spans more than `maxStep` degrees
 * of arc.  This keeps polygon edges curving along the sphere instead
 * of cutting through it as chords.
 */
function densifyRing(ring: Ring, maxStep = 3): Ring {
  const out: Ring = []
  for (let i = 0; i < ring.length; i++) {
    const [lon1, lat1] = ring[i]
    const [lon2, lat2] = ring[(i + 1) % ring.length]
    out.push([lon1, lat1])
    const span = Math.max(Math.abs(lon2 - lon1), Math.abs(lat2 - lat1))
    if (span > maxStep) {
      const steps = Math.ceil(span / maxStep)
      for (let s = 1; s < steps; s++) {
        const t = s / steps
        out.push([lon1 + (lon2 - lon1) * t, lat1 + (lat2 - lat1) * t])
      }
    }
  }
  return out
}

/**
 * Build a filled BufferGeometry for a single Polygon (with optional holes),
 * triangulated in (lon,lat) space and projected to a sphere of given radius.
 */
function polygonToFillGeometry(rings: Ring[], radius: number): BufferGeometry | null {
  if (!rings.length) return null
  const denseRings = rings.map((r) => densifyRing(unwrapRing(r)))
  const flat: number[] = []
  const holeIndices: number[] = []
  for (let i = 0; i < denseRings.length; i++) {
    if (i > 0) holeIndices.push(flat.length / 2)
    for (const [lon, lat] of denseRings[i]) {
      flat.push(lon, lat)
    }
  }
  const indices = earcut(flat, holeIndices, 2)
  if (!indices.length) return null

  const vertices = new Float32Array((flat.length / 2) * 3)
  const tmp = new Vector3()
  for (let i = 0, j = 0; i < flat.length; i += 2, j += 3) {
    const lon = flat[i]
    const lat = flat[i + 1]
    latLonToVec3(lat, lon, radius, tmp)
    vertices[j] = tmp.x
    vertices[j + 1] = tmp.y
    vertices[j + 2] = tmp.z
  }

  const geo = new BufferGeometry()
  geo.setAttribute('position', new BufferAttribute(vertices, 3))
  geo.setIndex(indices)
  geo.computeVertexNormals()
  return geo
}

/**
 * Build a line BufferGeometry for the outline of one or more rings.
 * Returns LineSegments-compatible position data (paired vertices).
 */
function ringsToBorderGeometry(rings: Ring[], radius: number): BufferGeometry | null {
  if (!rings.length) return null
  const segments: number[] = []
  const tmp = new Vector3()
  for (const ring of rings) {
    const dense = densifyRing(unwrapRing(ring))
    for (let i = 0; i < dense.length; i++) {
      const a = dense[i]
      const b = dense[(i + 1) % dense.length]
      latLonToVec3(a[1], a[0], radius, tmp)
      segments.push(tmp.x, tmp.y, tmp.z)
      latLonToVec3(b[1], b[0], radius, tmp)
      segments.push(tmp.x, tmp.y, tmp.z)
    }
  }
  if (!segments.length) return null
  const geo = new BufferGeometry()
  geo.setAttribute('position', new BufferAttribute(new Float32Array(segments), 3))
  return geo
}

export interface CountryGeoms {
  fill: BufferGeometry | null
  border: BufferGeometry | null
  centroid: [number, number] // lat, lon — picks the largest polygon's centroid
}

/**
 * Convert a GeoJSON Feature (Polygon or MultiPolygon) into combined fill +
 * border geometries on the sphere.  All polygons of a multi-polygon are
 * merged into a single BufferGeometry per layer.
 */
export function featureToGeoms(
  feature: GeoJSON.Feature,
  fillRadius = 1.001,
  borderRadius = 1.0015,
): CountryGeoms {
  const polygons: Ring[][] = []
  if (!feature.geometry) {
    return { fill: null, border: null, centroid: [0, 0] }
  }
  if (feature.geometry.type === 'Polygon') {
    polygons.push(feature.geometry.coordinates as Ring[])
  } else if (feature.geometry.type === 'MultiPolygon') {
    for (const poly of feature.geometry.coordinates) polygons.push(poly as Ring[])
  } else {
    return { fill: null, border: null, centroid: [0, 0] }
  }

  const fills: BufferGeometry[] = []
  const borders: BufferGeometry[] = []
  let largestArea = 0
  let centroid: [number, number] = [0, 0]

  for (const poly of polygons) {
    const fill = polygonToFillGeometry(poly, fillRadius)
    if (fill) fills.push(fill)
    const border = ringsToBorderGeometry(poly, borderRadius)
    if (border) borders.push(border)

    // crude bbox-area centroid for fly-to selection
    const ring = poly[0]
    if (ring && ring.length) {
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
      if (area > largestArea) {
        largestArea = area
        centroid = [sumLat / ring.length, sumLon / ring.length]
      }
    }
  }

  return {
    fill: fills.length ? mergeGeoms(fills) : null,
    border: borders.length ? mergeGeoms(borders) : null,
    centroid,
  }
}

function mergeGeoms(geoms: BufferGeometry[]): BufferGeometry {
  if (geoms.length === 1) return geoms[0]
  const positions: number[] = []
  const indices: number[] = []
  let offset = 0
  for (const g of geoms) {
    const pos = g.getAttribute('position')
    for (let i = 0; i < pos.count * 3; i++) positions.push(pos.array[i])
    const idx = g.getIndex()
    if (idx) {
      for (let i = 0; i < idx.count; i++) indices.push(idx.array[i] + offset)
    }
    offset += pos.count
  }
  const out = new BufferGeometry()
  out.setAttribute('position', new BufferAttribute(new Float32Array(positions), 3))
  if (indices.length) out.setIndex(indices)
  out.computeVertexNormals()
  return out
}
