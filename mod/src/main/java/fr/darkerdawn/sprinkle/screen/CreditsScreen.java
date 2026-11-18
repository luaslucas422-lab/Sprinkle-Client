package fr.darkerdawn.sprinkle.screen;

import fr.darkerdawn.sprinkle.compat.G;
import fr.darkerdawn.sprinkle.Jokes;
import fr.darkerdawn.sprinkle.Textures;
import fr.darkerdawn.sprinkle.Theme;
import fr.darkerdawn.sprinkle.ui.SprinkleButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/** Credits: made by DarkerDawnFR, plus some Donut SMP shout-outs. */
public class CreditsScreen extends Screen {
    private final Screen parent;
    private final long start = Util.getMeasuringTimeMs();

    public CreditsScreen(Screen parent) {
        super(Text.literal("Credits"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addDrawableChild(new SprinkleButton(width / 2 - 50, height - 30, 100, 20, Text.literal("Back"), b -> close()));
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
        Theme.skyBackground(g, width, height, (Util.getMeasuringTimeMs() - start) / 1000f);
        g.fill(0, 0, width, height, 0x88020B35);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int cx = width / 2;
        int pw = Math.min(300, width - 20), ph = Math.min(height - 50, 230);
        int px = cx - pw / 2, py = 12;
        Theme.panel(g, px, py, pw, ph);
        Textures.draw(g, Textures.DONUT, cx - 16, py + 10, 32, 32, Textures.DONUT_SIZE, Textures.DONUT_SIZE, 0xFFFFFFFF);
        G.push(g);
        G.translate(g, cx, py + 48);
        G.scale(g, 2);
        g.drawCenteredTextWithShadow(textRenderer, "Sprinkle Client", 0, 0, Theme.WHITE);
        G.pop(g);
        g.drawCenteredTextWithShadow(textRenderer, "Made by", cx, py + 72, Theme.MUTED);
        G.push(g);
        G.translate(g, cx, py + 84);
        G.scale(g, 1.5f);
        g.drawCenteredTextWithShadow(textRenderer, "DarkerDawnFR", 0, 0, Theme.GOLD);
        G.pop(g);

        int y = py + 106;
        g.drawCenteredTextWithShadow(textRenderer, "Special thanks", cx, y, Theme.DOUGH_LIGHT);
        y += 14;
        for (String[] t : Jokes.THANKS) {
            if (y > py + ph - 30) break;
            g.drawTextWithShadow(textRenderer, t[0], px + 14, y, Theme.WHITE);
            g.drawTextWithShadow(textRenderer, t[1], px + 80, y, Theme.MUTED);
            y += 12;
        }
        g.drawCenteredTextWithShadow(textRenderer, "Fan-made. Not affiliated with DrDonut or Donut SMP.", cx, py + ph - 14, 0xFF7FA6D6);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
