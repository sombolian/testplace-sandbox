import { useTexture } from '@react-three/drei'
import { BackSide, SRGBColorSpace, type Texture } from 'three'

export function Stars({ radius = 60 }: { radius?: number }) {
  const tex = useTexture(`${import.meta.env.BASE_URL}textures/stars-milkyway.jpg`) as unknown as Texture
  tex.colorSpace = SRGBColorSpace
  return (
    <mesh>
      <sphereGeometry args={[radius, 32, 32]} />
      <meshBasicMaterial map={tex} side={BackSide} depthWrite={false} />
    </mesh>
  )
}
