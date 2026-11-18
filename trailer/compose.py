"""Cuts the Sprinkle Client trailer.
Usage: python3 compose.py <frames dir> <screenshots dir + client.out.txt> <music.ogg> <out.mp4>
Blender frames: <frames>/<scene>_0001.png ...   Game shots are matched by name from the test log.
"""
import os
import re
import subprocess
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

W, H, FPS = 1280, 720, 30
FRAMES, SHOTS, MUSIC, OUT = sys.argv[1:5]
TMP = os.path.join(os.path.dirname(os.path.abspath(OUT)), "parts")
os.makedirs(TMP, exist_ok=True)

FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_R = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
BLUE = (3, 112, 255)
INK = (2, 11, 53)


def font(size, bold=True):
    return ImageFont.truetype(FONT_B if bold else FONT_R, size)


# ------------------------------------------------------------------ inputs

def shot_files():
    """Maps the test's screenshot names ('main menu', 'pause menu'...) to files."""
    log = open(os.path.join(SHOTS, "client.out.txt"), errors="ignore").read()
    files = sorted(f for f in os.listdir(SHOTS) if f.endswith((".jpg", ".png")))
    by_time = {}
    for f in files:
        m = re.match(r"\d{4}-\d\d-\d\d_(\d\d)\.(\d\d)\.(\d\d)", f)
        if m:
            by_time.setdefault(":".join(m.groups()), []).append(f)
    out = {}
    for m in re.finditer(r"\[(\d\d:\d\d:\d\d)\].*?screenshot (.+)", log):
        t, name = m.group(1), m.group(2).strip()
        if by_time.get(t):
            out[name] = os.path.join(SHOTS, by_time[t].pop(0))
    return out


SHOT = shot_files()


def blender(scene):
    fs = sorted(f for f in os.listdir(FRAMES) if f.startswith(scene + "_"))
    return [os.path.join(FRAMES, f) for f in fs]


# ------------------------------------------------------------------ drawing

def ease(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def caption(img, text, sub, t, dur):
    """Lower-left caption with a frosting bar; slides in, fades out."""
    a_in = ease(t / 0.4)
    a_out = ease((dur - t) / 0.35)
    a = min(a_in, a_out)
    if a <= 0:
        return img
    layer = Image.new("RGBA", img.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    f1, f2 = font(40), font(22, False)
    x = 64 - int((1 - a_in) * 30)
    y = H - 150
    tw = d.textlength(text, font=f1)
    sw = d.textlength(sub, font=f2) if sub else 0
    box_w = int(max(tw, sw)) + 36
    d.rectangle([x - 18, y - 14, x - 18 + box_w, y + (92 if sub else 58)], fill=(2, 11, 53, int(170 * a)))
    d.rectangle([x - 18, y - 14, x - 12, y + (92 if sub else 58)], fill=BLUE + (int(255 * a),))
    d.text((x, y), text, font=f1, fill=(255, 255, 255, int(255 * a)))
    if sub:
        d.text((x, y + 52), sub, font=f2, fill=(185, 214, 245, int(255 * a)))
    return Image.alpha_composite(img.convert("RGBA"), layer)


def cover(img, zoom, pan):
    """Scales an image to cover the frame with a slow push-in (sub-pixel smooth)."""
    iw, ih = img.size
    s = max(W / iw, H / ih) * zoom
    cw, ch = W / s, H / s
    x0 = (iw - cw) * pan[0]
    y0 = (ih - ch) * pan[1]
    return img.transform((W, H), Image.Transform.EXTENT, (x0, y0, x0 + cw, y0 + ch), Image.Resampling.BICUBIC)


def title_card(t, dur, lines):
    img = Image.new("RGB", (W, H), INK)
    d = ImageDraw.Draw(img)
    a = min(ease(t / 0.35), ease((dur - t) / 0.35))
    y = H // 2 - 30 * len(lines)
    for text, size, col in lines:
        f = font(size)
        tw = d.textlength(text, font=f)
        c = tuple(int(col[i] * a + INK[i] * (1 - a)) for i in range(3))
        d.text(((W - tw) / 2, y), text, font=f, fill=c)
        y += size + 22
    return img


# ------------------------------------------------------------------ encoding

class Writer:
    def __init__(self, name):
        self.path = os.path.join(TMP, name + ".mp4")
        self.tmp = self.path + ".part.mp4"
        self.p = subprocess.Popen(["ffmpeg", "-loglevel", "error", "-y", "-f", "rawvideo", "-pix_fmt", "rgb24",
                                   "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-", "-c:v", "libx264", "-preset", "veryfast",
                                   "-crf", "18", "-pix_fmt", "yuv420p", "-r", str(FPS), self.tmp], stdin=subprocess.PIPE)

    def add(self, img):
        self.p.stdin.write(img.convert("RGB").tobytes())

    def close(self):
        self.p.stdin.close()
        if self.p.wait() == 0:
            os.replace(self.tmp, self.path)
        return self.path


def fade(img, t, dur, fin=0.25, fout=0.25):
    a = min(1, t / fin if fin else 1, (dur - t) / fout if fout else 1)
    if a >= 1:
        return img
    black = Image.new("RGB", (W, H), (0, 0, 0))
    return Image.blend(black, img.convert("RGB"), max(0, a))


def scene_blender(name, frames, cap=None, sub=None, overlay=None, fin=0.0, fout=0.25):
    wr = Writer(name)
    dur = len(frames) / FPS
    for i, f in enumerate(frames):
        t = i / FPS
        img = Image.open(f).convert("RGB").resize((W, H), Image.Resampling.LANCZOS)
        if overlay:
            img = overlay(img, t, dur)
        if cap:
            img = caption(img, cap, sub, t, dur)
        wr.add(fade(img, t, dur, fin, fout))
    return wr.close()


def scene_shot(name, key, dur, cap, sub=None, pan=(0.5, 0.5), zoom=(1.0, 1.08)):
    src = Image.open(SHOT[key]).convert("RGB")
    wr = Writer(name)
    n = int(dur * FPS)
    for i in range(n):
        t = i / FPS
        z = zoom[0] + (zoom[1] - zoom[0]) * ease(t / dur)
        img = cover(src, z, pan)
        img = caption(img, cap, sub, t, dur)
        wr.add(fade(img, t, dur, 0.2, 0.2))
    return wr.close()


def scene_card(name, dur, lines):
    wr = Writer(name)
    for i in range(int(dur * FPS)):
        wr.add(title_card(i / FPS, dur, lines))
    return wr.close()


def logo_overlay(img, t, dur):
    """Big title over the drop scene once the donut lands."""
    a = ease((t - 2.2) / 0.6)
    if a <= 0:
        return img
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    f = font(92)
    text = "SPRINKLE CLIENT"
    tw = d.textlength(text, font=f)
    x, y = (W - tw) / 2, H - 250 + (1 - a) * 20
    d.rectangle([0, H - 290, W, H], fill=(2, 11, 53, int(150 * a)))
    d.text((x + 5, y + 6), text, font=f, fill=INK + (int(200 * a),))
    d.text((x, y), text, font=f, fill=(255, 255, 255, int(255 * a)))
    f2 = font(26, False)
    s = "a Donut SMP client"
    d.text(((W - d.textlength(s, font=f2)) / 2, y + 112), s, font=f2, fill=(230, 240, 255, int(255 * a)))
    return Image.alpha_composite(img.convert("RGBA"), layer)


def end_overlay(img, t, dur):
    a = ease((t - 0.8) / 0.8)
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.rectangle([0, H - 210, W, H], fill=(2, 11, 53, int(200 * a)))
    rows = [("SPRINKLE CLIENT", font(56), (255, 255, 255)),
            ("Free  ·  Minecraft 1.21 – 1.21.11  ·  Fabric", font(24, False), (185, 214, 245)),
            ("github.com/luaslucas422-lab/Sprinkle-Client", font(24), (255, 211, 77)),
            ("Made by DarkerDawnFR   ·   Fan-made, not affiliated with Donut SMP", font(16, False), (140, 170, 210))]
    y = H - 190
    for text, f, col in rows:
        d.text(((W - d.textlength(text, font=f)) / 2, y), text, font=f, fill=col + (int(255 * a),))
        y += f.size + 14
    return Image.alpha_composite(img.convert("RGBA"), layer)


# ------------------------------------------------------------------ the cut

parts = []
parts.append(scene_blender("01_drop", blender("drop"), overlay=logo_overlay, fin=0.4))
parts.append(scene_shot("02_menu", "main menu", 3.4, "Every button goes to Donut SMP.", "Even Quit. It's called Rage Quit now.", pan=(0.5, 0.65)))
parts.append(scene_shot("03_hud", "in-game hud with fake scoreboard and donut pigs", 3.4, "Your money, combat tag and totem pops.", "Right on your screen.", pan=(0.2, 0.2)))
parts.append(scene_shot("04_board", "fake scoreboard editor", 3.2, "Make up your own scoreboard.", "It always says the stats aren't real.", pan=(0.8, 0.4)))
parts.append(scene_blender("05_spawners", blender("spawners"), "Know what your spawners are worth.", None))
parts.append(scene_shot("06_shulker", "shulker preview tooltip", 3.0, "Hold Shift to look inside shulkers.", None, pan=(0.5, 0.45), zoom=(1.15, 1.25)))
parts.append(scene_blender("07_pig", blender("pig"), "Pigs meow now.", "We're not sorry.", fin=0.2))
parts.append(scene_shot("08_sky", "sky changer", 3.0, "Any picture as your sky.", None, pan=(0.5, 0.3)))
parts.append(scene_shot("09_soundboard", "soundboard", 3.0, "A Donut SMP soundboard.", "Plays through Simple Voice Chat.", pan=(0.5, 0.5)))
parts.append(scene_shot("10_mods", "donut mods menu", 3.2, "Thirteen Donut mods. One menu.", "Right Shift.", pan=(0.5, 0.5)))
parts.append(scene_blender("11_outro", blender("outro"), overlay=end_overlay, fin=0.3, fout=0.6))

lst = os.path.join(TMP, "list.txt")
with open(lst, "w") as f:
    for p in parts:
        f.write(f"file '{os.path.abspath(p)}'\n")
silent = os.path.join(TMP, "silent.mp4")
subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-f", "concat", "-safe", "0", "-i", lst, "-c", "copy", silent], check=True)
dur = float(subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "csv=p=0", silent],
                           capture_output=True, text=True).stdout)
tmp = OUT + ".part.mp4"
subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", silent, "-stream_loop", "-1", "-i", MUSIC,
                "-map", "0:v", "-map", "1:a", "-c:v", "copy", "-c:a", "aac", "-b:a", "192k",
                "-af", f"afade=t=in:d=0.8,afade=t=out:st={dur - 2.0}:d=2.0", "-t", f"{dur}", tmp], check=True)
os.replace(tmp, OUT)
print("trailer:", OUT, round(dur, 1), "s")
