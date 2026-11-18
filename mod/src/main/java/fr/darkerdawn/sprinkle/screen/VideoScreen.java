package fr.darkerdawn.sprinkle.screen;

import fr.darkerdawn.sprinkle.MenuVideo;
import fr.darkerdawn.sprinkle.Theme;
import fr.darkerdawn.sprinkle.compat.G;
import fr.darkerdawn.sprinkle.ui.SprinkleButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/** "Real Video": the menu video full screen, looping, with a tiny X in the top-left corner. */
public class VideoScreen extends Screen {
    private final Screen parent;
    private final long start = Util.getMeasuringTimeMs();

    public VideoScreen(Screen parent) {
        super(Text.literal("Real Video"));
        this.parent = parent;
        MenuVideo.ensureStarted();
    }

    @Override
    protected void init() {
        addDrawableChild(new SprinkleButton(3, 3, 12, 12, Text.literal("x"), b -> close()));
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
        g.fill(0, 0, width, height, 0xFF000000);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        Identifier frame = MenuVideo.ready() ? MenuVideo.frame(Util.getMeasuringTimeMs() - start) : null;
        if (frame == null) {
            String s = MenuVideo.hasVideoFile() ? (MenuVideo.status.isEmpty() ? "Loading video..." : MenuVideo.status)
                    : "No video yet. Pick one in the Sprinkle installer.";
            g.drawCenteredTextWithShadow(textRenderer, s, width / 2, height / 2 - 4, Theme.MUTED);
            return;
        }
        int tw = MenuVideo.width(), th = MenuVideo.height();
        float s = Math.min(width / (float) tw, height / (float) th);
        int w = Math.round(tw * s), h = Math.round(th * s);
        G.tex(g, frame, (width - w) / 2, (height - h) / 2, 0, 0, w, h, tw, th, tw, th, 0xFFFFFFFF);
        G.layer(g);
        for (var c : children()) if (c instanceof SprinkleButton b) b.render(g, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
