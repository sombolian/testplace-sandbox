import { CameraControls } from '@react-three/drei'
import { useFrame } from '@react-three/fiber'
import { useEffect, useRef } from 'react'
import { useGlobeStore } from '@/state/useGlobeStore'
import { latLonToVec3 } from '@/lib/latlon'

export function CameraRig() {
  const ref = useRef<CameraControls>(null!)
  const flyToTarget = useGlobeStore((s) => s.flyToTarget)
  const autoRotate = useGlobeStore((s) => s.autoRotate)
  const userInteracting = useRef(false)

  useEffect(() => {
    if (!flyToTarget || !ref.current) return
    const dist = flyToTarget.zoom ?? 2.4
    const v = latLonToVec3(flyToTarget.lat, flyToTarget.lon, dist)
    ref.current.setLookAt(v.x, v.y, v.z, 0, 0, 0, true)
  }, [flyToTarget])

  useEffect(() => {
    const c = ref.current
    if (!c) return
    const onStart = () => (userInteracting.current = true)
    const onEnd = () => (userInteracting.current = false)
    c.addEventListener('controlstart', onStart)
    c.addEventListener('controlend', onEnd)
    return () => {
      c.removeEventListener('controlstart', onStart)
      c.removeEventListener('controlend', onEnd)
    }
  }, [])

  useFrame((_, dt) => {
    if (autoRotate && ref.current && !userInteracting.current) {
      ref.current.azimuthAngle += dt * 0.04
    }
  })

  return (
    <CameraControls
      ref={ref}
      makeDefault
      minDistance={1.2}
      maxDistance={6}
      smoothTime={0.4}
      draggingSmoothTime={0.12}
      polarRotateSpeed={0.6}
      azimuthRotateSpeed={0.6}
      dollySpeed={0.6}
    />
  )
}
