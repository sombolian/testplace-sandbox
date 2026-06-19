"""
Trump shapeshift video — optical-flow morph + layered effects.

Per-frame pipeline:
  CONSTANT (every frame):
    • Vignette          — dark radial border, always on
    • Film grain        — per-frame noise overlay
    • Breathing zoom    — slow ~3 s pulse ±1.5% scale
    • Slow colour temp  — gentle warm↔cool oscillation (~8 s period)
    • Saturation boost  — subtle extra pop
  TRANSITION-ONLY (peak-modulated):
    • Optical-flow morph (Farneback)
    • Chromatic aberration
    • Radial zoom-burst
    • Edge-energy glow
    • Scanline flicker
"""

import cv2
import numpy as np
from PIL import Image
import subprocess, os
import imageio.plugins.ffmpeg as _iff
FFMPEG = _iff.get_exe()

RNG     = np.random.default_rng(42)
SRC     = '/root/.claude/uploads/3e54e6ad-570c-5eba-baef-481eb42cce8e/3f141e08-67519.jpg'
OUT_DIR = '/home/user/testplace-sandbox'
FPS     = 30
TARGET  = (512, 512)

# ── Crop ──────────────────────────────────────────────────────────────────────
img = Image.open(SRC)
W, H = img.size
trim = 6
israel = img.crop((trim, trim,        W//2 - trim, H//2 - trim))
iran   = img.crop((trim, H//2 + trim, W//2 - trim, H    - trim))
A8 = np.array(israel.resize(TARGET, Image.LANCZOS))
B8 = np.array(iran  .resize(TARGET, Image.LANCZOS))

h, w = A8.shape[:2]
base_y, base_x = np.mgrid[0:h, 0:w].astype(np.float32)

# ── Optical flow ───────────────────────────────────────────────────────────────
gA = cv2.cvtColor(A8, cv2.COLOR_RGB2GRAY)
gB = cv2.cvtColor(B8, cv2.COLOR_RGB2GRAY)
flow = cv2.calcOpticalFlowFarneback(
    gA, gB, None,
    pyr_scale=0.5, levels=5, winsize=25,
    iterations=5, poly_n=7, poly_sigma=1.5,
    flags=cv2.OPTFLOW_FARNEBACK_GAUSSIAN
)

# ── Precomputed constant-effect assets ────────────────────────────────────────

# Vignette mask (applied by multiplication)
cx, cy = w / 2, h / 2
dist = np.sqrt(((base_x - cx) / cx)**2 + ((base_y - cy) / cy)**2)
VIGNETTE = np.clip(1 - dist * 0.65, 0.18, 1.0).astype(np.float32)   # [0.18, 1]
VIGNETTE_3 = VIGNETTE[..., np.newaxis]

# Radial direction vectors for chromatic aberration
rad_dx = (base_x - cx) / cx
rad_dy = (base_y - cy) / cy


# ── Remap helper ──────────────────────────────────────────────────────────────
def remap(img_f, mx, my):
    return cv2.remap(
        img_f.astype(np.float32),
        np.clip(mx, 0, w - 1).astype(np.float32),
        np.clip(my, 0, h - 1).astype(np.float32),
        cv2.INTER_LINEAR
    )

def ease(t):
    return t * t * (3 - 2 * t)


# ══════════════════════════════════════════════════════════════════════════════
#  CONSTANT EFFECTS  (frame_index = absolute frame number in video)
# ══════════════════════════════════════════════════════════════════════════════

def apply_vignette(f):
    return f * VIGNETTE_3


def apply_grain(f, frame_index):
    """Animated film grain — intensity varies frame to frame."""
    grain_amp = 14
    noise = RNG.standard_normal((h, w, 3)).astype(np.float32) * grain_amp
    # slightly coarser grain: blur a touch
    noise = cv2.GaussianBlur(noise, (3, 3), 0)
    return f + noise


def apply_breathing_zoom(f, frame_index):
    """Very slow ±1.5% scale oscillation (~3 s period) from face centre."""
    period = FPS * 3.0
    scale = 1.0 + 0.015 * np.sin(frame_index / period * 2 * np.pi)
    # remap: zoom around centre
    mx = cx + (base_x - cx) / scale
    my = cy + (base_y - cy) / scale
    return remap(f, mx, my)


def apply_colour_temp(f, frame_index):
    """Slow warm↔cool tint, ~8 s period. Affects R and B channels subtly."""
    period = FPS * 8.0
    shift = 10.0 * np.sin(frame_index / period * 2 * np.pi)   # ±10 value
    out = f.copy()
    out[..., 0] = out[..., 0] + shift     # R: warm up / cool down
    out[..., 2] = out[..., 2] - shift     # B: inverse
    return out


def apply_saturation(f, boost=1.25):
    """Boost colour saturation via HSV."""
    u8 = np.clip(f, 0, 255).astype(np.uint8)
    hsv = cv2.cvtColor(u8, cv2.COLOR_RGB2HSV).astype(np.float32)
    hsv[..., 1] = np.clip(hsv[..., 1] * boost, 0, 255)
    return cv2.cvtColor(hsv.astype(np.uint8), cv2.COLOR_HSV2RGB).astype(np.float32)


def apply_constant_fx(f, frame_index):
    """Stack all always-on effects."""
    f = apply_breathing_zoom(f, frame_index)
    f = apply_colour_temp(f, frame_index)
    f = apply_saturation(f)
    f = apply_vignette(f)
    f = apply_grain(f, frame_index)
    return np.clip(f, 0, 255).astype(np.uint8)


# ══════════════════════════════════════════════════════════════════════════════
#  TRANSITION EFFECTS  (t 0→1, peak = sin(t·π))
# ══════════════════════════════════════════════════════════════════════════════
TINT_A = np.array([0.5, 0.7, 1.0])
TINT_B = np.array([0.2, 0.8, 0.3])


def chromatic_aberration(f, strength):
    if strength < 0.4:
        return f
    shift = strength * 14
    r = remap(f[..., 0], base_x + rad_dx * shift,  base_y + rad_dy * shift)
    g = remap(f[..., 1], base_x,                   base_y)
    b = remap(f[..., 2], base_x - rad_dx * shift,  base_y - rad_dy * shift)
    return np.stack([r, g, b], axis=-1)


def zoom_burst(f, strength):
    if strength < 0.4:
        return f
    acc = f.copy()
    steps = 6
    for i in range(1, steps + 1):
        s = strength * 0.045 * i
        acc += remap(f, base_x + rad_dx * (w * s), base_y + rad_dy * (h * s))
    return acc / (steps + 1)


def edge_glow(f, tint, strength):
    if strength < 0.25:
        return f
    gray = cv2.cvtColor(np.clip(f, 0, 255).astype(np.uint8),
                        cv2.COLOR_RGB2GRAY).astype(np.float32)
    mag = np.sqrt(cv2.Sobel(gray, cv2.CV_32F, 1, 0, ksize=3)**2 +
                  cv2.Sobel(gray, cv2.CV_32F, 0, 1, ksize=3)**2)
    mag = cv2.GaussianBlur(mag, (0, 0), 3) / (mag.max() + 1e-6)
    return f + (mag * strength * 130)[..., np.newaxis] * tint


def scanline_flicker(f, t, strength):
    if strength < 0.25:
        return f
    mask = np.ones((h, w), dtype=np.float32)
    phase = (base_y.astype(int) // 4 + int(t * 30)) % 2
    mask[phase == 0] = 1 + strength * 0.28
    mask[phase == 1] = 1 - strength * 0.12
    return f * mask[..., np.newaxis]


def make_transition(src, dst, flow_fwd, tint_src, tint_dst, n_frames, frame_offset):
    frames = []
    for i in range(n_frames):
        t = i / n_frames
        e = ease(t)
        peak = np.sin(t * np.pi)

        mx_fwd = base_x + flow_fwd[..., 0] * e
        my_fwd = base_y + flow_fwd[..., 1] * e
        mx_bck = base_x - flow_fwd[..., 0] * (1 - e)
        my_bck = base_y - flow_fwd[..., 1] * (1 - e)

        wA = remap(src.astype(np.float32), mx_fwd, my_fwd)
        wB = remap(dst.astype(np.float32), mx_bck, my_bck)
        frame = wA * (1 - e) + wB * e

        tint = tint_src * (1 - e) + tint_dst * e
        frame = zoom_burst(frame, peak)
        frame = chromatic_aberration(frame, peak)
        frame = edge_glow(frame, tint, peak)
        frame = scanline_flicker(frame, t, peak)

        # Apply constant FX last (always on)
        frame = apply_constant_fx(frame, frame_offset + i)
        frames.append(frame)
    return frames


def make_hold(img8, secs, frame_offset):
    n = int(FPS * secs)
    return [apply_constant_fx(img8.astype(np.float32), frame_offset + i)
            for i in range(n)]


# ── Assemble ──────────────────────────────────────────────────────────────────
HOLD_SEC  = 1.2
TRANS_SEC = 2.0
LOOPS     = 3

n_hold  = int(FPS * HOLD_SEC)
n_trans = int(FPS * TRANS_SEC)
cycle   = n_hold + n_trans + n_hold + n_trans    # frames per loop

all_frames = []
for loop in range(LOOPS):
    off = loop * cycle
    all_frames += make_hold(A8, HOLD_SEC, off)
    off += n_hold
    all_frames += make_transition(A8, B8,  flow,  TINT_A, TINT_B, n_trans, off)
    off += n_trans
    all_frames += make_hold(B8, HOLD_SEC, off)
    off += n_hold
    all_frames += make_transition(B8, A8, -flow,  TINT_B, TINT_A, n_trans, off)
# final hold
all_frames += make_hold(A8, HOLD_SEC, LOOPS * cycle)

print(f'Total frames: {len(all_frames)}')

# ── Encode ────────────────────────────────────────────────────────────────────
raw_path = os.path.join(OUT_DIR, 'trump_shapeshift_raw.mp4')
out_path = os.path.join(OUT_DIR, 'trump_shapeshift.mp4')

fourcc = cv2.VideoWriter_fourcc(*'mp4v')
vw = cv2.VideoWriter(raw_path, fourcc, FPS, TARGET)
for fr in all_frames:
    vw.write(cv2.cvtColor(fr, cv2.COLOR_RGB2BGR))
vw.release()

r = subprocess.run([
    FFMPEG, '-y', '-i', raw_path,
    '-vcodec', 'libx264', '-crf', '16',
    '-pix_fmt', 'yuv420p', '-movflags', '+faststart',
    out_path
], capture_output=True, text=True)

if r.returncode == 0:
    print(f'Done: {out_path}  ({os.path.getsize(out_path)/1e6:.1f} MB)')
else:
    print('ffmpeg error:', r.stderr[-800:])
