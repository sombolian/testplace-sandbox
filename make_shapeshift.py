"""
Trump shapeshift — complete overhaul.

Constant effects (every frame):
  heat haze, animated particles, breathing zoom, colour temperature drift,
  cinematic teal-orange grade, saturation boost, lens dirt, vignette, film grain

Transition-only effects (peak-modulated):
  optical-flow face morph, god rays, chromatic aberration, horizontal glitch
  blocks, electric arc lightning, camera shake, brightness flash
"""

import cv2, subprocess, os
import numpy as np
from PIL import Image
import imageio.plugins.ffmpeg as _iff

FFMPEG  = _iff.get_exe()
SRC     = '/root/.claude/uploads/3e54e6ad-570c-5eba-baef-481eb42cce8e/3f141e08-67519.jpg'
OUT_DIR = '/home/user/testplace-sandbox'
FPS     = 30
TARGET  = (720, 720)

# ─── crop ─────────────────────────────────────────────────────────────────────
src_img = Image.open(SRC)
W, H = src_img.size
T = 6
A8 = np.array(src_img.crop((T, T,   W//2-T, H//2-T)).resize(TARGET, Image.LANCZOS))
B8 = np.array(src_img.crop((T, H//2+T, W//2-T, H-T)).resize(TARGET, Image.LANCZOS))
h, w = A8.shape[:2]
Y, X = np.mgrid[0:h, 0:w].astype(np.float32)

# ─── optical flow ─────────────────────────────────────────────────────────────
FLOW = cv2.calcOpticalFlowFarneback(
    cv2.cvtColor(A8, cv2.COLOR_RGB2GRAY),
    cv2.cvtColor(B8, cv2.COLOR_RGB2GRAY),
    None, pyr_scale=0.5, levels=6, winsize=35,
    iterations=7, poly_n=7, poly_sigma=1.5,
    flags=cv2.OPTFLOW_FARNEBACK_GAUSSIAN
)

# ─── precomputed geometry ─────────────────────────────────────────────────────
cx, cy  = w/2, h/2
RDX     = (X - cx) / cx          # radial unit vectors
RDY     = (Y - cy) / cy
RDIST   = np.sqrt(RDX**2 + RDY**2)

# vignette: dark + blue-cool border
_vig    = np.clip(1 - RDIST * 0.72, 0.10, 1.0).astype(np.float32)
VIGNETTE= np.stack([_vig*0.86, _vig*0.91, _vig*1.0], axis=-1)

# procedural lens-dirt (static smudge overlay)
_rng0  = np.random.default_rng(777)
_dirt  = np.zeros((h, w), dtype=np.float32)
for _ in range(30):
    bx, by = _rng0.integers(0,w), _rng0.integers(0,h)
    br, bs = _rng0.integers(40,200), _rng0.uniform(0.05, 0.18)
    _dirt += bs * np.exp(-((X-bx)**2+(Y-by)**2) / (2*br**2))
LENS_DIRT = np.clip(_dirt, 0, 0.32)[..., np.newaxis]

# ─── particle system ──────────────────────────────────────────────────────────
NP    = 120
_prng = np.random.default_rng(999)
Ppos  = _prng.random((NP, 2)).astype(np.float32)
Pvel  = ((_prng.random((NP, 2)) - 0.5) * 0.003).astype(np.float32)
Plif  = _prng.random(NP).astype(np.float32)
Pdec  = _prng.uniform(0.003, 0.009, NP).astype(np.float32)
Psiz  = _prng.uniform(1.5, 5.0, NP).astype(np.float32)
Pcol  = np.stack([_prng.uniform(0.88,1.0,NP),
                  _prng.uniform(0.78,0.95,NP),
                  _prng.uniform(0.45,0.72,NP)], axis=1).astype(np.float32)


# ═══════════════════════════════════════════════════════════════════════════════
#  HELPERS
# ═══════════════════════════════════════════════════════════════════════════════

def remap(img_f, mx, my):
    return cv2.remap(
        img_f.astype(np.float32),
        np.clip(mx, 0, w-1).astype(np.float32),
        np.clip(my, 0, h-1).astype(np.float32),
        cv2.INTER_LINEAR
    )

def ease(t):
    return t*t*(3 - 2*t)


# ═══════════════════════════════════════════════════════════════════════════════
#  CONSTANT EFFECTS  (applied to every single frame)
# ═══════════════════════════════════════════════════════════════════════════════

def fx_heat_haze(f, fidx):
    t   = fidx / FPS * 0.35
    amp = 3.5
    dx  = amp * np.sin(Y/h * 9*np.pi + t*2*np.pi)
    dy  = amp * np.cos(X/w * 7*np.pi + t*1.6*np.pi)
    return remap(f, X+dx, Y+dy)


def fx_breathing_zoom(f, fidx):
    s  = 1.0 + 0.019 * np.sin(fidx / (FPS*3.5) * 2*np.pi)
    return remap(f, cx+(X-cx)/s, cy+(Y-cy)/s)


def fx_colour_temp(f, fidx):
    shift = 9 * np.sin(fidx / (FPS*9) * 2*np.pi)
    out   = f.copy()
    out[...,0] = np.clip(out[...,0]+shift, 0, 255)
    out[...,2] = np.clip(out[...,2]-shift, 0, 255)
    return out


def fx_cinematic_grade(f):
    f   = f * 0.88 + 12
    lum = f.mean(axis=2, keepdims=True) / 255.0
    # teal shadows
    shd = np.clip(1 - lum*2.5, 0, 1)
    f[...,0] -= shd[...,0] * 12
    f[...,2] += shd[...,0] * 18
    # orange highlights
    hi  = np.clip(lum*2 - 1, 0, 1)
    f[...,0] += hi[...,0] * 22
    f[...,1] += hi[...,0] *  7
    f[...,2] -= hi[...,0] * 15
    return np.clip(f, 0, 255)


def fx_saturation(f, factor=1.35):
    u8  = np.clip(f, 0, 255).astype(np.uint8)
    hsv = cv2.cvtColor(u8, cv2.COLOR_RGB2HSV).astype(np.float32)
    hsv[...,1] = np.clip(hsv[...,1]*factor, 0, 255)
    return cv2.cvtColor(hsv.astype(np.uint8), cv2.COLOR_HSV2RGB).astype(np.float32)


def fx_lens_dirt(f):
    return np.clip(f * (1 + LENS_DIRT*0.7), 0, 255)


def fx_vignette(f):
    return f * VIGNETTE


def fx_grain(f, fidx):
    rng = np.random.default_rng(fidx * 9973)
    g   = rng.standard_normal((h, w, 3)).astype(np.float32) * 13
    return f + cv2.GaussianBlur(g, (3,3), 0)


def fx_particles(f, fidx, peak=0.0):
    global Ppos, Pvel, Plif

    # update
    Ppos += Pvel
    Plif -= Pdec
    if peak > 0.15:
        pull  = np.stack([0.5-Ppos[:,0], 0.44-Ppos[:,1]], axis=1)
        Pvel += pull * peak * 0.0018
        Plif -= Pdec * peak * 2.5

    dead = Plif <= 0
    nd   = dead.sum()
    if nd:
        Ppos[dead] = _prng.random((nd,2)).astype(np.float32)
        Plif[dead] = _prng.uniform(0.5,1.0,nd).astype(np.float32)
        Pvel[dead] = ((_prng.random((nd,2))-0.5)*0.003).astype(np.float32)
    Ppos = Ppos % 1.0

    result = f.copy()
    for i in range(NP):
        if Plif[i] <= 0:
            continue
        px = int(Ppos[i,0]*w);  py = int(Ppos[i,1]*h)
        if not (0 <= px < w and 0 <= py < h):
            continue
        alpha = float(Plif[i]) * (1 + peak*2.5)
        sz    = max(1, int(Psiz[i]*(1+peak*2)))
        col   = (Pcol[i]*255*alpha).tolist()
        cv2.circle(result, (px,py), sz, col, -1)
        if peak > 0.4 and sz > 2:
            cv2.circle(result, (px,py), sz+4, [c*0.25 for c in col], 1)
    return result


def constant_fx(f, fidx, peak=0.0):
    f = fx_heat_haze(f, fidx)
    f = fx_breathing_zoom(f, fidx)
    f = fx_colour_temp(f, fidx)
    f = fx_cinematic_grade(f)
    f = fx_saturation(f)
    f = fx_lens_dirt(f)
    f = fx_particles(f, fidx, peak)
    f = fx_vignette(f)
    f = fx_grain(f, fidx)
    return np.clip(f, 0, 255).astype(np.uint8)


# ═══════════════════════════════════════════════════════════════════════════════
#  TRANSITION EFFECTS  (peak-modulated, layered on top of morph)
# ═══════════════════════════════════════════════════════════════════════════════

def tx_god_rays(f, peak):
    if peak < 0.15:
        return f
    bright = np.clip(f.astype(np.float32) - 60, 0, None)
    acc    = f.astype(np.float32).copy()
    for i in range(1, 20):
        s  = 1 - peak * 0.016 * i
        w_ = (1 - i/20) * peak * 0.65
        acc += remap(bright, cx+(X-cx)*s, cy+(Y-cy)*s) * w_
    return acc


def tx_chromatic(f, peak):
    if peak < 0.1:
        return f
    shift = peak * 22
    r = remap(f[...,0], X+RDX*shift, Y+RDY*shift)
    g = remap(f[...,1], X,           Y)
    b = remap(f[...,2], X-RDX*shift, Y-RDY*shift)
    return np.stack([r, g, b], axis=-1)


def tx_glitch_blocks(f, peak, fidx):
    if peak < 0.3:
        return f
    rng    = np.random.default_rng(fidx*7919)
    result = f.copy()
    for _ in range(int(peak*14)+3):
        y1 = rng.integers(0, h-5)
        bh = rng.integers(2, 22)
        sh = int(rng.integers(-45, 45) * peak)
        if sh:
            result[y1:y1+bh] = np.roll(result[y1:y1+bh], sh, axis=1)
    return result


def tx_electric_arcs(f, peak, fidx):
    if peak < 0.25:
        return f
    rng    = np.random.default_rng(fidx*12345)
    layer  = np.zeros((h,w,3), dtype=np.float32)
    fcx, fcy = int(w*0.5), int(h*0.42)

    for _ in range(int(peak*12)+4):
        ang  = rng.uniform(0, 2*np.pi)
        rs   = rng.uniform(25, 90) * peak
        re   = rng.uniform(90, 280) * peak
        p0   = np.array([fcx + rs*np.cos(ang), fcy + rs*np.sin(ang)])
        a1   = ang + rng.uniform(-np.pi/2.5, np.pi/2.5)
        p1   = np.array([fcx + re*np.cos(a1),  fcy + re*np.sin(a1)])

        pts  = [p0]
        perp = np.array([-(p1[1]-p0[1]), p1[0]-p0[0]])
        pn   = np.linalg.norm(perp)
        if pn > 0: perp /= pn
        for i in range(1, 13):
            t_  = i/13
            bp  = p0*(1-t_) + p1*t_
            jit = rng.normal() * 38 * peak * np.sin(t_*np.pi)
            pts.append(bp + perp*jit)
        pts.append(p1)

        for j in range(len(pts)-1):
            a_ = tuple(np.clip(pts[j].astype(int),   [0,0],[w-1,h-1]))
            b_ = tuple(np.clip(pts[j+1].astype(int), [0,0],[w-1,h-1]))
            cv2.line(layer, a_, b_, (40,70,200), 9)
            cv2.line(layer, a_, b_, (110,160,255), 3)
            cv2.line(layer, a_, b_, (230,245,255), 1)

    glow = cv2.GaussianBlur(layer, (21,21), 7)
    return f.astype(np.float32) + glow


def tx_camera_shake(f, peak, fidx):
    if peak < 0.15:
        return f
    rng = np.random.default_rng(fidx*31337)
    sx  = int(rng.integers(-12,12)*peak)
    sy  = int(rng.integers(-12,12)*peak)
    return np.roll(np.roll(f, sx, axis=1), sy, axis=0)


def tx_flash(f, peak):
    fl = max(0, peak - 0.52) / 0.48
    return f + fl*fl*110


# ═══════════════════════════════════════════════════════════════════════════════
#  FRAME ASSEMBLY
# ═══════════════════════════════════════════════════════════════════════════════
TINT_A = np.array([0.5, 0.7, 1.0])
TINT_B = np.array([0.2, 0.8, 0.3])


def make_hold(img8, secs, foff):
    n = int(FPS * secs)
    return [constant_fx(img8.astype(np.float32), foff+i) for i in range(n)]


def make_transition(src, dst, flow_fwd, tint_s, tint_d, n, foff):
    frames = []
    for i in range(n):
        t    = i / n
        e    = ease(t)
        peak = float(np.sin(t*np.pi))

        # face morph
        wA = remap(src.astype(np.float32), X+flow_fwd[...,0]*e,   Y+flow_fwd[...,1]*e)
        wB = remap(dst.astype(np.float32), X-flow_fwd[...,0]*(1-e), Y-flow_fwd[...,1]*(1-e))
        frame = wA*(1-e) + wB*e

        # transition FX stack
        frame = tx_god_rays(frame, peak)
        frame = tx_chromatic(frame, peak)
        frame = tx_glitch_blocks(frame, peak, foff+i)
        frame = tx_electric_arcs(frame, peak, foff+i)
        frame = tx_camera_shake(frame, peak*0.65, foff+i)
        frame = tx_flash(frame, peak)

        # constant FX (always on)
        frame = constant_fx(frame, foff+i, peak)
        frames.append(frame)
    return frames


# ─── assemble timeline ────────────────────────────────────────────────────────
HOLD_SEC  = 1.5
TRANS_SEC = 2.2
LOOPS     = 3

nh = int(FPS*HOLD_SEC)
nt = int(FPS*TRANS_SEC)
cy_ = nh+nt+nh+nt

all_frames = []
for loop in range(LOOPS):
    off = loop*cy_
    all_frames += make_hold(A8, HOLD_SEC, off);                                  off += nh
    all_frames += make_transition(A8,B8, FLOW,  TINT_A,TINT_B, nt, off);        off += nt
    all_frames += make_hold(B8, HOLD_SEC, off);                                  off += nh
    all_frames += make_transition(B8,A8, -FLOW, TINT_B,TINT_A, nt, off);        off += nt
all_frames += make_hold(A8, HOLD_SEC, LOOPS*cy_)

print(f'Total frames: {len(all_frames)}  ({len(all_frames)/FPS:.1f}s)')

# ─── encode ───────────────────────────────────────────────────────────────────
raw  = os.path.join(OUT_DIR, 'trump_shapeshift_raw.mp4')
out  = os.path.join(OUT_DIR, 'trump_shapeshift.mp4')

vw = cv2.VideoWriter(raw, cv2.VideoWriter_fourcc(*'mp4v'), FPS, (w,h))
for fr in all_frames:
    vw.write(cv2.cvtColor(fr, cv2.COLOR_RGB2BGR))
vw.release()

r = subprocess.run([
    FFMPEG, '-y', '-i', raw,
    '-vcodec','libx264','-crf','15',
    '-pix_fmt','yuv420p','-movflags','+faststart', out
], capture_output=True, text=True)

if r.returncode == 0:
    print(f'Done: {out}  ({os.path.getsize(out)/1e6:.1f} MB)')
else:
    print('ffmpeg error:', r.stderr[-600:])
