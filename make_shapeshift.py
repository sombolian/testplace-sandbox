"""
Trump shapeshift video — optical-flow morph + glitch burst effect.

Pipeline per transition frame (t 0→1):
  1. Farneback dense optical flow A→B
  2. Forward-warp A by t*flow, backward-warp B by (1-t)*flow, blend
  3. Chromatic aberration: split RGB channels, shift them radially
  4. Radial zoom-burst at mid-transition peak
  5. Edge-energy glow (Sobel) tinted to the destination palette
  6. Scanline flicker at peak for digital-glitch feel
"""

import cv2
import numpy as np
from PIL import Image
import subprocess, os
import imageio.plugins.ffmpeg as _iff
FFMPEG = _iff.get_exe()

SRC     = '/root/.claude/uploads/3e54e6ad-570c-5eba-baef-481eb42cce8e/3f141e08-67519.jpg'
OUT_DIR = '/home/user/testplace-sandbox'

# ── 1. Crop ───────────────────────────────────────────────────────────────────
img = Image.open(SRC)
W, H = img.size
trim = 6
israel = img.crop((trim, trim,        W//2 - trim, H//2 - trim))
iran   = img.crop((trim, H//2 + trim, W//2 - trim, H    - trim))

TARGET = (512, 512)
A8 = np.array(israel.resize(TARGET, Image.LANCZOS))
B8 = np.array(iran  .resize(TARGET, Image.LANCZOS))

# ── 2. Optical flow A→B ──────────────────────────────────────────────────────
gA = cv2.cvtColor(A8, cv2.COLOR_RGB2GRAY)
gB = cv2.cvtColor(B8, cv2.COLOR_RGB2GRAY)
flow = cv2.calcOpticalFlowFarneback(
    gA, gB, None,
    pyr_scale=0.5, levels=5, winsize=25,
    iterations=5, poly_n=7, poly_sigma=1.5,
    flags=cv2.OPTFLOW_FARNEBACK_GAUSSIAN
)
flow_x, flow_y = flow[..., 0], flow[..., 1]

h, w = A8.shape[:2]
base_y, base_x = np.mgrid[0:h, 0:w].astype(np.float32)


def remap(img, mx, my):
    mx = np.clip(mx, 0, w - 1).astype(np.float32)
    my = np.clip(my, 0, h - 1).astype(np.float32)
    return cv2.remap(img.astype(np.float32), mx, my, cv2.INTER_LINEAR)


def ease(t):
    return t * t * (3 - 2 * t)          # smoothstep


# ── 3. Effect helpers ─────────────────────────────────────────────────────────

def chromatic_aberration(frame_f, strength):
    """Radially shift R outward, B inward."""
    if strength < 0.5:
        return frame_f
    cx, cy = w / 2, h / 2
    dx = (base_x - cx) / cx
    dy = (base_y - cy) / cy

    shift = strength * 12

    def shift_channel(ch, s):
        mx = base_x + dx * s
        my = base_y + dy * s
        return remap(frame_f[..., ch:ch+1].squeeze(), mx, my)

    r = shift_channel(0,  shift)
    g = shift_channel(1,  0)
    b = shift_channel(2, -shift)
    return np.stack([r, g, b], axis=-1)


def zoom_burst(frame_f, strength):
    """Radial zoom-out blur accumulated over a few steps."""
    if strength < 0.5:
        return frame_f
    cx, cy = w / 2, h / 2
    dx = (base_x - cx) / cx
    dy = (base_y - cy) / cy

    acc = frame_f.copy()
    steps = 6
    for i in range(1, steps + 1):
        s = strength * 0.04 * i
        mx = base_x + dx * (w * s)
        my = base_y + dy * (h * s)
        acc = acc + remap(frame_f, mx, my)
    return acc / (steps + 1)


def edge_glow(frame_f, tint_rgb, strength):
    """Sobel edges tinted toward destination colour, added as bloom."""
    if strength < 0.3:
        return frame_f
    gray = cv2.cvtColor(np.clip(frame_f, 0, 255).astype(np.uint8),
                        cv2.COLOR_RGB2GRAY).astype(np.float32)
    sx = cv2.Sobel(gray, cv2.CV_32F, 1, 0, ksize=3)
    sy = cv2.Sobel(gray, cv2.CV_32F, 0, 1, ksize=3)
    mag = np.sqrt(sx**2 + sy**2)
    mag = cv2.GaussianBlur(mag, (0, 0), 3)
    mag = mag / (mag.max() + 1e-6)
    glow_mask = (mag * strength * 120)[..., np.newaxis]
    tint = np.array(tint_rgb, dtype=np.float32)
    return frame_f + glow_mask * tint


def scanline_flicker(frame_f, t, strength):
    """Horizontal scanlines that sweep downward during peak."""
    if strength < 0.3:
        return frame_f
    # every 4th row gets slightly brightened/darkened alternately
    mask = np.ones((h, w), dtype=np.float32)
    row_phase = (base_y.astype(int) // 4 + int(t * 30)) % 2
    mask[row_phase == 0] = 1 + strength * 0.25
    mask[row_phase == 1] = 1 - strength * 0.10
    return frame_f * mask[..., np.newaxis]


# ── 4. Build transition A→B ───────────────────────────────────────────────────

# Colour tints for the glow (Israel: blue-gold, Iran: green-gold)
TINT_A = [0.5, 0.7, 1.0]      # blue-ish (Israeli flag)
TINT_B = [0.2, 0.8, 0.3]      # green (Iranian flag)


def make_transition(src, dst, flow_fwd, tint_src, tint_dst, n_frames):
    frames = []
    for i in range(n_frames):
        t = i / n_frames
        e = ease(t)
        peak = np.sin(t * np.pi)          # 0 at both ends, 1 at midpoint

        # --- optical-flow warp ---
        mx_fwd = base_x + flow_fwd[..., 0] * e
        my_fwd = base_y + flow_fwd[..., 1] * e
        mx_bck = base_x - flow_fwd[..., 0] * (1 - e)
        my_bck = base_y - flow_fwd[..., 1] * (1 - e)

        wA = remap(src.astype(np.float32), mx_fwd, my_fwd)
        wB = remap(dst.astype(np.float32), mx_bck, my_bck)

        frame = wA * (1 - e) + wB * e

        # --- effects (peak-modulated) ---
        tint = [tint_src[c] * (1 - e) + tint_dst[c] * e for c in range(3)]

        frame = zoom_burst(frame, peak)
        frame = chromatic_aberration(frame, peak)
        frame = edge_glow(frame, tint, peak)
        frame = scanline_flicker(frame, t, peak)

        frames.append(np.clip(frame, 0, 255).astype(np.uint8))
    return frames


# ── 5. Assemble full video ────────────────────────────────────────────────────
FPS       = 30
HOLD_SEC  = 1.2
TRANS_SEC = 2.0
LOOPS     = 3

def hold_frames(img8, secs):
    n = int(FPS * secs)
    return [img8] * n

A_bgr = cv2.cvtColor(A8, cv2.COLOR_RGB2BGR)
B_bgr = cv2.cvtColor(B8, cv2.COLOR_RGB2BGR)

n_trans = int(FPS * TRANS_SEC)
fwd_frames = make_transition(A8, B8, flow,       TINT_A, TINT_B, n_trans)
bwd_frames = make_transition(B8, A8, -flow,      TINT_B, TINT_A, n_trans)

all_frames_rgb = []
for _ in range(LOOPS):
    all_frames_rgb += hold_frames(A8, HOLD_SEC)
    all_frames_rgb += fwd_frames
    all_frames_rgb += hold_frames(B8, HOLD_SEC)
    all_frames_rgb += bwd_frames
all_frames_rgb += hold_frames(A8, HOLD_SEC)

# ── 6. Encode ─────────────────────────────────────────────────────────────────
raw_path = os.path.join(OUT_DIR, 'trump_shapeshift_raw.mp4')
out_path = os.path.join(OUT_DIR, 'trump_shapeshift.mp4')

fourcc = cv2.VideoWriter_fourcc(*'mp4v')
vw = cv2.VideoWriter(raw_path, fourcc, FPS, TARGET)
for fr in all_frames_rgb:
    vw.write(cv2.cvtColor(fr, cv2.COLOR_RGB2BGR))
vw.release()

print(f'{len(all_frames_rgb)} frames written → {raw_path}')

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
