"""Synthesizes every Sprinkle Client sound from scratch (no samples, so no licensing issues).
Run: python3 tools/gen_sounds.py   (needs numpy + ffmpeg with libvorbis)
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mod/src/main/resources/assets/sprinkleclient/sounds")
SR = 44100
rng = np.random.default_rng(67)


def t_axis(sec):
    return np.arange(int(sec * SR)) / SR


def env(n, a=0.005, r=0.1):
    t = np.arange(n) / SR
    e = np.minimum(1, t / max(a, 1e-4))
    tail = np.clip((n / SR - t) / max(r, 1e-4), 0, 1)
    return e * tail


def write_ogg(name, sig, gain=0.9):
    sig = np.asarray(sig, dtype=np.float64)
    sig = sig / max(1e-6, np.max(np.abs(sig))) * gain
    pcm = (sig * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        with wave.open(tmp.name, "w") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(pcm.tobytes())
        os.makedirs(OUT, exist_ok=True)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp.name, "-ac", "1", "-c:a", "libvorbis",
                        "-q:a", "4", os.path.join(OUT, name + ".ogg")], check=True)
    os.unlink(tmp.name)


def bell(freq, sec, decay, partials=((1, 1), (2.0, 0.4), (3.0, 0.2), (4.2, 0.1))):
    t = t_axis(sec)
    s = sum(a * np.sin(2 * np.pi * freq * m * t) * np.exp(-t * decay * (0.7 + 0.3 * m)) for m, a in partials)
    return s * np.minimum(1, t / 0.002)


def mix(total_sec, parts):
    out = np.zeros(int(total_sec * SR))
    for start, sig in parts:
        i = int(start * SR)
        n = min(len(sig), len(out) - i)
        if n > 0:
            out[i:i + n] += sig[:n]
    return out


# ------------------------------------------------------------------ UI sounds

def cash():
    """Cash-register ka-ching: a mechanical clack, then two bright bells."""
    clack_n = int(0.04 * SR)
    clack = rng.normal(0, 1, clack_n) * np.exp(-np.arange(clack_n) / SR * 120)
    clack = np.diff(np.concatenate([[0], clack])) * 0.6
    return mix(0.6, [(0, clack), (0.035, bell(2093, 0.55, 7) * 0.6), (0.05, bell(2637, 0.5, 8) * 0.5)])


def coin():
    return mix(0.35, [(0, bell(1975, 0.12, 30) * 0.6), (0.06, bell(2637, 0.3, 14) * 0.7)])


def pop():
    t = t_axis(0.12)
    f = 700 * np.exp(-t * 18) + 250
    return np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 35)


def rage_quit():
    """Sad trombone 'wah wah wah waaah' with a crash at the start."""
    parts = []
    notes = [(0.0, 0.32, 311), (0.34, 0.32, 294), (0.68, 0.32, 277), (1.02, 1.1, 262)]
    for start, dur, f in notes:
        t = t_axis(dur)
        vib = 1 + (0.012 * np.sin(2 * np.pi * 6 * t) if dur > 0.5 else 0)
        bend = np.where(t > dur - 0.12, 1 - (t - (dur - 0.12)) * 0.25, 1) if dur < 0.5 else 1 - t * 0.04
        ph = 2 * np.pi * np.cumsum(f * vib * bend) / SR
        tone = sum(np.sin(k * ph) / k ** 0.9 for k in range(1, 9))
        # wah: a moving low-pass made by mixing brightness over time
        bright = 0.35 + 0.65 * np.minimum(1, t / 0.08) * np.exp(-t * 3)
        tone = tone * (0.55 + 0.45 * bright) * env(len(t), 0.02, 0.08)
        parts.append((start, tone * 0.5))
    crash_n = int(0.6 * SR)
    crash = rng.normal(0, 1, crash_n) * np.exp(-np.arange(crash_n) / SR * 7) * 0.35
    parts.append((0, crash))
    return mix(2.2, parts)


def alarm():
    out = []
    for i in range(3):
        t = t_axis(0.18)
        out.append((i * 0.24, np.sign(np.sin(2 * np.pi * 880 * t)) * 0.4 * env(len(t), 0.005, 0.03)))
        t2 = t_axis(0.06)
        out.append((i * 0.24 + 0.18, np.sign(np.sin(2 * np.pi * 660 * t2)) * 0.3 * env(len(t2), 0.003, 0.02)))
    return mix(0.8, out)


def flex():
    """Fanfare + cash for Rich Flex."""
    notes = [(0, 523), (0.1, 659), (0.2, 784), (0.3, 1047)]
    parts = []
    for s, f in notes:
        t = t_axis(0.5 if f == 1047 else 0.14)
        ph = 2 * np.pi * f * t
        tone = (np.sin(ph) + 0.5 * np.sin(2 * ph) + 0.3 * np.sin(3 * ph)) * env(len(t), 0.01, 0.06)
        parts.append((s, tone * 0.4))
    parts.append((0.45, cash() * 0.8))
    return mix(1.2, parts)


# ------------------------------------------------------------------ meows (pigs in the Donut SMP pack)

def meow(base=620, dur=0.55, shape="up", seed=0):
    """A synthetic cat meow: rising-falling pitch, 'ee-ow' vowel made by moving formants."""
    r = np.random.default_rng(seed)
    t = t_axis(dur)
    x = t / dur
    if shape == "up":
        contour = 0.75 + 0.45 * np.sin(np.pi * np.minimum(1, x * 1.3)) - 0.15 * x
    elif shape == "hurt":
        contour = 1.25 - 0.5 * x
    else:  # sad
        contour = 0.9 - 0.35 * x
    f0 = base * contour * (1 + 0.01 * np.sin(2 * np.pi * 7 * t))
    ph = 2 * np.pi * np.cumsum(f0) / SR
    # formant 1 & 2 glide from "ee" to "ow"
    f1 = 450 + 450 * np.sin(np.pi * np.minimum(1, x * 1.6))
    f2 = 2300 - 1400 * x
    sig = np.zeros_like(t)
    for k in range(1, 18):
        fk = f0 * k
        w = np.exp(-((fk - f1) / 260) ** 2) + 0.6 * np.exp(-((fk - f2) / 420) ** 2) + 0.02
        sig += w * np.sin(k * ph) / k ** 0.3
    sig += r.normal(0, 0.02, len(t))
    return sig * env(len(t), 0.04, 0.12)


# ------------------------------------------------------------------ soundboard (all synthesized)

def sb_pay():
    return mix(0.8, [(0, coin() * 0.8), (0.12, bell(1568, 0.6, 6) * 0.6), (0.2, bell(2093, 0.6, 6) * 0.5)])


def sb_kill():
    n = int(0.15 * SR)
    crunch = rng.normal(0, 1, n) * np.exp(-np.arange(n) / SR * 25)
    t = t_axis(0.25)
    thud = np.sin(2 * np.pi * np.cumsum(90 + 200 * np.exp(-t * 20)) / SR) * np.exp(-t * 10)
    return mix(1.0, [(0, crunch * 0.5), (0, thud), (0.25, bell(1318, 0.5, 6) * 0.5), (0.4, bell(1760, 0.6, 5) * 0.5)])


def sb_airhorn():
    parts = []
    for i, dur in enumerate((0.25, 0.25, 0.9)):
        t = t_axis(dur)
        f = 466 * (1 + 0.004 * np.sin(2 * np.pi * 5 * t))
        ph = 2 * np.pi * np.cumsum(f) / SR
        saw = sum(np.sin(k * ph) / k for k in range(1, 25)) + 0.7 * sum(np.sin(k * ph * 1.5) / k for k in range(1, 15))
        parts.append((i * 0.3, np.tanh(saw * 1.5) * env(len(t), 0.01, 0.05) * 0.4))
    return mix(1.6, parts)


def sb_boom():
    t = t_axis(1.6)
    f = 45 + 80 * np.exp(-t * 8)
    tone = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t * 2.2)
    click = rng.normal(0, 1, len(t)) * np.exp(-t * 60) * 0.3
    return np.tanh((tone + click) * 2.5)


def sb_drumroll():
    out = []
    for i in range(46):
        n = int(0.06 * SR)
        hit = rng.normal(0, 1, n) * np.exp(-np.arange(n) / SR * 40) * (0.25 + i / 92)
        out.append((i * 0.045, hit))
    crash_n = int(1.2 * SR)
    out.append((2.1, rng.normal(0, 1, crash_n) * np.exp(-np.arange(crash_n) / SR * 3) * 0.6))
    return mix(3.4, out)


def sb_crickets():
    parts = []
    for i in range(8):
        t = t_axis(0.12)
        chirp = np.sin(2 * np.pi * 4300 * t) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 50 * t))) * env(len(t), 0.005, 0.02)
        parts.append((i * 0.35 + (0.12 if i % 2 else 0), chirp * 0.3))
    return mix(3.2, parts)


def sb_scratch():
    t = t_axis(0.7)
    rate = np.where(t < 0.35, 1 - t * 4, -0.4 + (t - 0.35) * 3)
    noise = rng.normal(0, 1, len(t))
    base = np.cumsum(noise) * 0.02
    sig = np.sin(2 * np.pi * np.cumsum(300 * np.abs(rate) + 80) / SR) * 0.5 + np.diff(np.concatenate([[0], base])) * 3
    return sig * env(len(t), 0.005, 0.05)


def sb_clown():
    parts = []
    for i, f in enumerate((660, 880)):
        t = t_axis(0.18)
        ph = 2 * np.pi * f * t * (1 + 0.15 * t)
        honk = np.sign(np.sin(ph)) * 0.3 + np.sin(2 * ph) * 0.2
        parts.append((i * 0.2, honk * env(len(t), 0.005, 0.03)))
    return mix(0.5, parts)


def sb_dundun():
    parts = []
    for i, (f, d) in enumerate(((196, 0.3), (185, 0.3), (175, 1.4))):
        t = t_axis(d)
        ph = 2 * np.pi * f * t
        tone = sum(np.sin(k * ph) / k for k in range(1, 12)) * env(len(t), 0.01, 0.2)
        parts.append(((0, 0.42, 0.84)[i], tone * 0.35))
    return mix(2.4, parts)


# ------------------------------------------------------------------ menu music (original, chill loop)

def menu_music():
    bpm = 84
    beat = 60 / bpm
    bars = 16
    total = bars * 4 * beat
    out = np.zeros(int(total * SR) + SR)
    # chords: Fmaj7 - Em7 - Dm7 - Cmaj7 (simple, original progression voicing)
    chords = [[174.6, 220.0, 261.6, 329.6], [164.8, 196.0, 246.9, 293.7], [146.8, 174.6, 220.0, 261.6], [130.8, 164.8, 196.0, 246.9]]
    melody = [659, 587, 523, 587, 659, 784, 659, 587, 523, 494, 523, 587, 523, 440, 494, 523]

    def place(start, sig):
        i = int(start * SR)
        n = min(len(sig), len(out) - i)
        out[i:i + n] += sig[:n]

    for bar in range(bars):
        ch = chords[bar % 4]
        start = bar * 4 * beat
        for k, f in enumerate(ch):  # soft electric-piano pad
            t = t_axis(4 * beat)
            tone = (np.sin(2 * np.pi * f * t) + 0.25 * np.sin(2 * np.pi * 2 * f * t)) * np.exp(-t * 0.6)
            place(start + k * 0.02, tone * 0.12)
        for b in range(4):  # bass + gentle kick
            t = t_axis(beat * 0.9)
            bass = np.sin(2 * np.pi * ch[0] / 2 * t) * np.exp(-t * 2.5)
            place(start + b * beat, bass * 0.25)
            kt = t_axis(0.25)
            kick = np.sin(2 * np.pi * np.cumsum(60 + 120 * np.exp(-kt * 30)) / SR) * np.exp(-kt * 14)
            if b % 2 == 0:
                place(start + b * beat, kick * 0.35)
            hn = int(0.05 * SR)
            hat = rng.normal(0, 1, hn) * np.exp(-np.arange(hn) / SR * 90)
            hat = np.diff(np.concatenate([[0], hat]))
            place(start + b * beat + beat / 2, hat * 0.05)
        if bar >= 4:  # bell melody on top
            for n in range(4):
                f = melody[(bar * 4 + n) % len(melody)]
                if (bar + n) % 3 == 2:
                    continue
                place(start + n * beat, bell(f, beat * 1.2, 4) * 0.12)
    out = out[: int(total * SR)]
    fade = int(0.5 * SR)
    out[:fade] *= np.linspace(0, 1, fade)
    out[-fade:] *= np.linspace(1, 0, fade)
    return out


if __name__ == "__main__":
    write_ogg("cash", cash())
    write_ogg("coin", coin())
    write_ogg("pop", pop(), 0.7)
    write_ogg("rage_quit", rage_quit())
    write_ogg("alarm", alarm(), 0.7)
    write_ogg("flex", flex())
    write_ogg("meow1", meow(640, 0.55, "up", 1))
    write_ogg("meow2", meow(560, 0.7, "up", 2))
    write_ogg("meow3", meow(720, 0.45, "up", 3))
    write_ogg("meow_hurt", meow(820, 0.3, "hurt", 4))
    write_ogg("meow_sad", meow(520, 1.1, "sad", 5))
    write_ogg("menu_music", menu_music(), 0.8)
    write_ogg("sb_pay", sb_pay())
    write_ogg("sb_kill", sb_kill())
    write_ogg("sb_airhorn", sb_airhorn())
    write_ogg("sb_boom", sb_boom())
    write_ogg("sb_drumroll", sb_drumroll())
    write_ogg("sb_crickets", sb_crickets(), 0.7)
    write_ogg("sb_scratch", sb_scratch())
    write_ogg("sb_clown", sb_clown(), 0.8)
    write_ogg("sb_dundun", sb_dundun())
    print("sounds written to", OUT)
