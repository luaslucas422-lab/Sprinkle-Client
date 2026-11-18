package fr.darkerdawn.sprinkle.screen;

import fr.darkerdawn.sprinkle.compat.G;
import fr.darkerdawn.sprinkle.Join;
import fr.darkerdawn.sprinkle.Jokes;
import fr.darkerdawn.sprinkle.Textures;
import fr.darkerdawn.sprinkle.Theme;
import fr.darkerdawn.sprinkle.ui.SprinkleButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/** A short, silly loading screen before connecting to Donut SMP. */
public class JoinLoadingScreen extends Screen {
    private static final long DURATION = 3200;
    private final Screen parent;
    private final long start = Util.getMeasuringTimeMs();
    private final String tip = Jokes.pick(Jokes.TIPS);
    private final String[] lines = new String[4];
    private boolean done;

    public JoinLoadingScreen(Screen parent) {
        super(Text.literal("Joining Donut SMP"));
        this.parent = parent;
        java.util.List<String> pool = new java.util.ArrayList<>(Jokes.LOADING_LINES);
        java.util.Collections.shuffle(pool);
        for (int i = 0; i < lines.length; i++) lines[i] = pool.get(i);
    }

    @Override
    protected void init() {
        addDrawableChild(new SprinkleButton(width / 2 - 50, height - 34, 100, 20, Text.literal("Skip"), b -> go()));
    }

    private void go() {
        if (done) return;
        done = true;
        Join.connect(parent);
    }

    @Override
    public void tick() {
        if (Util.getMeasuringTimeMs() - start >= DURATION) go();
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
        Theme.skyBackground(g, width, height, (Util.getMeasuringTimeMs() - start) / 1000f);
        g.fillGradient(0, 0, width, height, 0x33020B35, 0xCC020B35);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        long t = Util.getMeasuringTimeMs() - start;
        float p = Math.min(1f, t / (float) DURATION);
        int cx = width / 2, cy = height / 2 - 20;

        int size = 64;
        G.push(g);
        G.translate(g, cx, cy - 30);
        G.rotate(g, t / 250f);
        Textures.draw(g, Textures.DONUT, -size / 2, -size / 2, size, size, Textures.DONUT_SIZE, Textures.DONUT_SIZE, 0xFFFFFFFF);
        G.pop(g);

        String line = lines[Math.min(lines.length - 1, (int) (p * lines.length))];
        g.drawCenteredTextWithShadow(textRenderer, line, cx, cy + 12, Theme.WHITE);

        int bw = 220, bx = cx - bw / 2, by = cy + 28;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + 9, Theme.OUTLINE);
        g.fill(bx, by, bx + bw, by + 8, Theme.DOUGH);
        g.fill(bx, by, bx + Math.round(bw * p), by + 8, Theme.FROSTING);
        Theme.sprinkles(g, bx + 1, by + 1, Math.max(5, Math.round(bw * p) - 2), 6, 99, (int) (p * 18));
        g.drawCenteredTextWithShadow(textRenderer, (int) (p * 100) + "%", cx, by + 12, Theme.MUTED);

        g.drawCenteredTextWithShadow(textRenderer, tip, cx, by + 34, Theme.GOLD);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void close() {
        done = true;
        client.setScreen(parent);
    }
}
