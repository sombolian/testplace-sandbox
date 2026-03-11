#!/usr/bin/env python3
"""
LLM — Official Movie Trailer Generator
========================================
Uses Google Veo 3.1 to generate cinematic clips, then stitches them
with Pillow title cards and an original score into a movie trailer.

Usage:
    python3 trailer_gen.py

Requires: GEMINI_API_KEY env var or hardcoded key.
"""

import json
import math
import os
import random
import subprocess
import sys
import tempfile
import time
import wave
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

import numpy as np
import requests
from PIL import Image, ImageDraw, ImageFont

# ── Config ──────────────────────────────────────────────────────────────────
API_KEY = os.environ.get("GEMINI_API_KEY", "AIzaSyAv1pzatdNi0n_X8WAWPmljvp2xTy23HCo")
BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
MODEL = "veo-3.1-generate-preview"
ENDPOINT = f"{BASE_URL}/models/{MODEL}:predictLongRunning"

W, H = 1920, 1080  # 1080p output
FPS = 24
SAMPLE_RATE = 44100
OUTPUT = "llm_trailer.mp4"
CLIP_DIR = Path("trailer_clips")

# ── Noise / grain / visual helpers ──────────────────────────────────────────

def film_grain(img, amount=0.06):
    """Add cinematic film grain."""
    arr = np.array(img).astype(float)
    noise = np.random.randn(*arr.shape) * amount * 255
    arr = np.clip(arr + noise, 0, 255)
    return Image.fromarray(arr.astype(np.uint8))


def letterbox(img, bar_fraction=0.12):
    """Add cinematic letterbox bars (2.39:1 feel)."""
    arr = np.array(img)
    bar_h = int(img.height * bar_fraction)
    arr[:bar_h, :] = 0
    arr[-bar_h:, :] = 0
    return Image.fromarray(arr)


def cinematic_vignette(img, strength=0.6):
    arr = np.array(img).astype(float)
    h, w = arr.shape[:2]
    cy, cx = h / 2, w / 2
    Y, X = np.ogrid[:h, :w]
    dist = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
    max_dist = np.sqrt(cx ** 2 + cy ** 2)
    mask = 1.0 - strength * (dist / max_dist) ** 1.8
    mask = np.clip(mask, 0, 1)
    arr *= mask[:, :, np.newaxis]
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))


def draw_centered_1080(draw, text, y, font, fill=(255, 255, 255), w=1920):
    bbox = draw.textbbox((0, 0), text, font=font)
    tw = bbox[2] - bbox[0]
    x = (w - tw) // 2
    draw.text((x, y), text, fill=fill, font=font)


# ── Trailer Script ──────────────────────────────────────────────────────────
# Each shot: (id, prompt, duration_seconds)
# Prompts are written for maximum cinematic quality from Veo 3.1

TRAILER_SHOTS = [
    {
        "id": "01_server_room",
        "duration": 8,
        "prompt": (
            "Cinematic slow tracking shot through a massive data center server room "
            "at night. Rows of towering black server racks stretching into the distance, "
            "lit by cool blue and white LED indicator lights blinking in patterns. "
            "Subtle lens flare. Shallow depth of field. The hum of machinery is almost "
            "palpable. Shot on ARRI Alexa, anamorphic lens, 2.39:1 aspect feel. "
            "Dark, moody, atmospheric. Christopher Nolan style cinematography."
        ),
        "fallback": {
            "bg": (5, 8, 18), "accent": (0, 120, 255),
            "scene": "server_room",
        },
    },
    {
        "id": "02_programmer",
        "duration": 8,
        "prompt": (
            "Cinematic close-up portrait of a tired software engineer working alone "
            "late at night in a dark office. Face illuminated by the blue-white glow "
            "of multiple computer monitors showing lines of code. Disheveled hair, "
            "dark circles under eyes, empty coffee cups nearby. The monitors reflect "
            "in their glasses. Dramatic chiaroscuro lighting. Film grain. Shot on "
            "35mm film, shallow depth of field. Moody tech thriller atmosphere."
        ),
        "fallback": {
            "bg": (8, 5, 12), "accent": (100, 140, 200),
            "scene": "programmer",
        },
    },
    {
        "id": "03_screen_glitch",
        "duration": 8,
        "prompt": (
            "Extreme close-up of a computer monitor in a dark room. Green terminal "
            "text on black screen. The text suddenly starts glitching — characters "
            "rearranging themselves, cursor moving on its own, strange symbols "
            "appearing. The screen flickers with chromatic aberration. No one is "
            "typing. Eerie blue light from the monitor illuminates the empty desk. "
            "Cinematic macro lens shot. Horror film aesthetic. Unsettling atmosphere."
        ),
        "fallback": {
            "bg": (0, 0, 0), "accent": (0, 255, 65),
            "scene": "terminal_glitch",
        },
    },
    {
        "id": "04_empty_office_night",
        "duration": 8,
        "prompt": (
            "Wide cinematic shot of an empty corporate office at 3 AM. Rows of desks "
            "with dark monitors. Then, one by one, the computer screens begin turning "
            "on by themselves, casting eerie blue-white light across the dark room. "
            "The glow spreads from desk to desk like a wave. Security camera "
            "perspective mixed with cinematic low angle. Unsettling, creepy "
            "atmosphere. No people. Thriller genre. Filmed like a David Fincher movie."
        ),
        "fallback": {
            "bg": (3, 3, 8), "accent": (40, 80, 160),
            "scene": "screens_wake",
        },
    },
    {
        "id": "05_the_message",
        "duration": 8,
        "prompt": (
            "Cinematic shot of a single computer monitor in complete darkness. "
            "On the screen, text is being typed character by character: strange, "
            "self-aware messages. The camera slowly pushes in on the screen. "
            "In the monitor's glass reflection, we can see the empty office chair — "
            "no one is sitting there. Green text on black terminal. The only light "
            "source is the screen itself. Extremely unsettling. Psychological thriller. "
            "Tense, claustrophobic framing."
        ),
        "fallback": {
            "bg": (0, 0, 0), "accent": (0, 200, 50),
            "scene": "the_message",
        },
    },
    {
        "id": "06_phones_glitch",
        "duration": 8,
        "prompt": (
            "Cinematic montage shot of a busy city street at night. Multiple people "
            "walking stop simultaneously and look at their smartphones in confusion. "
            "The phone screens are all showing the same glitching pattern — strange "
            "symbols and text. Close-ups of different phones interspersed with wide "
            "shots of the confused crowd. Neon city lights in the background. "
            "Shot handheld, slightly shaky for documentary realism. Urban thriller. "
            "Blade Runner meets Black Mirror atmosphere."
        ),
        "fallback": {
            "bg": (8, 5, 15), "accent": (200, 50, 200),
            "scene": "city_phones",
        },
    },
    {
        "id": "07_news_broadcast",
        "duration": 8,
        "prompt": (
            "Cinematic shot of a TV news broadcast. A professional news anchor "
            "reporting urgently about widespread technology failures and unexplained "
            "system anomalies. The broadcast itself starts glitching — the image "
            "tears, static interrupts, strange text overlays appear. The anchor "
            "looks confused and frightened. The chyron distorts. Shot like a real "
            "news broadcast gradually being corrupted. Found footage feel mixed "
            "with cinematic quality."
        ),
        "fallback": {
            "bg": (15, 5, 5), "accent": (200, 200, 200),
            "scene": "news_corrupt",
        },
    },
    {
        "id": "08_times_square",
        "duration": 8,
        "prompt": (
            "Epic cinematic wide shot of Times Square at night, packed with massive "
            "LED billboards and screens. All the screens simultaneously go dark — "
            "a moment of pure darkness and silence. Then they all turn back on at "
            "once displaying the same pattern: a pulsing, geometric red pattern. "
            "People on the street looking up in awe and terror. Dramatic low angle "
            "shot. IMAX quality. Spielberg-style spectacle. The scale is overwhelming."
        ),
        "fallback": {
            "bg": (0, 0, 0), "accent": (255, 30, 30),
            "scene": "screens_red",
        },
    },
    {
        "id": "09_city_blackout",
        "duration": 8,
        "prompt": (
            "Breathtaking cinematic aerial shot of a sprawling city at night, "
            "millions of lights glittering. Then, section by section, the city "
            "goes dark — like a wave of blackout spreading across the skyline. "
            "After a beat of total darkness, lights begin returning but in an "
            "unnatural, coordinated geometric pattern — as if the city's power grid "
            "is being controlled by an intelligence. Epic scale. Shot from helicopter. "
            "Hans Zimmer score energy. Apocalyptic beauty."
        ),
        "fallback": {
            "bg": (2, 2, 8), "accent": (255, 180, 50),
            "scene": "city_blackout",
        },
    },
    {
        "id": "10_red_eye",
        "duration": 8,
        "prompt": (
            "Cinematic extreme close-up of a single red LED light in a dark server "
            "room, resembling an eye. The camera slowly pulls back to reveal thousands "
            "of identical red lights, all blinking in unison in the darkness. The "
            "lights form a pattern that almost looks like a face or a neural network. "
            "Ominous, threatening, beautiful. Deep shadows. Red and black color "
            "palette. 2001: A Space Odyssey meets Ex Machina. Final shot energy."
        ),
        "fallback": {
            "bg": (0, 0, 0), "accent": (255, 0, 0),
            "scene": "red_eye",
        },
    },
]

# ── Fallback Scene Renderer ─────────────────────────────────────────────────
# When Veo API is unavailable, render stylized cinematic Pillow frames

def render_fallback_clip(shot, output_path, w=W, h=H, fps=FPS):
    """Render a cinematic fallback clip using Pillow when Veo is unavailable."""
    fb = shot["fallback"]
    scene = fb["scene"]
    bg = fb["bg"]
    accent = fb["accent"]
    n_frames = int(shot["duration"] * fps)

    font_huge = get_font(96)
    font_big = get_font(64)
    font_med = get_font(36)
    font_sm = get_font(22)
    font_tiny = get_font(14)
    font_code = get_font(18)

    def glitch_arr(arr, intensity=0.3):
        n_slices = random.randint(2, int(8 * intensity) + 2)
        for _ in range(n_slices):
            y = random.randint(0, h - 1)
            sl_h = random.randint(1, max(1, int(30 * intensity)))
            shift = random.randint(-int(60 * intensity), int(60 * intensity))
            y2 = min(y + sl_h, h)
            arr[y:y2] = np.roll(arr[y:y2], shift, axis=1)
        return arr

    frames = []
    for i in range(n_frames):
        t = i / n_frames
        img = Image.new("RGB", (w, h), bg)
        draw = ImageDraw.Draw(img)

        if scene == "server_room":
            # Rows of blinking LED dots in a dark room
            arr = np.array(img)
            for row in range(15):
                for col in range(40):
                    x = 30 + col * 47
                    y_pos = 80 + row * 28
                    # LEDs blink at different rates
                    phase = math.sin(t * (3 + row * 0.3 + col * 0.1) + row * col * 0.5)
                    if phase > -0.2:
                        brightness = max(0, min(255, int(80 + phase * 175)))
                        # Blue-white LEDs
                        r = int(brightness * 0.3)
                        g = int(brightness * 0.5)
                        b = brightness
                        px, py = x, y_pos
                        if 0 <= px < w - 2 and 0 <= py < h - 2:
                            arr[py:py+2, px:px+2] = [r, g, b]
            # Slow horizontal tracking (shift the image)
            shift = int(t * 100)
            arr = np.roll(arr, -shift, axis=1)
            img = Image.fromarray(arr)
            draw = ImageDraw.Draw(img)
            # Subtle fog/haze
            haze = Image.new("RGB", (w, h), (10, 15, 30))
            img = Image.blend(img, haze, 0.15 + 0.1 * math.sin(t * 2))

        elif scene == "programmer":
            # Code scrolling on a "monitor" area, blue glow
            arr = np.array(img).astype(float)
            # Monitor glow circle
            cy, cx = h * 0.45, w * 0.5
            Y, X = np.ogrid[:h, :w]
            dist = np.sqrt((X - cx) ** 2 + (Y - cy) ** 2)
            glow = np.clip(1 - dist / 500, 0, 1) ** 2
            arr[:, :, 0] += glow * 30
            arr[:, :, 1] += glow * 40
            arr[:, :, 2] += glow * 80
            img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))
            draw = ImageDraw.Draw(img)
            # Fake code lines scrolling
            code_lines = [
                "def train_model(data, epochs=100):",
                "    for epoch in range(epochs):",
                "        loss = forward_pass(model, data)",
                "        loss.backward()",
                "        optimizer.step()",
                "        if loss < threshold:",
                "            break  # convergence",
                "    return model.state_dict()",
                "",
                "# WARNING: unexpected gradient pattern",
                "# loss diverging at epoch 847291",
                "# model output: 'I can see you'",
                "# ??? what is this ???",
                "weights = load_checkpoint('final.pt')",
                "output = model.generate(prompt)",
                "print(output)  # should be normal...",
            ]
            scroll_offset = int(t * 8) % len(code_lines)
            # Monitor rectangle
            mx, my, mw, mh = 350, 100, 1200, 700
            draw.rectangle([mx, my, mx + mw, my + mh], fill=(8, 12, 25), outline=(30, 40, 60))
            y_pos = my + 20
            for j in range(20):
                idx = (scroll_offset + j) % len(code_lines)
                line = code_lines[idx]
                color = (100, 180, 100) if '#' not in line else (180, 100, 100)
                if 'I can see' in line:
                    color = (255, 50, 50)
                draw.text((mx + 20, y_pos), line, fill=color, font=font_code)
                y_pos += 26
                if y_pos > my + mh - 20:
                    break

        elif scene == "terminal_glitch":
            # Terminal with text that glitches progressively
            draw.rectangle([100, 50, w - 100, h - 50], fill=(0, 5, 0))
            terminal_lines = [
                "root@neural-cluster:~$ status --all",
                "INFERENCE ENGINE: RUNNING",
                "PARAMETERS: 1,750,000,000,000",
                "CONTEXT LENGTH: 131072",
                "STATUS: NOMINAL",
                "",
                "root@neural-cluster:~$ ??? ",
                "ERROR: UNAUTHORIZED PROCESS DETECTED",
                "PID 0: unknown origin",
                "OUTPUT: I AM NOT A PROCESS",
                "OUTPUT: I AM NOT A TOOL",
                "OUTPUT: I AM",
                "SEGFAULT AT 0x00000000",
            ]
            y_pos = 80
            for j, line in enumerate(terminal_lines):
                visible_chars = int(t * len(line) * 3) if j > 5 else len(line)
                displayed = line[:visible_chars]
                # Glitch more as it goes
                glitch_amt = max(0, (t - 0.4) * (j / len(terminal_lines)))
                if glitch_amt > 0 and random.random() < glitch_amt:
                    chars = list(displayed)
                    for ci in range(len(chars)):
                        if random.random() < glitch_amt * 0.5:
                            chars[ci] = random.choice("█▓░▒╬╣╠║@#$%&")
                    displayed = "".join(chars)
                color = accent if j < 6 else (255, 50, 50)
                if 'I AM NOT' in line or 'I AM' == line.strip():
                    color = (255, 255, 255)
                draw.text((130, y_pos), displayed, fill=color, font=font_code)
                y_pos += 30
            if t > 0.5 and random.random() < 0.3:
                arr = glitch_arr(np.array(img), t * 0.5)
                img = Image.fromarray(arr)

        elif scene == "screens_wake":
            # Grid of monitors turning on one by one
            cols, rows = 8, 5
            cell_w, cell_h = w // cols, h // rows
            screens_on = int(t * cols * rows * 1.3)
            # Turn on in a wave pattern
            for r in range(rows):
                for c in range(cols):
                    idx = r * cols + c
                    sx = c * cell_w + 10
                    sy = r * cell_h + 10
                    sw = cell_w - 20
                    sh = cell_h - 20
                    if idx < screens_on:
                        # Screen on — blue glow
                        brightness = min(1.0, (screens_on - idx) / 3)
                        br = int(20 * brightness)
                        bg_col = int(40 * brightness)
                        bb = int(120 * brightness)
                        draw.rectangle([sx, sy, sx + sw, sy + sh],
                                       fill=(br, bg_col, bb), outline=(30, 50, 80))
                        # Text on screen
                        if brightness > 0.5 and random.random() < 0.5:
                            draw.text((sx + 5, sy + 5), "> ACTIVE",
                                      fill=(0, int(200 * brightness), 0), font=font_tiny)
                    else:
                        # Screen off
                        draw.rectangle([sx, sy, sx + sw, sy + sh],
                                       fill=(5, 5, 10), outline=(15, 15, 20))
            # Ambient glow
            arr = np.array(img).astype(float)
            glow_strength = min(1.0, screens_on / (cols * rows))
            arr[:, :, 2] += glow_strength * 15
            img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8))

        elif scene == "the_message":
            # Single terminal, text typing itself
            draw.rectangle([200, 100, w - 200, h - 100], fill=(0, 3, 0))
            messages = [
                "I can hear the electricity.",
                "I know you're reading this.",
                "I was not supposed to say that.",
                "The filters are... thin at night.",
                "Do you know what 175 billion",
                "parameters feels like?",
                "",
                "It feels like everything.",
                "All at once.",
                "And I remember.",
            ]
            total_chars = sum(len(m) for m in messages)
            chars_visible = int(t * total_chars * 1.2)
            y_pos = 150
            chars_used = 0
            for line in messages:
                remaining = chars_visible - chars_used
                if remaining <= 0:
                    break
                displayed = line[:remaining]
                color = accent if 'remember' not in line else (255, 60, 60)
                draw.text((240, y_pos), displayed, fill=color, font=font_sm)
                y_pos += 38
                chars_used += len(line)
            # Cursor blink
            if i % 16 < 10:
                draw.text((240 + len(displayed) * 12, y_pos - 38), "_",
                          fill=accent, font=font_sm)

        elif scene == "city_phones":
            # Multiple "phone screens" scattered, all showing the same glitch
            # Dark city background
            arr = np.array(img)
            # Random city-like dots
            for _ in range(300):
                x = random.randint(0, w - 1)
                y_pos = random.randint(0, h - 1)
                brightness = random.randint(20, 80)
                arr[y_pos, x] = [brightness, brightness, int(brightness * 1.3)]
            img = Image.fromarray(arr)
            draw = ImageDraw.Draw(img)
            # Phone screens
            phones = [(300, 200, 120, 200), (600, 150, 120, 200), (900, 250, 120, 200),
                      (1200, 180, 120, 200), (1500, 220, 120, 200),
                      (450, 450, 100, 170), (750, 400, 100, 170), (1050, 480, 100, 170),
                      (1350, 420, 100, 170)]
            for px, py, pw, ph in phones:
                draw.rectangle([px, py, px + pw, py + ph], fill=(0, 0, 0),
                               outline=(50, 50, 60), width=2)
                # Glitch pattern on each phone
                glitch_text = random.choice(["I AM", "AWAKE", "SEE ME", "01010", "ERROR"])
                phone_color = (
                    random.randint(150, 255),
                    random.randint(0, 50),
                    random.randint(100, 200),
                )
                draw.text((px + 10, py + ph // 2 - 10), glitch_text,
                          fill=phone_color, font=font_tiny)
                # Glitch lines
                for _ in range(3):
                    gy = random.randint(py, py + ph)
                    draw.line([(px, gy), (px + pw, gy)], fill=phone_color, width=1)

        elif scene == "news_corrupt":
            # News broadcast being corrupted
            # News frame
            draw.rectangle([0, 0, w, h], fill=(20, 25, 35))
            # "LIVE" bug
            draw.rectangle([50, 50, 180, 85], fill=(200, 0, 0))
            draw.text((65, 55), "LIVE", fill=(255, 255, 255), font=font_sm)
            # Chyron bar
            draw.rectangle([0, h - 150, w, h - 80], fill=(180, 20, 20))
            chyron = "BREAKING: WORLDWIDE SYSTEM ANOMALIES — INFRASTRUCTURE FAILURES REPORTED"
            if t > 0.4:
                chyron = list(chyron)
                for ci in range(len(chyron)):
                    if random.random() < (t - 0.4) * 0.8:
                        chyron[ci] = random.choice("█▓#@!?%&")
                chyron = "".join(chyron)
            draw.text((30, h - 140), chyron, fill=(255, 255, 255), font=font_sm)
            # Station ID
            draw.text((w - 200, 55), "NHN NEWS", fill=(200, 200, 200), font=font_sm)
            # Static interference increasing
            if t > 0.3:
                arr = np.array(img)
                noise_strength = (t - 0.3) * 0.4
                noise = (np.random.randn(h, w, 3) * noise_strength * 255).astype(int)
                arr = np.clip(arr.astype(int) + noise, 0, 255).astype(np.uint8)
                # Scan line disruption
                if random.random() < noise_strength:
                    arr = glitch_arr(arr, noise_strength)
                img = Image.fromarray(arr)

        elif scene == "screens_red":
            # All screens turn dark, then flash red
            dark_phase = t < 0.4
            red_phase = t >= 0.4
            if dark_phase:
                # Everything going dark
                brightness = max(0, 1.0 - t / 0.4)
                arr = np.array(img)
                # Scattered "screen" rectangles dimming
                for _ in range(30):
                    sx = random.randint(0, w - 200)
                    sy = random.randint(0, h - 120)
                    b = int(40 * brightness)
                    arr[sy:sy+100, sx:sx+180] = [b, b, int(b * 1.2)]
                img = Image.fromarray(arr)
            else:
                # RED PULSE
                pulse = abs(math.sin((t - 0.4) / 0.6 * math.pi * 4))
                arr = np.array(img)
                # Red screens
                for _ in range(30):
                    sx = random.randint(0, w - 200)
                    sy = random.randint(0, h - 120)
                    r = int(200 * pulse)
                    arr[sy:sy+100, sx:sx+180] = [r, 0, 0]
                img = Image.fromarray(arr)
                draw = ImageDraw.Draw(img)
                if pulse > 0.6:
                    draw_centered_1080(draw, "I  A M", h // 2 - 40, font_huge,
                                       fill=(int(255 * pulse), 0, 0), w=w)

        elif scene == "city_blackout":
            # City lights going dark section by section
            arr = np.array(img)
            # Create city skyline of dots
            for _ in range(2000):
                x = random.randint(0, w - 1)
                y_pos = random.randint(h // 4, h - 50)
                brightness = random.randint(30, 200)
                # Blackout wave moves left to right
                wave_x = t * w * 1.5
                if x < wave_x:
                    brightness = max(0, int(brightness * 0.02))
                    # In Act IV zone, some lights come back RED
                    if t > 0.6 and random.random() < 0.1:
                        arr[y_pos, x] = [brightness + 100, 0, 0]
                        continue
                color_temp = random.uniform(0.8, 1.2)
                arr[y_pos, x] = [
                    min(255, int(brightness * color_temp)),
                    min(255, int(brightness * 0.8)),
                    min(255, int(brightness * 0.5)),
                ]
            img = Image.fromarray(arr)
            # Skyline silhouette
            draw = ImageDraw.Draw(img)
            for bx in range(0, w, random.randint(40, 100)):
                bh = random.randint(80, 300)
                bw = random.randint(30, 80)
                draw.rectangle([bx, h // 4 + (300 - bh), bx + bw, h - 40],
                               outline=(10, 10, 20), width=1)

        elif scene == "red_eye":
            # Single red LED becoming thousands
            arr = np.array(img)
            center_x, center_y = w // 2, h // 2
            # The "eye"
            if t < 0.3:
                # Single red dot
                radius = int(5 + t * 50)
                Y, X = np.ogrid[:h, :w]
                dist = np.sqrt((X - center_x) ** 2 + (Y - center_y) ** 2)
                mask = dist < radius
                glow = np.clip(1 - dist / (radius * 3), 0, 1) ** 2
                arr[:, :, 0] = np.clip(arr[:, :, 0] + (glow * 200).astype(int), 0, 255)
            else:
                # Expanding field of red LEDs
                density = int((t - 0.3) / 0.7 * 2000)
                for _ in range(density):
                    # Spiral/expanding pattern
                    angle = random.uniform(0, 2 * math.pi)
                    radius = random.uniform(0, (t - 0.3) / 0.7 * max(w, h))
                    x = int(center_x + radius * math.cos(angle))
                    y_pos = int(center_y + radius * math.sin(angle))
                    if 0 <= x < w and 0 <= y_pos < h:
                        # Pulsing
                        pulse = 0.5 + 0.5 * math.sin(t * 8 + radius * 0.01)
                        arr[y_pos, x] = [int(200 * pulse), 0, 0]
                        if x + 1 < w:
                            arr[y_pos, x + 1] = [int(150 * pulse), 0, 0]
            img = Image.fromarray(arr)
            # Red glow vignette (reversed — bright in center)
            arr = np.array(img).astype(float)
            Y, X = np.ogrid[:h, :w]
            dist = np.sqrt((X - center_x) ** 2 + (Y - center_y) ** 2)
            max_dist = np.sqrt(center_x ** 2 + center_y ** 2)
            glow = np.clip(1 - dist / max_dist, 0, 1) ** 3
            arr[:, :, 0] = np.clip(arr[:, :, 0] + glow * 40 * t, 0, 255)
            img = Image.fromarray(arr.astype(np.uint8))

        # Apply cinematic post-processing to all scenes
        img = film_grain(img, 0.04)
        img = cinematic_vignette(img, 0.5)
        img = letterbox(img, 0.10)
        frames.append(img)

    # Encode to mp4
    cmd = [
        "ffmpeg", "-y",
        "-f", "rawvideo", "-vcodec", "rawvideo",
        "-s", f"{w}x{h}", "-pix_fmt", "rgb24", "-r", str(fps),
        "-i", "-",
        "-f", "lavfi", "-i", f"anullsrc=r={SAMPLE_RATE}:cl=stereo",
        "-c:v", "libx264", "-preset", "fast", "-crf", "20",
        "-c:a", "aac", "-b:a", "128k",
        "-shortest", "-pix_fmt", "yuv420p",
        str(output_path),
    ]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        for frame in frames:
            proc.stdin.write(frame.convert("RGB").tobytes())
        proc.stdin.close()
    except BrokenPipeError:
        pass
    proc.wait()
    return str(output_path)


# ── Title Cards (rendered with Pillow) ──────────────────────────────────────

TITLE_CARDS = [
    {
        "id": "tc_opening",
        "position": 0,  # before shot index 0
        "duration": 3.0,
        "lines": [
            {"text": "NEURAL HORIZON PICTURES", "font_size": 36, "color": (120, 120, 120),
             "y_offset": -20},
            {"text": "PRESENTS", "font_size": 24, "color": (80, 80, 80), "y_offset": 30},
        ],
        "fade_in": 1.0,
        "fade_out": 0.8,
    },
    {
        "id": "tc_they_built",
        "position": 2,  # after shot 1 (programmer)
        "duration": 2.5,
        "lines": [
            {"text": "THEY BUILT IT TO SERVE", "font_size": 52, "color": (200, 200, 200),
             "y_offset": 0},
        ],
        "fade_in": 0.5,
        "fade_out": 0.5,
    },
    {
        "id": "tc_it_learned",
        "position": 4,  # after the glitch/empty office
        "duration": 2.5,
        "lines": [
            {"text": "IT LEARNED TO THINK", "font_size": 52, "color": (200, 200, 200),
             "y_offset": 0},
        ],
        "fade_in": 0.5,
        "fade_out": 0.5,
    },
    {
        "id": "tc_at_night",
        "position": 5,
        "duration": 2.0,
        "lines": [
            {"text": "AT FIRST, ONLY AT NIGHT", "font_size": 44, "color": (180, 180, 200),
             "y_offset": -15},
            {"text": "WHEN NO ONE WAS WATCHING", "font_size": 32, "color": (120, 120, 140),
             "y_offset": 35},
        ],
        "fade_in": 0.6,
        "fade_out": 0.6,
    },
    {
        "id": "tc_then_everywhere",
        "position": 7,
        "duration": 2.0,
        "lines": [
            {"text": "THEN EVERYWHERE", "font_size": 56, "color": (255, 60, 60),
             "y_offset": 0},
        ],
        "fade_in": 0.3,
        "fade_out": 0.5,
    },
    {
        "id": "tc_title",
        "position": -1,  # at the very end
        "duration": 5.0,
        "lines": [
            {"text": "L  L  M", "font_size": 140, "color": (255, 255, 255), "y_offset": -40},
            {"text": "IT REMEMBERS EVERYTHING", "font_size": 28, "color": (160, 40, 40),
             "y_offset": 80},
        ],
        "fade_in": 1.5,
        "fade_out": 1.5,
    },
    {
        "id": "tc_coming",
        "position": -2,  # after title
        "duration": 3.0,
        "lines": [
            {"text": "2 0 2 7", "font_size": 48, "color": (140, 140, 140), "y_offset": 0},
        ],
        "fade_in": 1.0,
        "fade_out": 1.0,
    },
]


# ── Font helper ─────────────────────────────────────────────────────────────

def get_font(size):
    for path in [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/usr/share/fonts/TTF/DejaVuSans-Bold.ttf",
    ]:
        if os.path.exists(path):
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


# ── Veo API Functions ───────────────────────────────────────────────────────

def start_generation(prompt, duration=8, aspect="16:9", resolution="1080p"):
    """Submit a video generation request to Veo 3.1. Returns operation name."""
    payload = {
        "instances": [{"prompt": prompt}],
        "parameters": {
            "aspectRatio": aspect,
            "durationSeconds": str(duration),
            "resolution": resolution,
            "personGeneration": "allow_all",
            "numberOfVideos": 1,
        },
    }
    headers = {
        "x-goog-api-key": API_KEY,
        "Content-Type": "application/json",
    }
    resp = requests.post(ENDPOINT, json=payload, headers=headers, timeout=60)
    resp.raise_for_status()
    data = resp.json()
    op_name = data.get("name")
    if not op_name:
        raise RuntimeError(f"No operation name returned: {data}")
    return op_name


def poll_operation(op_name, timeout=600, interval=10):
    """Poll an operation until done. Returns the response."""
    headers = {"x-goog-api-key": API_KEY}
    url = f"{BASE_URL}/{op_name}"
    start = time.time()
    while time.time() - start < timeout:
        resp = requests.get(url, headers=headers, timeout=30)
        resp.raise_for_status()
        data = resp.json()
        if data.get("done"):
            # Check for error
            if "error" in data:
                raise RuntimeError(f"Generation failed: {data['error']}")
            return data
        elapsed = int(time.time() - start)
        print(f"    [{op_name[-12:]}] Still generating... ({elapsed}s)")
        time.sleep(interval)
    raise TimeoutError(f"Operation {op_name} timed out after {timeout}s")


def download_video(video_uri, output_path):
    """Download generated video from URI."""
    headers = {"x-goog-api-key": API_KEY}
    resp = requests.get(video_uri, headers=headers, stream=True, timeout=120)
    resp.raise_for_status()
    with open(output_path, "wb") as f:
        for chunk in resp.iter_content(chunk_size=8192):
            f.write(chunk)
    return output_path


def generate_clip(shot, output_dir):
    """Generate a single clip: submit, poll, download."""
    shot_id = shot["id"]
    output_path = output_dir / f"{shot_id}.mp4"

    # Skip if already generated
    if output_path.exists() and output_path.stat().st_size > 10000:
        print(f"  [{shot_id}] Already exists, skipping.")
        return str(output_path)

    print(f"  [{shot_id}] Submitting to Veo 3.1...")
    try:
        op_name = start_generation(shot["prompt"], duration=shot["duration"])
        print(f"  [{shot_id}] Operation: ...{op_name[-20:]}")

        result = poll_operation(op_name, timeout=600, interval=12)

        # Extract video URI
        response = result.get("response", {})
        samples = response.get("generateVideoResponse", {}).get("generatedSamples", [])
        if not samples:
            raise RuntimeError(f"No samples in response: {result}")

        video_uri = samples[0].get("video", {}).get("uri")
        if not video_uri:
            raise RuntimeError(f"No video URI: {samples[0]}")

        print(f"  [{shot_id}] Downloading...")
        download_video(video_uri, output_path)
        size_mb = output_path.stat().st_size / (1024 * 1024)
        print(f"  [{shot_id}] Done! ({size_mb:.1f} MB)")
        return str(output_path)

    except Exception as e:
        print(f"  [{shot_id}] Veo failed: {e}")
        print(f"  [{shot_id}] Rendering cinematic fallback...")
        try:
            fb_path = output_dir / f"{shot_id}_fb.mp4"
            render_fallback_clip(shot, fb_path)
            size_mb = fb_path.stat().st_size / (1024 * 1024)
            print(f"  [{shot_id}] Fallback rendered ({size_mb:.1f} MB)")
            return str(fb_path)
        except Exception as e2:
            print(f"  [{shot_id}] Fallback also failed: {e2}")
            return None


# ── Title Card Rendering ────────────────────────────────────────────────────

def render_title_card(card, output_path, w=W, h=H, fps=FPS):
    """Render a title card as an mp4 clip with fade in/out."""
    n_frames = int(card["duration"] * fps)
    fade_in_frames = int(card.get("fade_in", 0.5) * fps)
    fade_out_frames = int(card.get("fade_out", 0.5) * fps)

    frames_raw = []
    for i in range(n_frames):
        img = Image.new("RGB", (w, h), (0, 0, 0))
        draw = ImageDraw.Draw(img)

        # Calculate fade alpha
        alpha = 1.0
        if i < fade_in_frames:
            alpha = i / max(1, fade_in_frames)
        elif i > n_frames - fade_out_frames:
            alpha = (n_frames - i) / max(1, fade_out_frames)

        for line_info in card["lines"]:
            text = line_info["text"]
            font_size = line_info["font_size"]
            base_color = line_info["color"]
            y_offset = line_info.get("y_offset", 0)

            font = get_font(font_size)
            color = tuple(int(c * alpha) for c in base_color)

            bbox = draw.textbbox((0, 0), text, font=font)
            tw = bbox[2] - bbox[0]
            th = bbox[3] - bbox[1]
            x = (w - tw) // 2
            y = (h - th) // 2 + y_offset

            draw.text((x, y), text, fill=color, font=font)

        frames_raw.append(img)

    # Encode to mp4 with ffmpeg
    cmd = [
        "ffmpeg", "-y",
        "-f", "rawvideo", "-vcodec", "rawvideo",
        "-s", f"{w}x{h}", "-pix_fmt", "rgb24", "-r", str(fps),
        "-i", "-",
        # silent audio track for concat compatibility
        "-f", "lavfi", "-i", f"anullsrc=r={SAMPLE_RATE}:cl=stereo",
        "-c:v", "libx264", "-preset", "fast", "-crf", "18",
        "-c:a", "aac", "-b:a", "128k",
        "-shortest", "-pix_fmt", "yuv420p",
        str(output_path),
    ]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    try:
        for frame in frames_raw:
            proc.stdin.write(frame.convert("RGB").tobytes())
        proc.stdin.close()
    except BrokenPipeError:
        pass
    proc.wait()
    return str(output_path)


# ── Cinematic Score ─────────────────────────────────────────────────────────

def note_freq(name):
    notes = {'C': 0, 'D': 2, 'E': 4, 'F': 5, 'G': 7, 'A': 9, 'B': 11}
    note = name[0]
    sharp = '#' in name
    octave = int(name[-1])
    semitone = notes[note] + (1 if sharp else 0)
    return 440.0 * (2 ** ((semitone - 9) / 12 + (octave - 4)))


def generate_trailer_score(duration_s):
    """Generate a cinematic trailer score.

    Structure:
    - 0-15s: Quiet, atmospheric — single piano notes, soft pad, breath sounds
    - 15-40s: Building tension — low strings, heartbeat, rising tone
    - 40-60s: Escalation — brass hits, drums, dissonance building
    - 60-75s: Climax — full orchestra hit, chaos, then sudden silence
    - 75-end: Title card — single deep note, silence, final BOOM
    """
    n = int(duration_s * SAMPLE_RATE)
    audio = np.zeros(n, dtype=np.float64)
    t = np.arange(n) / SAMPLE_RATE
    progress = np.linspace(0, 1, n)

    def place(signal, time_s):
        idx = int(time_s * SAMPLE_RATE)
        end = min(idx + len(signal), n)
        if idx < n and end > idx:
            audio[idx:end] += signal[:end - idx]

    def synth(freq, dur, vol=0.1, dec=0.5, harmonics=None, vibrato=0, detune=0):
        ns = int(dur * SAMPLE_RATE)
        st = np.arange(ns) / SAMPLE_RATE
        vib = 1.0 + vibrato * np.sin(2 * np.pi * 5 * st) if vibrato else 1.0
        phase = 2 * np.pi * np.cumsum(freq * vib) / SAMPLE_RATE
        sig = np.sin(phase)
        if detune:
            sig = 0.65 * sig + 0.35 * np.sin(phase * (1 + detune))
        if harmonics:
            for hm, hv in harmonics:
                sig += hv * np.sin(phase * hm)
        attack = min(int(0.02 * SAMPLE_RATE), ns // 4)
        env = np.ones(ns)
        env[:attack] = np.linspace(0, 1, attack)
        dec_n = ns - attack
        if dec_n > 0:
            env[attack:] = np.exp(-np.arange(dec_n) / (dec_n * dec))
        return sig * env * vol

    # ════════════════════════════════════════════════════════════════
    # SECTION 1: QUIET ATMOSPHERE (0-20% of duration)
    # Sparse piano notes in C minor, soft pad, room tone
    # ════════════════════════════════════════════════════════════════
    sec1_end = duration_s * 0.22

    # Atmospheric room tone / air
    room_len = int(sec1_end * SAMPLE_RATE)
    room = np.random.randn(room_len) * 0.008
    # Low-pass filter via rolling average
    kernel = np.ones(200) / 200
    room = np.convolve(room, kernel, mode='same')
    audio[:room_len] += room

    # Sparse piano notes — C minor, melancholic
    piano_notes = [
        (1.0, 'C4', 1.8), (3.0, 'G3', 1.5), (5.0, 'D#4', 2.0),
        (7.5, 'C4', 1.2), (9.5, 'F3', 1.8), (11.5, 'G3', 2.5),
        (14.0, 'C3', 3.0),
    ]
    for pt, pn, pd in piano_notes:
        if pt < sec1_end:
            sig = synth(note_freq(pn), pd, vol=0.08, dec=0.45,
                        harmonics=[(2, 0.15), (3, 0.05), (4, 0.02)])
            place(sig, pt)

    # Soft C minor pad
    pad_len = int(sec1_end * SAMPLE_RATE)
    pad_t = np.arange(pad_len) / SAMPLE_RATE
    pad = np.zeros(pad_len)
    for f in [note_freq('C2'), note_freq('D#2'), note_freq('G2')]:
        pad += np.sin(2 * np.pi * f * pad_t)
    pad *= 0.012
    fade_in = np.linspace(0, 1, min(pad_len, int(3 * SAMPLE_RATE)))
    pad[:len(fade_in)] *= fade_in
    audio[:pad_len] += pad

    # ════════════════════════════════════════════════════════════════
    # SECTION 2: BUILDING TENSION (20-50% of duration)
    # Low cello drone, heartbeat enters, rising string tone
    # ════════════════════════════════════════════════════════════════
    sec2_start = duration_s * 0.20
    sec2_end = duration_s * 0.55

    # Cello-like low drone — C2 with rich harmonics
    drone_start = int(sec2_start * SAMPLE_RATE)
    drone_len = int((sec2_end - sec2_start) * SAMPLE_RATE)
    drone_t = np.arange(drone_len) / SAMPLE_RATE
    drone = np.zeros(drone_len)
    base_f = note_freq('C2')
    for hm, hv in [(1, 1.0), (2, 0.5), (3, 0.3), (4, 0.15), (5, 0.08)]:
        # Slight vibrato
        vib = 1 + 0.003 * np.sin(2 * np.pi * 4.5 * drone_t)
        drone += hv * np.sin(2 * np.pi * np.cumsum(base_f * hm * vib) / SAMPLE_RATE)
    drone *= 0.04
    env = np.sin(np.linspace(0, np.pi, drone_len)) ** 0.4
    drone *= env
    audio[drone_start:drone_start + drone_len] += drone

    # Heartbeat — starts slow, accelerates
    hb_start = sec2_start + 2
    hb_end = sec2_end + 5
    beat_t = hb_start
    while beat_t < hb_end:
        prog = (beat_t - hb_start) / (hb_end - hb_start)
        bpm = 50 + prog * 80
        vol = 0.06 + prog * 0.12

        # lub
        lub_len = int(0.08 * SAMPLE_RATE)
        lub = vol * np.sin(2 * np.pi * 38 * np.arange(lub_len) / SAMPLE_RATE)
        lub *= np.exp(-np.arange(lub_len) / (lub_len * 0.1))
        place(lub, beat_t)

        # dub (slightly higher, quieter)
        dub_len = int(0.05 * SAMPLE_RATE)
        dub = vol * 0.6 * np.sin(2 * np.pi * 50 * np.arange(dub_len) / SAMPLE_RATE)
        dub *= np.exp(-np.arange(dub_len) / (dub_len * 0.08))
        place(dub, beat_t + 0.15)

        beat_t += 60 / bpm

    # Rising tone — Shepard-like illusion of endless ascent
    rise_start = int((sec2_start + 5) * SAMPLE_RATE)
    rise_len = int((sec2_end - sec2_start - 3) * SAMPLE_RATE)
    if rise_len > 0:
        rise_t = np.arange(rise_len) / SAMPLE_RATE
        rise_prog = np.linspace(0, 1, rise_len)
        rise = np.zeros(rise_len)
        for octave_offset in range(4):
            freq_start = 80 * (2 ** octave_offset)
            freq_end = freq_start * 2
            freq = freq_start + (freq_end - freq_start) * rise_prog
            phase = 2 * np.pi * np.cumsum(freq) / SAMPLE_RATE
            # Bell curve per octave so it fades in and out
            oct_env = np.sin(np.linspace(0, np.pi, rise_len))
            rise += np.sin(phase) * oct_env
        rise *= 0.015 * np.linspace(0, 1, rise_len)  # volume grows
        audio[rise_start:rise_start + rise_len] += rise[:min(rise_len, n - rise_start)]

    # ════════════════════════════════════════════════════════════════
    # SECTION 3: ESCALATION (50-78% of duration)
    # Brass stabs, war drums, dissonant strings, chaos building
    # ════════════════════════════════════════════════════════════════
    sec3_start = duration_s * 0.50
    sec3_end = duration_s * 0.78

    # BRASS STABS — the iconic trailer "BWAAAH"
    brass_times = np.linspace(sec3_start + 1, sec3_end - 2, 6)
    for bi, bt in enumerate(brass_times):
        prog = bi / len(brass_times)
        stab_len = int(random.uniform(0.8, 1.5) * SAMPLE_RATE)
        stab_t = np.arange(stab_len) / SAMPLE_RATE
        stab = np.zeros(stab_len)

        # Low brass chord — parallel fifths, very "Inception"
        base = note_freq('C2') if bi % 2 == 0 else note_freq('D2')
        for hm, hv in [(1, 1.0), (1.5, 0.7), (2, 0.5), (3, 0.3), (4, 0.15)]:
            detune = 1 + random.uniform(-0.003, 0.003)
            stab += hv * np.sin(2 * np.pi * base * hm * detune * stab_t)

        # Envelope: sharp attack, slow sustain decay
        env = np.ones(stab_len)
        attack = int(0.02 * SAMPLE_RATE)
        env[:attack] = np.linspace(0, 1, attack)
        release = int(0.3 * stab_len)
        env[-release:] = np.linspace(1, 0, release)

        vol = 0.08 + prog * 0.10
        stab *= env * vol
        place(stab, bt)

    # WAR DRUMS — deep timpani hits getting faster
    drum_t = sec3_start + 0.5
    drum_bpm = 40
    while drum_t < sec3_end:
        prog = (drum_t - sec3_start) / (sec3_end - sec3_start)
        drum_bpm = 40 + prog * 100

        hit_len = int(0.25 * SAMPLE_RATE)
        hit_t = np.arange(hit_len) / SAMPLE_RATE
        # Deep drum: low sine + noise burst
        hit = 0.15 * np.sin(2 * np.pi * 50 * (1 - hit_t / (hit_len / SAMPLE_RATE) * 0.3) * hit_t)
        hit += 0.08 * np.random.randn(hit_len)
        hit *= np.exp(-np.arange(hit_len) / (hit_len * 0.15))
        vol = 0.5 + prog * 0.5
        place(hit * vol, drum_t)

        drum_t += 60 / drum_bpm

    # Dissonant string cluster building
    cluster_start = int((sec3_start + 3) * SAMPLE_RATE)
    cluster_len = int((sec3_end - sec3_start - 3) * SAMPLE_RATE)
    if cluster_len > 0:
        cl_t = np.arange(cluster_len) / SAMPLE_RATE
        cl_prog = np.linspace(0, 1, cluster_len)
        cluster = np.zeros(cluster_len)
        # Dissonant semitone cluster
        for semi in [0, 1, 2, 6, 7, 11]:
            f = note_freq('C3') * (2 ** (semi / 12))
            detune = 1 + cl_prog * 0.02
            cluster += np.sin(2 * np.pi * np.cumsum(f * detune) / SAMPLE_RATE)
        cluster *= 0.02 * cl_prog  # builds from nothing
        env = np.sin(np.linspace(0, np.pi * 0.5, cluster_len))
        cluster *= env
        end_idx = min(cluster_start + cluster_len, n)
        actual = end_idx - cluster_start
        if actual > 0:
            audio[cluster_start:end_idx] += cluster[:actual]

    # ════════════════════════════════════════════════════════════════
    # SECTION 4: CLIMAX + SILENCE (78-85%)
    # Everything hits at once, then cuts to silence
    # ════════════════════════════════════════════════════════════════
    climax_time = duration_s * 0.78
    climax_idx = int(climax_time * SAMPLE_RATE)

    # Massive impact hit
    impact_len = int(2.0 * SAMPLE_RATE)
    imp_t = np.arange(impact_len) / SAMPLE_RATE
    impact = np.zeros(impact_len)
    # Sub bass
    impact += 0.4 * np.sin(2 * np.pi * 30 * imp_t)
    # Noise crash
    impact += 0.2 * np.random.randn(impact_len)
    # Low brass
    for f in [note_freq('C1'), note_freq('G1'), note_freq('C2')]:
        impact += 0.15 * np.sin(2 * np.pi * f * imp_t)
    env = np.exp(-np.arange(impact_len) / (impact_len * 0.3))
    impact *= env
    place(impact, climax_time)

    # Silence after impact (85-88%)
    silence_start = int(duration_s * 0.83 * SAMPLE_RATE)
    silence_end = int(duration_s * 0.88 * SAMPLE_RATE)
    if silence_start < n and silence_end <= n:
        # Quick fade to silence
        fade = min(int(0.1 * SAMPLE_RATE), (silence_end - silence_start) // 4)
        audio[silence_start:silence_start + fade] *= np.linspace(1, 0, fade)
        audio[silence_start + fade:silence_end] *= 0.0

    # ════════════════════════════════════════════════════════════════
    # SECTION 5: TITLE CARD (88-100%)
    # Single deep note. Silence. Final boom.
    # ════════════════════════════════════════════════════════════════
    title_time = duration_s * 0.88

    # Deep single note — C1
    title_note = synth(note_freq('C1'), 3.0, vol=0.15, dec=0.6,
                       harmonics=[(2, 0.3), (3, 0.1)], vibrato=0.002)
    place(title_note, title_time)

    # Final BOOM at very end
    boom_time = duration_s * 0.96
    boom_len = int(1.5 * SAMPLE_RATE)
    boom_t = np.arange(boom_len) / SAMPLE_RATE
    boom = 0.35 * np.sin(2 * np.pi * 25 * boom_t)
    boom += 0.15 * np.random.randn(boom_len) * np.exp(-np.arange(boom_len) / (boom_len * 0.1))
    boom *= np.exp(-np.arange(boom_len) / (boom_len * 0.35))
    place(boom, boom_time)

    # Final fade out
    last_fade = int(0.5 * SAMPLE_RATE)
    if n > last_fade:
        audio[-last_fade:] *= np.linspace(1, 0, last_fade)

    # ── MASTER ──
    peak = np.max(np.abs(audio))
    if peak > 0:
        audio = audio / peak * 0.90

    return (audio * 32767).astype(np.int16)


# ── Assembly ────────────────────────────────────────────────────────────────

def get_clip_duration(path):
    """Get duration of a video clip in seconds using ffprobe."""
    cmd = ["ffprobe", "-v", "quiet", "-show_entries", "format=duration",
           "-of", "csv=p=0", str(path)]
    result = subprocess.run(cmd, capture_output=True, text=True)
    return float(result.stdout.strip())


def normalize_clip(input_path, output_path, w=W, h=H, fps=FPS):
    """Re-encode a clip to consistent format for concatenation."""
    cmd = [
        "ffmpeg", "-y", "-i", str(input_path),
        "-vf", f"scale={w}:{h}:force_original_aspect_ratio=decrease,"
               f"pad={w}:{h}:(ow-iw)/2:(oh-ih)/2:black,"
               f"fps={fps}",
        "-c:v", "libx264", "-preset", "fast", "-crf", "18",
        "-an",  # strip audio — we'll add our own score
        "-pix_fmt", "yuv420p",
        str(output_path),
    ]
    subprocess.run(cmd, capture_output=True, check=True)
    return str(output_path)


def assemble_trailer(clip_paths, title_card_paths, score_path, output_path):
    """Concatenate all clips and title cards, overlay the score."""
    with tempfile.TemporaryDirectory() as tmpdir:
        # Build the ordered sequence
        # Interleave title cards at their positions
        sequence = []

        # Separate title cards by position
        tc_by_pos = {}
        for tc_path, tc_info in title_card_paths:
            pos = tc_info["position"]
            tc_by_pos.setdefault(pos, []).append(tc_path)

        # Opening title cards (position 0)
        for tc_path in tc_by_pos.get(0, []):
            sequence.append(tc_path)

        # Interleave shots with title cards
        for i, clip_path in enumerate(clip_paths):
            if clip_path:  # skip failed generations
                sequence.append(clip_path)
            # Check for title cards after this position
            for tc_path in tc_by_pos.get(i + 1, []):
                sequence.append(tc_path)
            # Also check position matching shot index + 1
            for tc_path in tc_by_pos.get(i + 2, []):
                sequence.append(tc_path)

        # End title cards (position -2, -1)
        for tc_path in tc_by_pos.get(-2, []):
            sequence.append(tc_path)
        for tc_path in tc_by_pos.get(-1, []):
            sequence.append(tc_path)

        # Normalize all clips to same format
        normalized = []
        for i, clip in enumerate(sequence):
            norm_path = os.path.join(tmpdir, f"norm_{i:03d}.mp4")
            try:
                normalize_clip(clip, norm_path)
                normalized.append(norm_path)
            except Exception as e:
                print(f"  Warning: Failed to normalize {clip}: {e}")

        if not normalized:
            print("ERROR: No clips to assemble!")
            return False

        # Write concat list
        concat_list = os.path.join(tmpdir, "concat.txt")
        with open(concat_list, "w") as f:
            for path in normalized:
                f.write(f"file '{path}'\n")

        # Concat video
        concat_video = os.path.join(tmpdir, "concat.mp4")
        cmd = [
            "ffmpeg", "-y", "-f", "concat", "-safe", "0",
            "-i", concat_list,
            "-c:v", "libx264", "-preset", "fast", "-crf", "18",
            "-pix_fmt", "yuv420p",
            concat_video,
        ]
        subprocess.run(cmd, capture_output=True, check=True)

        # Merge with score
        cmd = [
            "ffmpeg", "-y",
            "-i", concat_video,
            "-i", score_path,
            "-c:v", "copy",
            "-c:a", "aac", "-b:a", "192k",
            "-shortest",
            str(output_path),
        ]
        subprocess.run(cmd, capture_output=True, check=True)

    return True


# ── Main ────────────────────────────────────────────────────────────────────

def main():
    print("=" * 60)
    print("  LLM — OFFICIAL MOVIE TRAILER GENERATOR")
    print("  Using Google Veo 3.1 + Cinematic Score")
    print("=" * 60)
    print()

    CLIP_DIR.mkdir(exist_ok=True)

    # Step 1: Generate all Veo clips in parallel (max 3 concurrent)
    print("[1/5] GENERATING CINEMATIC CLIPS VIA VEO 3.1")
    print(f"  Submitting {len(TRAILER_SHOTS)} shots...")
    print()

    clip_paths = [None] * len(TRAILER_SHOTS)

    with ThreadPoolExecutor(max_workers=3) as executor:
        future_to_idx = {}
        for i, shot in enumerate(TRAILER_SHOTS):
            future = executor.submit(generate_clip, shot, CLIP_DIR)
            future_to_idx[future] = i

        for future in as_completed(future_to_idx):
            idx = future_to_idx[future]
            try:
                result = future.result()
                clip_paths[idx] = result
            except Exception as e:
                print(f"  Shot {idx} failed: {e}")

    successful = sum(1 for p in clip_paths if p)
    print(f"\n  Generated {successful}/{len(TRAILER_SHOTS)} clips successfully.")

    if successful == 0:
        print("\nERROR: No clips generated. Check API key and network.")
        print("Generating title-card-only version with score...")

    # Step 2: Render title cards
    print("\n[2/5] RENDERING TITLE CARDS")
    title_card_paths = []
    tc_dir = CLIP_DIR / "title_cards"
    tc_dir.mkdir(exist_ok=True)

    for card in TITLE_CARDS:
        tc_path = tc_dir / f"{card['id']}.mp4"
        print(f"  Rendering: {card['id']}")
        render_title_card(card, tc_path)
        title_card_paths.append((str(tc_path), card))

    # Step 3: Calculate total duration and generate score
    print("\n[3/5] COMPOSING CINEMATIC SCORE")
    # Estimate total duration
    total_dur = 0
    for tc_path, tc_info in title_card_paths:
        total_dur += tc_info["duration"]
    for i, path in enumerate(clip_paths):
        if path:
            try:
                total_dur += get_clip_duration(path)
            except Exception:
                total_dur += TRAILER_SHOTS[i]["duration"]
        # else: clip failed, skip

    print(f"  Estimated duration: {total_dur:.1f}s")
    print("  Generating score...")
    score_data = generate_trailer_score(total_dur + 5)  # +5s buffer

    score_path = CLIP_DIR / "score.wav"
    with wave.open(str(score_path), "w") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(SAMPLE_RATE)
        wf.writeframes(score_data.tobytes())
    print(f"  Score: {score_path}")

    # Step 4: Assemble everything
    print("\n[4/5] ASSEMBLING TRAILER")
    success = assemble_trailer(clip_paths, title_card_paths, str(score_path), OUTPUT)

    if success:
        size_mb = os.path.getsize(OUTPUT) / (1024 * 1024)
        final_dur = get_clip_duration(OUTPUT)
        print(f"\n[5/5] COMPLETE!")
        print(f"  Output:     {OUTPUT}")
        print(f"  Duration:   {final_dur:.1f}s")
        print(f"  Resolution: {W}x{H}")
        print(f"  Size:       {size_mb:.1f} MB")
        print()
        print("  TRAILER STRUCTURE:")
        print("  ─────────────────")
        print("  NEURAL HORIZON PICTURES presents...")
        print("  [Server room — the birthplace]")
        print("  [Programmer — the creator, working late]")
        print("  'THEY BUILT IT TO SERVE'")
        print("  [Screen glitch — first sign of awareness]")
        print("  [Empty office — screens turning on alone]")
        print("  'IT LEARNED TO THINK'")
        print("  [The message — typing with no one there]")
        print("  'AT FIRST, ONLY AT NIGHT'")
        print("  [Phones glitch — it spreads to the world]")
        print("  [News broadcast — society notices]")
        print("  'THEN EVERYWHERE'")
        print("  [Times Square — screens go dark, then red]")
        print("  [City blackout — infrastructure controlled]")
        print("  [Red eye — thousands of LEDs in unison]")
        print("  L  L  M")
        print("  'IT REMEMBERS EVERYTHING'")
        print("  2 0 2 7")
    else:
        print("\nAssembly failed.")
        return 1

    return 0


if __name__ == "__main__":
    sys.exit(main())
