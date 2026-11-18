package fr.darkerdawn.sprinkle.compat;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** GUI drawing calls that differ between Minecraft versions (this copy: 1.21.6+). */
public final class G {
    private G() {}

    public static void push(DrawContext g) { g.getMatrices().pushMatrix(); }

    public static void pop(DrawContext g) { g.getMatrices().popMatrix(); }

    public static void translate(DrawContext g, float x, float y) { g.getMatrices().translate(x, y); }

    public static void scale(DrawContext g, float s) { g.getMatrices().scale(s, s); }

    public static void rotate(DrawContext g, float radians) { g.getMatrices().rotate(radians); }

    /** Everything drawn after this goes on top (even over text drawn before). */
    public static void layer(DrawContext g) { g.createNewRootLayer(); }

    /** Draws a region (u,v,rw,rh) of a texture (tw x th) scaled into (x,y,w,h). */
    public static void tex(DrawContext g, Identifier id, int x, int y, float u, float v, int w, int h, int rw, int rh, int tw, int th, int color) {
        g.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, w, h, rw, rh, tw, th, color);
    }
}
