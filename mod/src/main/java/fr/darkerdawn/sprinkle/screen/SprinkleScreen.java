package fr.darkerdawn.sprinkle.screen;

import fr.darkerdawn.sprinkle.Theme;
import fr.darkerdawn.sprinkle.ui.SprinkleButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.function.Consumer;

/** Shared look for Sprinkle's own screens: sprinkle sky (or the game behind it), a panel, a title. */
public abstract class SprinkleScreen extends Screen {
    protected final Screen parent;
    private final long opened = Util.getMeasuringTimeMs();
    protected int px, py, pw, ph;

    protected SprinkleScreen(String title, Screen parent) {
        super(Text.literal(title));
        this.parent = parent;
    }

    protected void layoutPanel(int maxW, int maxH) {
        pw = Math.min(maxW, width - 16);
        ph = Math.min(maxH, height - 16);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
    }

    protected SprinkleButton button(int x, int y, int w, String label, Consumer<SprinkleButton> action) {
        return addDrawableChild(new SprinkleButton(x, y, w, 20, Text.literal(label), action));
    }

    protected static String onOff(boolean on) {
        return on ? "§aON" : "§cOFF";
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
        if (client != null && client.world != null) {
            g.fill(0, 0, width, height, 0xAA020B35);
        } else {
            Theme.skyBackground(g, width, height, (Util.getMeasuringTimeMs() - opened) / 1000f);
            g.fill(0, 0, width, height, 0x66020B35);
        }
        Theme.panel(g, px, py, pw, ph);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredTextWithShadow(textRenderer, title, px + pw / 2, py + 9, Theme.WHITE);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
