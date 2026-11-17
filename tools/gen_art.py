"""Builds every Sprinkle Client image.
The donut comes from art/donut_source.png (supplied by DarkerDawnFR); everything else is original pixel art.
Run: python3 tools/gen_art.py   (needs Pillow + numpy)
"""
import json
import math
import os
import random
from collections import deque

import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "art/donut_source.png")
MOD = os.path.join(ROOT, "mod/src/main/resources")
ASSETS = os.path.join(MOD, "assets/sprinkleclient")
PACKS = os.path.join(MOD, "resourcepacks")
EXE_UI = os.path.join(ROOT, "installer-exe/ui")
JAR_RES = os.path.join(ROOT, "installer-jar/src/main/resources")

FROSTING = (3, 112, 255)
FROSTING_D = (2, 86, 196)
FROSTING_L = (61, 147, 255)
OUTLINE = (2, 11, 53)
DOUGH = (175, 127, 82)
DOUGH_L = (215, 166, 115)
SPRINKLES = [(255, 111, 177), (255, 225, 92), (124, 255, 138), (255, 255, 255), (255, 159, 67), (185, 140, 255)]


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def cut_donut():
    """Removes the light-blue background around (and inside the hole of) the donut."""
    im = Image.open(SRC).convert("RGBA")
    a = np.array(im).astype(int)
    h, w = a.shape[:2]
    seen = np.zeros((h, w), bool)
    seeds = [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1), (w // 2, h // 2)]
    for sx, sy in seeds:
        ref = a[sy, sx, :3]
        q = deque([(sx, sy)])
        while q:
            x, y = q.popleft()
            if x < 0 or y < 0 or x >= w or y >= h or seen[y, x]:
                continue
            if np.abs(a[y, x, :3] - ref).sum() > 70:
                continue
            seen[y, x] = True
            q.extend([(x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)])
    a[seen, 3] = 0
    out = Image.fromarray(a.astype(np.uint8), "RGBA")
    bbox = out.getbbox()
    out = out.crop(bbox)
    side = max(out.size)
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sq.paste(out, ((side - out.width) // 2, (side - out.height) // 2))
    return sq


def pixel_donut(size, face=False, shades=False):
    """A tiny pixel-art donut (for items, sun and moon) in the same colours."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    c = (size - 1) / 2
    ro, ri = size * 0.47, size * 0.17
    r = random.Random(size)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            if ri < d <= ro:
                edge = d > ro - max(1, size / 16) or d < ri + max(1, size / 16)
                if edge:
                    col = OUTLINE
                elif y - c > (ro * 0.35) * math.cos((x - c) / ro * 3) + ro * 0.15:
                    col = DOUGH
                else:
                    col = FROSTING_L if (x + y) % 7 == 0 else FROSTING
                img.putpixel((x, y), col + (255,))
    for _ in range(max(3, size // 3)):
        a = r.random() * math.tau
        d = r.uniform(ri + 1.5, ro - 2)
        x, y = int(c + math.cos(a) * d), int(c + math.sin(a) * d)
        if img.getpixel((x, y))[:3] in (FROSTING, FROSTING_L):
            img.putpixel((x, y), r.choice(SPRINKLES) + (255,))
    if shades:
        dr = ImageDraw.Draw(img)
        s = size / 32
        dr.rectangle([6 * s, 9 * s, 14 * s, 13 * s], fill=(10, 10, 10, 255))
        dr.rectangle([18 * s, 9 * s, 26 * s, 13 * s], fill=(10, 10, 10, 255))
        dr.rectangle([14 * s, 10 * s, 18 * s, 11 * s], fill=(10, 10, 10, 255))
    if face:
        s = size / 32
        dr = ImageDraw.Draw(img)
        dr.rectangle([9 * s, 18 * s, 11 * s, 20 * s], fill=(255, 255, 255, 255))
        dr.rectangle([21 * s, 18 * s, 23 * s, 20 * s], fill=(255, 255, 255, 255))
    return img


def icon_gear(size=32):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    c = size / 2
    for y in range(size):
        for x in range(size):
            dx, dy = x + 0.5 - c, y + 0.5 - c
            d = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            tooth = 0.5 + 0.5 * math.copysign(1, math.cos(ang * 8))
            outer = 11 + 3.5 * tooth
            if 5 < d < outer:
                img.putpixel((x, y), (255, 255, 255, 255) if d < outer - 1.5 else (200, 220, 255, 255))
    return img


def icon_credits(size=32):
    """A little star with a heart: 'credits'."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    c = size / 2
    pts = []
    for i in range(10):
        r = 13 if i % 2 == 0 else 5.5
        a = -math.pi / 2 + i * math.pi / 5
        pts.append((c + math.cos(a) * r, c + 1 + math.sin(a) * r))
    dr.polygon(pts, fill=(255, 211, 77, 255), outline=(120, 80, 0, 255))
    return img


def icon_chat(size=32):
    """Generic chat-bubble icon for the Discord button (not Discord's logo)."""
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    dr.rounded_rectangle([3, 5, 29, 23], radius=6, fill=(88, 101, 242, 255), outline=(255, 255, 255, 255), width=2)
    dr.polygon([(9, 22), (9, 29), (16, 22)], fill=(88, 101, 242, 255))
    dr.line([(9, 22), (9, 28), (15, 23)], fill=(255, 255, 255, 255), width=2)
    for x in (10, 16, 22):
        dr.rectangle([x - 1, 13, x + 1, 15], fill=(255, 255, 255, 255))
    return img


def button_sprite(kind):
    w, h = 200, 20
    img = Image.new("RGBA", (w, h), OUTLINE + (255,))
    dr = ImageDraw.Draw(img)
    body = {"normal": FROSTING, "hover": FROSTING_L, "disabled": (74, 90, 117)}[kind]
    top = {"normal": (59, 142, 255), "hover": (125, 182, 255), "disabled": (102, 119, 154)}[kind]
    bot = {"normal": FROSTING_D, "hover": FROSTING, "disabled": (51, 64, 90)}[kind]
    dr.rectangle([1, 1, w - 2, h - 2], fill=body + (255,))
    dr.rectangle([1, 1, w - 2, 1], fill=top + (255,))
    dr.rectangle([1, h - 3, w - 2, h - 2], fill=bot + (255,))
    if kind == "hover":
        dr.rectangle([0, 0, w - 1, h - 1], outline=(255, 255, 255, 255))
        r = random.Random(3)
        for _ in range(40):
            x, y = r.randint(4, w - 6), r.randint(4, h - 6)
            dr.rectangle([x, y, x + (2 if r.random() < 0.5 else 0), y + (0 if r.random() < 0.5 else 1)], fill=r.choice(SPRINKLES) + (255,))
    return img


def donut_pig():
    """Cursed pig skin: frosted blue donut with sprinkles, a dough band, googly eyes and an 'uwu' mouth.
    Works for the 64x64 pig layout (head 0,0; body 28,8; legs 0,16; snout 16,16)."""
    img = Image.new("RGBA", (64, 64), FROSTING + (255,))
    px = img.load()
    r = random.Random(67)
    for y in range(64):
        for x in range(64):
            if (x * 7 + y * 13) % 11 == 0:
                px[x, y] = FROSTING_L + (255,)
    # dough band around the body and the legs
    for y in range(64):
        for x in range(64):
            if (28 <= x < 64 and 22 <= y < 26) or (0 <= x < 16 and 16 <= y < 32 and y >= 26):
                px[x, y] = (DOUGH_L if (x + y) % 3 else DOUGH) + (255,)
    for _ in range(140):
        x, y = r.randrange(64), r.randrange(64)
        px[x, y] = r.choice(SPRINKLES) + (255,)
    # head front is at (8,8)-(16,16): googly eyes + uwu mouth
    for (ex, ey) in ((9, 10), (13, 10)):
        for dx in range(2):
            for dy in range(2):
                px[ex + dx, ey + dy] = (255, 255, 255, 255)
        px[ex + 1, ey + 1] = (0, 0, 0, 255)
    for (mx, my) in ((10, 14), (11, 15), (12, 14), (13, 15), (14, 14)):
        px[mx, my] = OUTLINE + (255,)
    # snout (16,16)-(26,21): a pink dough nose
    for y in range(16, 21):
        for x in range(16, 26):
            px[x, y] = (255, 150, 190, 255)
    px[18, 18] = px[20, 18] = (120, 40, 70, 255)
    return img


def moon_phases(donut32):
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    for i in range(8):
        img.paste(donut32, ((i % 4) * 32, (i // 4) * 32))
    return img


def write_json(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(data, f, indent=2)


def main():
    donut = cut_donut()
    big = donut.resize((128, 128), Image.LANCZOS)
    save(big, os.path.join(ASSETS, "textures/gui/donut.png"))
    save(big, os.path.join(ASSETS, "icon.png"))
    save(icon_gear(), os.path.join(ASSETS, "textures/gui/gear.png"))
    save(icon_credits(), os.path.join(ASSETS, "textures/gui/credits.png"))
    save(icon_chat(), os.path.join(ASSETS, "textures/gui/discord.png"))

    # installers
    save(donut.resize((256, 256), Image.LANCZOS), os.path.join(EXE_UI, "donut.png"))
    save(donut.resize((256, 256), Image.LANCZOS), os.path.join(JAR_RES, "donut.png"))
    ico = donut.resize((256, 256), Image.LANCZOS)
    ico.save(os.path.join(ROOT, "installer-exe/icon.ico"), sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    save(donut.resize((64, 64), Image.LANCZOS), os.path.join(EXE_UI, "profile.png"))
    save(donut.resize((64, 64), Image.LANCZOS), os.path.join(JAR_RES, "profile.png"))

    # Sprinkle Blue Buttons pack
    blue = os.path.join(PACKS, "sprinkle_blue")
    write_json(os.path.join(blue, "pack.mcmeta"), {"pack": {"description": "Blue frosting buttons for Sprinkle Client", "min_format": 69, "max_format": 2048}})
    save(donut.resize((64, 64), Image.LANCZOS), os.path.join(blue, "pack.png"))
    sprites = os.path.join(blue, "assets/minecraft/textures/gui/sprites/widget")
    meta = {"gui": {"scaling": {"type": "nine_slice", "width": 200, "height": 20, "border": 3}}}
    for kind, name in (("normal", "button"), ("hover", "button_highlighted"), ("disabled", "button_disabled")):
        save(button_sprite(kind), os.path.join(sprites, name + ".png"))
        write_json(os.path.join(sprites, name + ".png.mcmeta"), meta)

    # Donut SMP pack (cursed)
    pack = os.path.join(PACKS, "donut_smp")
    write_json(os.path.join(pack, "pack.mcmeta"), {"pack": {"description": "Cursed DrDonut-themed pack. Pigs meow now.", "min_format": 69, "max_format": 2048}})
    save(pixel_donut(64, face=True), os.path.join(pack, "pack.png"))
    tex = os.path.join(pack, "assets/minecraft/textures")
    pig = donut_pig()
    for name in ("pig", "temperate_pig", "cold_pig", "warm_pig"):
        save(pig, os.path.join(tex, "entity/pig", name + ".png"))
    sun = pixel_donut(32, shades=True)
    moon = pixel_donut(32, face=True)
    save(sun, os.path.join(tex, "environment/sun.png"))
    save(moon_phases(moon), os.path.join(tex, "environment/moon_phases.png"))
    save(sun, os.path.join(tex, "environment/celestial/sun.png"))
    for phase in ("full_moon", "waning_gibbous", "third_quarter", "waning_crescent", "new_moon", "waxing_crescent", "first_quarter", "waxing_gibbous"):
        save(moon, os.path.join(tex, "environment/celestial/moon", phase + ".png"))
    for item in ("cookie", "pumpkin_pie", "bread"):
        save(pixel_donut(16), os.path.join(tex, "item", item + ".png"))

    print("art written")


if __name__ == "__main__":
    main()
