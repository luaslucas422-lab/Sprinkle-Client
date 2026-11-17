package fr.darkerdawn.sprinkle;

import net.minecraft.client.gui.DrawContext;

/** Colours taken from the blue donut icon, plus small drawing helpers. */
public final class Theme {
    private Theme() {}

    public static final int SKY = 0xFF2EA1FC;
    public static final int LIGHT = 0xFF4CB3F5;
    public static final int FROSTING = 0xFF0370FF;
    public static final int FROSTING_DARK = 0xFF0256C4;
    public static final int FROSTING_LIGHT = 0xFF3D93FF;
    public static final int OUTLINE = 0xFF020B35;
    public static final int DOUGH = 0xFFAF7F52;
    public static final int DOUGH_LIGHT = 0xFFD7A673;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int GOLD = 0xFFFFD34D;
    public static final int GREEN = 0xFF5CFF7A;
    public static final int RED = 0xFFFF5C5C;
    public static final int MUTED = 0xFFB9D6F5;

    public static final int[] SPRINKLES = {0xFFFF6FB1, 0xFFFFE15C, 0xFF7CFF8A, 0xFFFFFFFF, 0xFFFF9F43, 0xFFB98CFF};

    /** A chunky Minecraft-style panel with a dark outline and frosting body. */
    public static void button(DrawContext g, int x, int y, int w, int h, boolean hovered, boolean active, long seed) {
        int body = !active ? 0xFF4A5A75 : hovered ? FROSTING_LIGHT : FROSTING;
        int top = !active ? 0xFF66779A : hovered ? 0xFF7DB6FF : 0xFF3B8EFF;
        int bottom = !active ? 0xFF33405A : FROSTING_DARK;
        g.fill(x, y, x + w, y + h, OUTLINE);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, body);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, top);
        g.fill(x + 1, y + h - 3, x + w - 1, y + h - 1, bottom);
        if (hovered && active) sprinkles(g, x + 3, y + 3, w - 6, h - 7, seed, 7);
    }

    /** Little sprinkles scattered in a box (deterministic per seed so they don't flicker). */
    public static void sprinkles(DrawContext g, int x, int y, int w, int h, long seed, int count) {
        if (w <= 4 || h <= 2) return;
        long s = seed * 6364136223846793005L + 1442695040888963407L;
        for (int i = 0; i < count; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            int px = x + (int) Math.floorMod(s >>> 17, (long) (w - 3));
            s = s * 6364136223846793005L + 1442695040888963407L;
            int py = y + (int) Math.floorMod(s >>> 17, (long) Math.max(1, h - 1));
            int col = SPRINKLES[(int) Math.floorMod(s >>> 40, (long) SPRINKLES.length)];
            boolean horizontal = ((s >>> 33) & 1) == 0;
            g.fill(px, py, px + (horizontal ? 3 : 1), py + (horizontal ? 1 : 2), col);
        }
    }

    /** Big rounded-ish panel used by settings and credits. */
    public static void panel(DrawContext g, int x, int y, int w, int h) {
        g.fill(x + 1, y, x + w - 1, y + h, OUTLINE);
        g.fill(x, y + 1, x + w, y + h - 1, OUTLINE);
        g.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xEE0A2A5C);
        g.fill(x + 2, y + 2, x + w - 2, y + 4, FROSTING);
    }

    /** Fallback background: sky gradient with a ring of dough at the bottom. */
    public static void skyBackground(DrawContext g, int w, int h, float time) {
        g.fillGradient(0, 0, w, h, SKY, 0xFF0B5CC2);
        long seed = 42;
        for (int i = 0; i < 40; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            float speed = 4 + (seed >>> 60);
            int x = (int) Math.floorMod((seed >>> 20), (long) Math.max(1, w));
            int y = (int) Math.floorMod((long) ((seed >>> 34) % Math.max(1, h) + time * speed), (long) Math.max(1, h));
            int col = (SPRINKLES[i % SPRINKLES.length] & 0x00FFFFFF) | 0x88000000;
            boolean horizontal = (i & 1) == 0;
            g.fill(x, y, x + (horizontal ? 4 : 1), y + (horizontal ? 1 : 3), col);
        }
    }

    public static int withAlpha(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255)));
        return (a << 24) | (rgb & 0x00FFFFFF);
    }
}
