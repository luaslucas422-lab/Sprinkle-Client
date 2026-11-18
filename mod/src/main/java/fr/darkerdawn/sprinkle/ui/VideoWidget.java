package fr.darkerdawn.sprinkle.ui;

import fr.darkerdawn.sprinkle.MenuVideo;
import fr.darkerdawn.sprinkle.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/** The looping video where the button next to Rage Quit used to be. Hover to see it bigger. */
public class VideoWidget extends ClickableWidget {
    private final long start = Util.getMeasuringTimeMs();
    private boolean pinned;

    public VideoWidget(int x, int y, int w, int h) {
        super(x, y, w, h, Text.literal("Video"));
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        pinned = !pinned;
    }

    @Override
    public void playDownSound(SoundManager soundManager) {
    }

    @Override
    protected void renderWidget(DrawContext g, int mouseX, int mouseY, float delta) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        g.fill(x, y, x + w, y + h, Theme.OUTLINE);
        Identifier frame = MenuVideo.ready() ? MenuVideo.frame(Util.getMeasuringTimeMs() - start) : null;
        if (frame == null) {
            Theme.button(g, x, y, w, h, false, true, 5);
            String s = MenuVideo.status.isEmpty() ? "Video" : MenuVideo.status;
            g.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer, s, x + w / 2, y + (h - 8) / 2, Theme.MUTED);
            return;
        }
        cover(g, frame, x + 1, y + 1, w - 2, h - 2);
        if (isHovered() || pinned) {
            int bw = 96, bh = Math.round(bw * MenuVideo.height() / (float) Math.max(1, MenuVideo.width()));
            bh = Math.min(bh, 200);
            int bx = x + w / 2 - bw / 2, by = Math.max(2, y - bh - 4);
            g.createNewRootLayer();
            g.fill(bx - 2, by - 2, bx + bw + 2, by + bh + 2, Theme.OUTLINE);
            g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, Theme.FROSTING);
            cover(g, frame, bx, by, bw, bh);
        }
    }

    /** Fills the box with the frame, cropping the edges. */
    private static void cover(DrawContext g, Identifier id, int x, int y, int bw, int bh) {
        int tw = MenuVideo.width(), th = MenuVideo.height();
        float scale = Math.max(bw / (float) tw, bh / (float) th);
        int rw = Math.max(1, Math.round(bw / scale)), rh = Math.max(1, Math.round(bh / scale));
        int u = Math.max(0, (tw - rw) / 2), v = Math.max(0, (th - rh) / 2);
        g.drawTexture(RenderPipelines.GUI_TEXTURED, id, x, y, u, v, bw, bh, rw, rh, tw, th, 0xFFFFFFFF);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    }
}
