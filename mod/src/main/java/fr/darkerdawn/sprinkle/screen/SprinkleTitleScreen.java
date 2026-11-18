package fr.darkerdawn.sprinkle.screen;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.Join;
import fr.darkerdawn.sprinkle.Jokes;
import fr.darkerdawn.sprinkle.SprinkleClient;
import fr.darkerdawn.sprinkle.Sounds;
import fr.darkerdawn.sprinkle.Textures;
import fr.darkerdawn.sprinkle.Theme;
import fr.darkerdawn.sprinkle.ui.Hotspot;
import fr.darkerdawn.sprinkle.ui.SprinkleButton;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The Donut SMP main menu: every button says Donut SMP and takes you there. */
public class SprinkleTitleScreen extends Screen {
    private static final Random RNG = new Random();
    private static final String COPYRIGHT = "Copyright Mojang AB. Do not distribute!";

    private final String splash = Jokes.pick(Jokes.SPLASHES);
    private final long opened = Util.getMeasuringTimeMs();
    private int logoClicks;
    private long rageQuitAt = -1;
    private long eggUntil = -1;
    private String eggLine = "";
    private final List<float[]> rain = new ArrayList<>(); // x, y, speed, spin, size

    // logo box (set in init)
    private int logoX, logoY, logoW, logoH;

    public SprinkleTitleScreen() {
        super(Text.literal("Sprinkle Client"));
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int y = height / 4 + 48;
        logoW = 256;
        logoH = 64;
        logoX = cx - logoW / 2;
        logoY = Math.max(8, y - logoH - 22);

        Text donut = Text.literal("Donut SMP");
        addDrawableChild(new SprinkleButton(cx - 100, y, 200, 20, donut, b -> Join.press(this)));
        addDrawableChild(new SprinkleButton(cx - 100, y + 24, 200, 20, donut, b -> Join.press(this)));
        addDrawableChild(new SprinkleButton(cx - 100, y + 48, 200, 20, donut, b -> Join.press(this)));
        int row = y + 72 + 12;
        if (Config.get().menuVideo && fr.darkerdawn.sprinkle.MenuVideo.hasVideoFile()) {
            fr.darkerdawn.sprinkle.MenuVideo.ensureStarted();
            addDrawableChild(new fr.darkerdawn.sprinkle.ui.VideoWidget(cx - 100, row, 98, 20));
        } else {
            addDrawableChild(new SprinkleButton(cx - 100, row, 98, 20, donut, b -> Join.press(this)));
        }
        addDrawableChild(new SprinkleButton(cx + 2, row, 98, 20, Text.literal("Rage Quit"), b -> rageQuit()).silent());
        // where the language button was: credits; where accessibility was: Discord
        addDrawableChild(new SprinkleButton(cx - 124, row, 20, 20, Text.empty(), b -> client.setScreen(new CreditsScreen(this)))
                .icon(Textures.CREDITS, Textures.ICON_SIZE));
        addDrawableChild(new SprinkleButton(cx + 104, row, 20, 20, Text.empty(), b -> SprinkleClient.openDiscord(this))
                .icon(Textures.DISCORD, Textures.ICON_SIZE));
        // the settings gear, bottom right
        addDrawableChild(new SprinkleButton(width - 24, height - 24, 20, 20, Text.empty(), b -> client.setScreen(new SettingsScreen(this)))
                .icon(Textures.GEAR, Textures.ICON_SIZE));
        addDrawableChild(new Hotspot(logoX, logoY, logoW, logoH, this::logoClicked));
    }

    private void rageQuit() {
        if (rageQuitAt >= 0) return;
        if (!Config.get().rageQuitSound) {
            client.scheduleStop();
            return;
        }
        Sounds.play(Sounds.RAGE_QUIT, 1.0f, 1.0f);
        rageQuitAt = Util.getMeasuringTimeMs();
    }

    private void logoClicked() {
        logoClicks++;
        Sounds.play(Sounds.POP, 0.8f + logoClicks * 0.06f, 0.7f);
        if (logoClicks >= 10) {
            logoClicks = 0;
            eggUntil = Util.getMeasuringTimeMs() + 6000;
            eggLine = Jokes.pick(Jokes.EGG_LINES);
            Sounds.play(Sounds.FLEX, 1.0f, 1.0f);
            for (int i = 0; i < 70; i++) {
                rain.add(new float[]{RNG.nextFloat() * width, -RNG.nextFloat() * height, 1.2f + RNG.nextFloat() * 2.5f,
                        RNG.nextFloat() * 6.28f, 12 + RNG.nextFloat() * 14});
            }
            Config.get().logoClicksFound++;
            Config.save();
        }
    }

    @Override
    public void tick() {
        if (rageQuitAt >= 0 && Util.getMeasuringTimeMs() - rageQuitAt > 1800) client.scheduleStop();
        rain.removeIf(r -> r[1] > height + 30);
        for (float[] r : rain) {
            r[1] += r[2] * 3;
            r[3] += 0.15f;
        }
    }

    @Override
    public void renderBackground(DrawContext g, int mouseX, int mouseY, float delta) {
        float t = (Util.getMeasuringTimeMs() - opened) / 1000f;
        List<Textures.Img> bgs = Config.get().backgroundSlideshow ? Textures.backgrounds() : List.of();
        if (bgs.isEmpty()) {
            Theme.skyBackground(g, width, height, t);
        } else {
            float slide = 9f, fade = 1.5f;
            int idx = (int) (t / slide);
            float local = t - idx * slide;
            Textures.Img cur = bgs.get(idx % bgs.size());
            Textures.drawCover(g, cur, width, height, 1.06f + local * 0.006f, (idx % 2 == 0) ? local / slide : 1 - local / slide, 0xFFFFFFFF);
            if (local < fade && idx > 0) {
                Textures.Img prev = bgs.get((idx - 1) % bgs.size());
                float a = 1 - local / fade;
                Textures.drawCover(g, prev, width, height, 1.06f + (slide + local) * 0.006f, ((idx - 1) % 2 == 0) ? 1f : 0f, Theme.withAlpha(0xFFFFFF, a));
            }
        }
        // blue frosting tint so text stays readable
        g.fillGradient(0, 0, width, height, 0x55021A4A, 0xAA020B35);
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        boolean shaking = rageQuitAt >= 0;
        if (shaking) {
            g.getMatrices().pushMatrix();
            g.getMatrices().translate((RNG.nextFloat() - 0.5f) * 8, (RNG.nextFloat() - 0.5f) * 8);
        }
        super.render(g, mouseX, mouseY, delta);
        drawLogo(g, now);
        if (Config.get().splashes) drawSplash(g, now);

        g.drawTextWithShadow(textRenderer, "Sprinkle Client " + SprinkleClient.VERSION + " (Minecraft 1.21.11)", 2, height - 10, Theme.MUTED);
        int cw = textRenderer.getWidth(COPYRIGHT);
        g.drawTextWithShadow(textRenderer, COPYRIGHT, width - cw - 28, height - 10, 0xFFFFFFFF);
        String fan = "Fan-made. Not affiliated with Donut SMP.";
        g.drawTextWithShadow(textRenderer, fan, width - textRenderer.getWidth(fan) - 28, height - 20, 0xAAB9D6F5);

        if (!rain.isEmpty() || now < eggUntil) {
            g.createNewRootLayer();
            for (float[] r : rain) {
                int s = (int) r[4];
                g.getMatrices().pushMatrix();
                g.getMatrices().translate(r[0], r[1]);
                g.getMatrices().rotate(r[3]);
                Textures.draw(g, Textures.DONUT, -s / 2, -s / 2, s, s, Textures.DONUT_SIZE, Textures.DONUT_SIZE, 0xFFFFFFFF);
                g.getMatrices().popMatrix();
            }
            if (now < eggUntil) {
                g.getMatrices().pushMatrix();
                g.getMatrices().translate(width / 2f, height / 2f - 10);
                float pulse = 3f + (float) Math.sin(now / 120.0) * 0.3f;
                g.getMatrices().scale(pulse, pulse);
                g.drawCenteredTextWithShadow(textRenderer, eggLine, 0, -4, Theme.GOLD);
                g.getMatrices().popMatrix();
            }
        }
        if (shaking) {
            g.getMatrices().popMatrix();
            g.createNewRootLayer();
            float p = Math.min(1, (now - rageQuitAt) / 1800f);
            g.fill(0, 0, width, height, Theme.withAlpha(0x8A0000, p * 0.6f));
            g.getMatrices().pushMatrix();
            g.getMatrices().translate(width / 2f, height / 2f);
            g.getMatrices().scale(4, 4);
            g.drawCenteredTextWithShadow(textRenderer, "RAGE QUIT", 0, -4, Theme.RED);
            g.getMatrices().popMatrix();
        }
    }

    private void drawLogo(DrawContext g, long now) {
        Textures.Img custom = Textures.customLogo();
        float bob = (float) Math.sin(now / 900.0) * 1.5f;
        if (custom != null) {
            float s = Math.min(logoW / (float) custom.w(), logoH / (float) custom.h());
            int w = Math.round(custom.w() * s), h = Math.round(custom.h() * s);
            Textures.draw(g, custom.id(), logoX + (logoW - w) / 2, logoY + (logoH - h) / 2 + Math.round(bob), w, h, custom.w(), custom.h(), 0xFFFFFFFF);
            return;
        }
        // Default: the donut + DONUT SMP in big Minecraft letters
        String word = "DONUT SMP";
        float scale = 4f;
        int textW = Math.round(textRenderer.getWidth(word) * scale);
        int size = 56;
        int total = size + 8 + textW;
        int x = width / 2 - total / 2;
        int y = logoY + (logoH - size) / 2;
        g.getMatrices().pushMatrix();
        g.getMatrices().translate(x + size / 2f, y + size / 2f + bob);
        g.getMatrices().rotate((float) Math.sin(now / 1400.0) * 0.12f);
        Textures.draw(g, Textures.DONUT, -size / 2, -size / 2, size, size, Textures.DONUT_SIZE, Textures.DONUT_SIZE, 0xFFFFFFFF);
        g.getMatrices().popMatrix();
        g.getMatrices().pushMatrix();
        g.getMatrices().translate(x + size + 8, y + size / 2f - 4 * scale + bob);
        g.getMatrices().scale(scale, scale);
        g.drawText(textRenderer, word, 1, 1, Theme.OUTLINE, false);
        g.drawText(textRenderer, word, 0, 0, Theme.WHITE, false);
        g.getMatrices().popMatrix();
        g.getMatrices().pushMatrix();
        g.getMatrices().translate(x + size + 8 + textW / 2f, y + size / 2f + 4 * scale + 4 + bob);
        g.drawCenteredTextWithShadow(textRenderer, "SPRINKLE CLIENT", 0, 0, Theme.DOUGH_LIGHT);
        g.getMatrices().popMatrix();
    }

    private void drawSplash(DrawContext g, long now) {
        g.getMatrices().pushMatrix();
        g.getMatrices().translate(width / 2f + 115, logoY + logoH - 8);
        g.getMatrices().rotate((float) Math.toRadians(-20));
        float s = 1.8f - Math.abs((float) Math.sin((now % 1000L) / 1000f * Math.PI * 2) * 0.1f);
        s = s * 100f / (textRenderer.getWidth(splash) + 32);
        g.getMatrices().scale(s, s);
        g.drawCenteredTextWithShadow(textRenderer, splash, 0, -8, 0xFFFFFF00);
        g.getMatrices().popMatrix();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
