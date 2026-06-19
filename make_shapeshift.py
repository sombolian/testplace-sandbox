import cv2
import numpy as np
from PIL import Image
import subprocess
import os
import imageio.plugins.ffmpeg as _iff
FFMPEG = _iff.get_exe()

SRC = '/root/.claude/uploads/3e54e6ad-570c-5eba-baef-481eb42cce8e/3f141e08-67519.jpg'
OUT_DIR = '/home/user/testplace-sandbox'

# ── crop ─────────────────────────────────────────────────────────────────────
img = Image.open(SRC)
W, H = img.size          # 1024 × 1024

# Left half has two photos stacked; right half is text.
# Add a small pixel trim to remove any hard border lines.
trim = 6
israel = img.crop((trim, trim,        W//2 - trim, H//2 - trim))
iran   = img.crop((trim, H//2 + trim, W//2 - trim, H    - trim))

israel.save(os.path.join(OUT_DIR, 'trump_israel.png'))
iran  .save(os.path.join(OUT_DIR, 'trump_iran.png'))
print(f'Crops saved: israel={israel.size}  iran={iran.size}')

TARGET = (512, 512)
A = np.array(israel.resize(TARGET, Image.LANCZOS)).astype(np.float32)
B = np.array(iran  .resize(TARGET, Image.LANCZOS)).astype(np.float32)

# ── morphing helpers ──────────────────────────────────────────────────────────

def ripple_warp(img_f, t, base_amp=28, freq=14, speed=5):
    """Sinusoidal liquid displacement that pulses with t (0→1 peaks at 0.5)."""
    h, w = img_f.shape[:2]
    amp = base_amp * np.sin(t * np.pi)          # ramps up then down

    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32)

    dx = amp * np.sin(ys / h * freq * np.pi + t * speed * np.pi)
    dy = amp * np.sin(xs / w * freq * np.pi + t * speed * np.pi + 1.3)

    # add a rotational swirl
    cx, cy = w / 2, h / 2
    swirl_r = np.sqrt((xs - cx)**2 + (ys - cy)**2)
    swirl_angle = amp * 0.03 * np.exp(-swirl_r / (w * 0.4))
    cos_s, sin_s = np.cos(swirl_angle), np.sin(swirl_angle)
    rx = cx + (xs - cx) * cos_s - (ys - cy) * sin_s
    ry = cy + (xs - cx) * sin_s + (ys - cy) * cos_s

    map_x = np.clip(rx + dx, 0, w - 1).astype(np.float32)
    map_y = np.clip(ry + dy, 0, h - 1).astype(np.float32)

    return cv2.remap(img_f, map_x, map_y, cv2.INTER_LINEAR)


def blend_frame(A, B, t):
    """Morph A→B: warp both + blend + colour ghost."""
    wA = ripple_warp(A, t)
    wB = ripple_warp(B, t)

    # Smooth easing
    ease = t * t * (3 - 2 * t)
    frame = wA * (1 - ease) + wB * ease

    # Chromatic ghost: briefly push hue toward a vivid cyan/gold tint at peak
    peak = np.sin(t * np.pi)
    tint = np.array([peak * 15, peak * 8, peak * 20], dtype=np.float32)
    frame = frame + tint[np.newaxis, np.newaxis, :]

    return np.clip(frame, 0, 255).astype(np.uint8)


# ── video parameters ──────────────────────────────────────────────────────────
FPS        = 30
HOLD_SEC   = 1.2     # seconds to hold each face
TRANS_SEC  = 1.8     # seconds for each morph
LOOPS      = 3       # how many full round-trips to encode

frames = []

def hold(img_np, seconds):
    n = int(FPS * seconds)
    bgr = cv2.cvtColor(img_np, cv2.COLOR_RGB2BGR)
    frames.extend([bgr] * n)

def transition(src, dst, seconds):
    n = int(FPS * seconds)
    for i in range(n):
        t = i / n
        fr = blend_frame(src.astype(np.float32), dst.astype(np.float32), t)
        frames.append(cv2.cvtColor(fr, cv2.COLOR_RGB2BGR))

A8 = A.astype(np.uint8)
B8 = B.astype(np.uint8)

for _ in range(LOOPS):
    hold(A8, HOLD_SEC)
    transition(A, B, TRANS_SEC)
    hold(B8, HOLD_SEC)
    transition(B, A, TRANS_SEC)

# close the loop cleanly
hold(A8, HOLD_SEC)

# ── write video ───────────────────────────────────────────────────────────────
raw_path = os.path.join(OUT_DIR, 'trump_shapeshift_raw.mp4')
out_path = os.path.join(OUT_DIR, 'trump_shapeshift.mp4')

fourcc = cv2.VideoWriter_fourcc(*'mp4v')
vw = cv2.VideoWriter(raw_path, fourcc, FPS, TARGET)
for fr in frames:
    vw.write(fr)
vw.release()
print(f'Raw frames: {len(frames)}  →  {raw_path}')

# Re-encode with ffmpeg for a widely-compatible H.264 file
result = subprocess.run([
    FFMPEG, '-y', '-i', raw_path,
    '-vcodec', 'libx264', '-crf', '18',
    '-pix_fmt', 'yuv420p',
    '-movflags', '+faststart',
    out_path
], capture_output=True, text=True)

if result.returncode == 0:
    size_mb = os.path.getsize(out_path) / 1e6
    print(f'Done!  {out_path}  ({size_mb:.1f} MB)')
else:
    print('ffmpeg stderr:', result.stderr[-600:])
    print(f'Raw file still available: {raw_path}')
