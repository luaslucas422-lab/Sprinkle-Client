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


# ------------------------------------------------------------------ Simple Voice Chat theme (all original art)

RED = (235, 40, 40, 255)
WHITE = (255, 255, 255, 255)


def _canvas(n=16):
    return Image.new("RGBA", (n, n), (0, 0, 0, 0))


def mic_icon(small=False):
    im = _canvas()
    d = ImageDraw.Draw(im)
    if small:
        d.rounded_rectangle([6, 3, 9, 9], radius=2, fill=FROSTING + (255,), outline=OUTLINE + (255,))
        d.line([(5, 8), (5, 10), (10, 10), (10, 8)], fill=DOUGH + (255,))
        d.line([(7, 11), (7, 12)], fill=DOUGH + (255,))
        return im
    d.rounded_rectangle([5, 1, 10, 10], radius=3, fill=FROSTING + (255,), outline=OUTLINE + (255,))
    for (x, y, c) in ((7, 3, SPRINKLES[0]), (8, 6, SPRINKLES[1]), (6, 8, SPRINKLES[2])):
        im.putpixel((x, y), c + (255,))
    d.line([(3, 7), (3, 10), (5, 12), (10, 12), (12, 10), (12, 7)], fill=DOUGH + (255,), width=1)
    d.line([(7, 13), (8, 13)], fill=DOUGH + (255,))
    d.line([(5, 14), (10, 14)], fill=OUTLINE + (255,))
    return im


def speaker_icon(small=False):
    im = _canvas()
    d = ImageDraw.Draw(im)
    if small:
        d.polygon([(3, 6), (5, 6), (8, 3), (8, 12), (5, 9), (3, 9)], fill=FROSTING + (255,), outline=OUTLINE + (255,))
        return im
    d.polygon([(1, 5), (4, 5), (8, 1), (8, 14), (4, 10), (1, 10)], fill=FROSTING + (255,), outline=OUTLINE + (255,))
    d.arc([6, 4, 12, 11], -60, 60, fill=DOUGH + (255,))
    d.arc([6, 1, 15, 14], -60, 60, fill=DOUGH_L + (255,))
    return im


def slash(im):
    d = ImageDraw.Draw(im)
    d.line([(2, 13), (13, 2)], fill=RED, width=2)
    return im


def forbidden(im):
    d = ImageDraw.Draw(im)
    d.ellipse([0, 0, 15, 15], outline=RED, width=2)
    d.line([(3, 12), (12, 3)], fill=RED, width=2)
    return im


def two_donuts():
    im = _canvas()
    a = pixel_donut(10)
    im.alpha_composite(a, (0, 1))
    im.alpha_composite(a, (6, 5))
    return im


def eye_icon():
    im = _canvas()
    d = ImageDraw.Draw(im)
    d.ellipse([1, 4, 14, 11], fill=WHITE, outline=OUTLINE + (255,))
    d.ellipse([5, 5, 10, 10], fill=FROSTING + (255,))
    d.ellipse([7, 7, 8, 8], fill=OUTLINE + (255,))
    return im


def sheet(cells):
    im = _canvas(32)
    for i, c in enumerate(cells):
        if c is not None:
            im.alpha_composite(c, ((i % 2) * 16, (i // 2) * 16))
    return im


def voicechat_textures(root):
    icons = os.path.join(root, "assets/voicechat/textures/icons")
    gui = os.path.join(root, "assets/voicechat/textures/gui")
    m, sp = mic_icon, speaker_icon
    files = {
        "microphone": m(), "microphone_off": slash(m()), "microphone_forced_off": forbidden(m()), "microphone_whisper": m(True),
        "speaker": sp(), "speaker_off": slash(sp()), "speaker_forced_off": forbidden(sp()), "speaker_whisper": sp(True),
        "speaker_small_off": slash(sp(True)), "test_speaker": sp(), "other_volume": sp(),
        "group": two_donuts(), "invite_button": two_donuts(), "lock": None, "leave": None, "adjust_volumes": None,
        "device_selected": None, "disconnected": None, "talk_outline": None,
    }
    lock = _canvas(); d = ImageDraw.Draw(lock)
    d.arc([4, 1, 11, 9], 180, 360, fill=DOUGH + (255,), width=2); d.rectangle([3, 7, 12, 14], fill=FROSTING + (255,), outline=OUTLINE + (255,))
    lock.putpixel((7, 10), WHITE); lock.putpixel((8, 10), WHITE)
    files["lock"] = lock
    leave = _canvas(); d = ImageDraw.Draw(leave)
    d.rectangle([1, 2, 7, 13], outline=OUTLINE + (255,), fill=FROSTING + (255,)); d.line([(6, 8), (14, 8)], fill=RED, width=2); d.polygon([(11, 5), (14, 8), (11, 11)], fill=RED)
    files["leave"] = leave
    adj = _canvas(); d = ImageDraw.Draw(adj)
    for y, x in ((3, 10), (8, 4), (13, 8)):
        d.line([(1, y), (14, y)], fill=OUTLINE + (255,)); d.rectangle([x - 1, y - 2, x + 1, y + 2], fill=FROSTING + (255,), outline=OUTLINE + (255,))
    files["adjust_volumes"] = adj
    files["device_selected"] = pixel_donut(12)
    disc = _canvas(); d = ImageDraw.Draw(disc)
    d.rectangle([5, 1, 6, 4], fill=OUTLINE + (255,)); d.rectangle([9, 1, 10, 4], fill=OUTLINE + (255,)); d.rounded_rectangle([3, 4, 12, 10], radius=2, fill=FROSTING + (255,), outline=OUTLINE + (255,)); d.line([(7, 10), (7, 14)], fill=DOUGH + (255,), width=2)
    files["disconnected"] = slash(disc)
    outline = _canvas(); d = ImageDraw.Draw(outline)
    d.rectangle([0, 0, 15, 15], outline=FROSTING_L + (255,), width=1)
    outline.putpixel((0, 0), SPRINKLES[0] + (255,)); outline.putpixel((15, 15), SPRINKLES[1] + (255,))
    files["talk_outline"] = outline
    for name, img in files.items():
        save(img, os.path.join(icons, name + ".png"))
    # 32x32 button sheets: [normal, off / forced, -]
    save(sheet([m(), slash(m()), forbidden(m()), None]), os.path.join(icons, "microphone_button.png"))
    save(sheet([sp(), slash(sp()), forbidden(sp()), None]), os.path.join(icons, "speaker_button.png"))
    save(sheet([eye_icon(), slash(eye_icon()), None, None]), os.path.join(icons, "hide_button.png"))
    save(sheet([two_donuts(), slash(two_donuts()), None, None]), os.path.join(icons, "group_hud_button.png"))
    rec_off = _canvas(); ImageDraw.Draw(rec_off).ellipse([2, 2, 13, 13], fill=(40, 50, 80, 255), outline=OUTLINE + (255,))
    rec_on = pixel_donut(14); rec_on2 = _canvas(); rec_on2.alpha_composite(rec_on, (1, 1))
    save(sheet([rec_off, rec_on2, None, None]), os.path.join(icons, "record_button.png"))
    # main voice chat window (195x76 used at the top-left of a 256x256 sheet)
    panel = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(panel)
    d.rectangle([0, 0, 194, 75], fill=OUTLINE + (255,))
    d.rectangle([2, 2, 192, 73], fill=(10, 42, 92, 255))
    d.rectangle([2, 2, 192, 4], fill=FROSTING + (255,))
    d.rectangle([2, 5, 192, 17], fill=(191, 227, 255, 255))  # light band so Voice Chat's dark title reads
    r = random.Random(9)
    for _ in range(50):
        x, y = r.randint(4, 188), r.randint(20, 70)
        d.rectangle([x, y, x + (2 if r.random() < 0.5 else 0), y + (0 if r.random() < 0.5 else 1)], fill=r.choice(SPRINKLES) + (160,))
    save(panel, os.path.join(gui, "gui_voicechat.png"))


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
    write_json(os.path.join(blue, "pack.mcmeta"), {"pack": {"description": "Blue frosting buttons for Sprinkle Client", "pack_format": 34, "supported_formats": [34, 2048], "min_format": 34, "max_format": 2048}})
    save(donut.resize((64, 64), Image.LANCZOS), os.path.join(blue, "pack.png"))
    sprites = os.path.join(blue, "assets/minecraft/textures/gui/sprites/widget")
    meta = {"gui": {"scaling": {"type": "nine_slice", "width": 200, "height": 20, "border": 3}}}
    for kind, name in (("normal", "button"), ("hover", "button_highlighted"), ("disabled", "button_disabled")):
        save(button_sprite(kind), os.path.join(sprites, name + ".png"))
        write_json(os.path.join(sprites, name + ".png.mcmeta"), meta)

    # Donut SMP pack (cursed)
    pack = os.path.join(PACKS, "donut_smp")
    write_json(os.path.join(pack, "pack.mcmeta"), {"pack": {"description": "Cursed DrDonut-themed pack. Pigs meow now.", "pack_format": 34, "supported_formats": [34, 2048], "min_format": 34, "max_format": 2048}})
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

    # Donut Stone pack (stone & deepslate become a donut tile; rotation removal is done in code by Anti-Leak)
    leak = os.path.join(PACKS, "donut_stone")
    write_json(os.path.join(leak, "pack.mcmeta"), {"pack": {"description": "Donut stone and deepslate", "pack_format": 34, "supported_formats": [34, 2048], "min_format": 34, "max_format": 2048}})
    save(pixel_donut(64), os.path.join(leak, "pack.png"))
    btex = os.path.join(leak, "assets/minecraft/textures/block")
    small = donut.resize((14, 14), Image.LANCZOS)
    for name, bg in (("stone", (125, 125, 125)), ("deepslate", (77, 77, 82)), ("deepslate_top", (77, 77, 82))):
        tile = Image.new("RGBA", (16, 16), bg + (255,))
        r = random.Random(name)
        for _ in range(40):
            x, y = r.randrange(16), r.randrange(16)
            d = r.choice((-12, -6, 8))
            tile.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in bg) + (255,))
        tile.alpha_composite(small, (1, 1))
        save(tile, os.path.join(btex, name + ".png"))

    # Donut Voice Chat pack (themes Simple Voice Chat when it's installed)
    vc = os.path.join(PACKS, "donut_voicechat")
    write_json(os.path.join(vc, "pack.mcmeta"), {"pack": {"description": "Donut SMP theme for Simple Voice Chat", "pack_format": 34, "supported_formats": [34, 2048], "min_format": 34, "max_format": 2048}})
    save(pixel_donut(64), os.path.join(vc, "pack.png"))
    voicechat_textures(vc)

    print("art written")


if __name__ == "__main__":
    main()
