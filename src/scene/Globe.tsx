import { useTexture } from '@react-three/drei'
import { useFrame } from '@react-three/fiber'
import { useMemo, useRef } from 'react'
import {
  AdditiveBlending,
  BackSide,
  Color,
  Mesh,
  ShaderMaterial,
  SRGBColorSpace,
  Vector3,
} from 'three'
import { useGlobeStore } from '@/state/useGlobeStore'

const earthVertex = /* glsl */ `
varying vec2 vUv;
varying vec3 vNormalW;
varying vec3 vWorldPos;

void main() {
  vUv = uv;
  vNormalW = normalize(mat3(modelMatrix) * normal);
  vec4 wp = modelMatrix * vec4(position, 1.0);
  vWorldPos = wp.xyz;
  gl_Position = projectionMatrix * viewMatrix * wp;
}
`

const earthFragment = /* glsl */ `
uniform sampler2D uDay;
uniform sampler2D uNight;
uniform sampler2D uSpecular;
uniform sampler2D uNormalMap;
uniform vec3 uSunDir;
uniform vec3 uCameraPos;
uniform int uMode;        // 0 political, 1 physical, 2 night
uniform float uPoliticalMix;

varying vec2 vUv;
varying vec3 vNormalW;
varying vec3 vWorldPos;

void main() {
  vec3 N = normalize(vNormalW);
  vec3 L = normalize(uSunDir);
  vec3 V = normalize(uCameraPos - vWorldPos);

  vec3 dayColor = texture2D(uDay, vUv).rgb;
  vec3 nightColor = texture2D(uNight, vUv).rgb;
  float specMask = texture2D(uSpecular, vUv).r;

  // Day/night blend with smooth terminator
  float lambert = dot(N, L);

  vec3 surfaceColor;
  if (uMode == 2) {
    // Night mode: emphasise night lights
    float k = smoothstep(-0.05, 0.15, lambert);
    surfaceColor = mix(nightColor * 1.4, dayColor * 0.6, k);
  } else {
    float k = smoothstep(-0.15, 0.25, lambert);
    surfaceColor = mix(nightColor * 1.1, dayColor, k);
  }

  // Sun-facing diffuse + specular highlight on oceans
  float diff = max(dot(N, L), 0.0);
  vec3 H = normalize(L + V);
  float specP = pow(max(dot(N, H), 0.0), 60.0) * specMask * 0.85;

  // Fresnel rim — subtle atmosphere kiss on lit side
  float fres = pow(1.0 - max(dot(N, V), 0.0), 3.0);
  vec3 rim = vec3(0.35, 0.55, 1.0) * fres * 0.4 * smoothstep(-0.2, 0.5, lambert);

  vec3 color = surfaceColor * (0.45 + 0.65 * diff) + specP + rim;

  if (uMode == 0) {
    // Political: desaturate slightly + mild colour-pop tint to make
    // overlay tints stand out.
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    vec3 cool = mix(color, vec3(gray), 0.12);
    color = mix(color, cool, uPoliticalMix);
  }

  gl_FragColor = vec4(color, 1.0);
}
`

function modeIndex(mode: 'political' | 'physical' | 'night'): number {
  return mode === 'political' ? 0 : mode === 'physical' ? 1 : 2
}

interface GlobeProps {
  radius?: number
  sunDir: Vector3
}

export function Globe({ radius = 1, sunDir }: GlobeProps) {
  const meshRef = useRef<Mesh>(null!)
  const matRef = useRef<ShaderMaterial>(null!)
  const layerMode = useGlobeStore((s) => s.layerMode)

  const base = import.meta.env.BASE_URL
  const [day, night, specular, normal] = useTexture([
    `${base}textures/earth-day.jpg`,
    `${base}textures/earth-night.png`,
    `${base}textures/earth-specular.jpg`,
    `${base}textures/earth-normal.jpg`,
  ]) as unknown as [
    THREE_TEX,
    THREE_TEX,
    THREE_TEX,
    THREE_TEX,
  ]
  ;[day, night].forEach((t) => (t.colorSpace = SRGBColorSpace))

  const uniforms = useMemo(
    () => ({
      uDay: { value: day },
      uNight: { value: night },
      uSpecular: { value: specular },
      uNormalMap: { value: normal },
      uSunDir: { value: sunDir.clone() },
      uCameraPos: { value: new Vector3() },
      uMode: { value: modeIndex(layerMode) },
      uPoliticalMix: { value: 1.0 },
    }),
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [day, night, specular, normal],
  )

  useFrame((state) => {
    uniforms.uCameraPos.value.copy(state.camera.position)
    uniforms.uSunDir.value.copy(sunDir)
    uniforms.uMode.value = modeIndex(layerMode)
  })

  return (
    <mesh ref={meshRef} renderOrder={0}>
      <sphereGeometry args={[radius, 96, 96]} />
      <shaderMaterial
        ref={matRef}
        uniforms={uniforms}
        vertexShader={earthVertex}
        fragmentShader={earthFragment}
      />
    </mesh>
  )
}

// drei's useTexture types are awkward; alias for clarity.
type THREE_TEX = import('three').Texture

interface CloudsProps {
  radius?: number
}

export function Clouds({ radius = 1.005 }: CloudsProps) {
  const meshRef = useRef<Mesh>(null!)
  const cloudTex = useTexture(`${import.meta.env.BASE_URL}textures/earth-clouds.png`) as unknown as THREE_TEX

  useFrame((_, dt) => {
    if (meshRef.current) {
      meshRef.current.rotation.y += dt * 0.0072
    }
  })

  return (
    <mesh ref={meshRef} renderOrder={1}>
      <sphereGeometry args={[radius, 96, 96]} />
      <meshLambertMaterial
        map={cloudTex}
        transparent
        opacity={0.4}
        depthWrite={false}
      />
    </mesh>
  )
}

const atmosphereVertex = /* glsl */ `
varying vec3 vNormalW;
varying vec3 vWorldPos;
void main() {
  vNormalW = normalize(mat3(modelMatrix) * normal);
  vec4 wp = modelMatrix * vec4(position, 1.0);
  vWorldPos = wp.xyz;
  gl_Position = projectionMatrix * viewMatrix * wp;
}
`

const atmosphereFragment = /* glsl */ `
uniform vec3 uColor;
uniform vec3 uCameraPos;
uniform float uPower;
uniform float uIntensity;
varying vec3 vNormalW;
varying vec3 vWorldPos;
void main() {
  vec3 V = normalize(uCameraPos - vWorldPos);
  vec3 N = normalize(vNormalW);
  // Inverted because we render BackSide
  float fres = pow(1.0 - max(dot(-N, V), 0.0), uPower);
  gl_FragColor = vec4(uColor, fres * uIntensity);
}
`

interface AtmosphereProps {
  radius?: number
}

export function Atmosphere({ radius = 1.06 }: AtmosphereProps) {
  const matUniforms = useMemo(
    () => ({
      uColor: { value: new Color('#5fa1ff') },
      uCameraPos: { value: new Vector3() },
      uPower: { value: 2.4 },
      uIntensity: { value: 1.2 },
    }),
    [],
  )

  useFrame((state) => {
    matUniforms.uCameraPos.value.copy(state.camera.position)
  })

  return (
    <mesh renderOrder={3}>
      <sphereGeometry args={[radius, 64, 64]} />
      <shaderMaterial
        uniforms={matUniforms}
        vertexShader={atmosphereVertex}
        fragmentShader={atmosphereFragment}
        transparent
        side={BackSide}
        depthWrite={false}
        blending={AdditiveBlending}
      />
    </mesh>
  )
}
