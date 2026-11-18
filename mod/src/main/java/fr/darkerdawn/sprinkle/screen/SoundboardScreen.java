package fr.darkerdawn.sprinkle.screen;

import fr.darkerdawn.sprinkle.Theme;
import fr.darkerdawn.sprinkle.feature.Soundboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;

/** The Donut SMP Soundboard. */
public class SoundboardScreen extends SprinkleScreen {
    public SoundboardScreen(Screen parent) {
        super("Donut SMP Soundboard", parent);
    }

    @Override
    protected void init() {
        layoutPanel(330, 230);
        int cols = 3, bw = (pw - 24 - (cols - 1) * 4) / cols;
        int y0 = py + 24;
        for (int i = 0; i < Soundboard.SOUNDS.size(); i++) {
            Soundboard.Sound s = Soundboard.SOUNDS.get(i);
            int x = px + 12 + (i % cols) * (bw + 4), y = y0 + (i / cols) * 24;
            button(x, y, bw, s.label(), b -> Soundboard.play(s)).silent();
        }
        button(width / 2 - 104, py + ph - 26, 100, "Stop", b -> Soundboard.stop());
        button(width / 2 + 4, py + ph - 26, 100, "Done", b -> close());
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        String note = Soundboard.voiceAvailable ? "§aPlays into your voice chat too (servers with Simple Voice Chat)."
                : "§7Only you hear it (Simple Voice Chat isn't installed).";
        g.drawCenteredTextWithShadow(textRenderer, note, width / 2, py + ph - 40, Theme.WHITE);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
