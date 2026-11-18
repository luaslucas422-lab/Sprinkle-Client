package fr.darkerdawn.sprinkle.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/** GUI drawing calls that differ between Minecraft versions (this copy: 1.21 / 1.21.1). */
public final class G {
    private G() {}

    public static void push(DrawContext g) { g.getMatrices().push(); }

    public static void pop(DrawContext g) { g.getMatrices().pop(); }

    public static void translate(DrawContext g, float x, float y) { g.getMatrices().translate(x, y, 0); }

    public static void scale(DrawContext g, float s) { g.getMatrices().scale(s, s, 1); }

    public static void rotate(DrawContext g, float radians) { g.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation(radians)); }

    /** Everything drawn after this goes on top. */
    public static void layer(DrawContext g) {
        g.draw();
        g.getMatrices().translate(0, 0, 300);
    }

    /** Draws a region (u,v,rw,rh) of a texture (tw x th) scaled into (x,y,w,h). */
    public static void tex(DrawContext g, Identifier id, int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th, int color) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f, ((color >>> 24) & 0xFF) / 255f);
        g.drawTexture(id, x, y, w, h, u, v, rw, rh, tw, th);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }
}
