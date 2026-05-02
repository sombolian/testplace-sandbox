import { Vector3 } from 'three'

/**
 * Convert (lat°, lon°) to a unit-vector point on the unit sphere.
 * x = cos(lat) cos(lon)
 * y = sin(lat)
 * z = -cos(lat) sin(lon)
 *
 * Earth texture (equirectangular) wraps so that lon=0 sits at the
 * x axis, lon=90 at z=-1, etc.  This matches how three's
 * SphereGeometry samples its UVs.
 */
export function latLonToVec3(lat: number, lon: number, radius = 1, out = new Vector3()): Vector3 {
  const phi = (lat * Math.PI) / 180
  const theta = (lon * Math.PI) / 180
  const cosPhi = Math.cos(phi)
  out.set(radius * cosPhi * Math.cos(theta), radius * Math.sin(phi), -radius * cosPhi * Math.sin(theta))
  return out
}

export function vec3ToLatLon(v: Vector3): [number, number] {
  const r = v.length()
  const lat = Math.asin(v.y / r) * (180 / Math.PI)
  const lon = Math.atan2(-v.z, v.x) * (180 / Math.PI)
  return [lat, lon]
}
