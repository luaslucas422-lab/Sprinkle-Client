package fr.darkerdawn.sprinkle;

import fr.darkerdawn.sprinkle.feature.Tracker;
import fr.darkerdawn.sprinkle.screen.ModsScreen;
import fr.darkerdawn.sprinkle.screen.SettingsScreen;
import fr.darkerdawn.sprinkle.ui.SprinkleButton;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;

/** Themes the Escape menu, adds the gear to the vanilla title screen and jokes to the death screen. */
public final class PauseMenu {
    private PauseMenu() {}

    private static boolean rageArmed;
    private static long rageBannerUntil;

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            if (rageArmed && client.world == null) {
                // we just rage quit from the pause menu: play the sound now that we're out
                rageArmed = false;
                if (Config.get().rageQuitSound) Sounds.play(Sounds.RAGE_QUIT, 1.0f, 1.0f);
                rageBannerUntil = System.currentTimeMillis() + 2500;
            }
            if (screen instanceof GameMenuScreen && Config.get().themedPause) themePause(client, screen, w, h);
            if (screen instanceof TitleScreen) {
                // vanilla menu is on: keep a way back to the Donut menu
                Screens.getButtons(screen).add(new SprinkleButton(w - 24, h - 24, 20, 20, Text.empty(),
                        b -> client.setScreen(new SettingsScreen(screen))).icon(Textures.GEAR, Textures.ICON_SIZE));
            }
            if (screen instanceof DeathScreen) {
                ScreenEvents.afterRender(screen).register((s, g, mx, my, d) -> {
                    if (!Config.get().deathSaver) return;
                    var tr = client.textRenderer;
                    g.drawCenteredTextWithShadow(tr, "§b" + Tracker.deathLine, w / 2, h / 4 + 140, 0xFFFFFFFF);
                    Config.DeathSaverRecord r = Config.get().deaths.isEmpty() ? null : Config.get().deaths.get(0);
                    if (r != null) {
                        String where = Config.get().streamerMode ? "hidden (streamer mode)" : r.x + " " + r.y + " " + r.z;
                        g.drawCenteredTextWithShadow(tr, "§7Death Saver: §f" + where, w / 2, h / 4 + 152, 0xFFFFFFFF);
                    }
                });
            }
            if (System.currentTimeMillis() < rageBannerUntil) {
                ScreenEvents.afterRender(screen).register((s, g, mx, my, d) -> {
                    if (System.currentTimeMillis() > rageBannerUntil) return;
                    g.getMatrices().pushMatrix();
                    g.getMatrices().translate(w / 2f, 18);
                    g.getMatrices().scale(2.5f, 2.5f);
                    g.drawCenteredTextWithShadow(client.textRenderer, "RAGE QUIT!", 0, 0, Theme.RED);
                    g.getMatrices().popMatrix();
                });
            }
        });
    }

    private static boolean isKey(ClickableWidget w, String key) {
        return w.getMessage().getContent() instanceof TranslatableTextContent t && t.getKey().equals(key);
    }

    private static void themePause(MinecraftClient client, Screen screen, int w, int h) {
        ClickableWidget rage = null;
        for (ClickableWidget b : Screens.getButtons(screen)) {
            if (isKey(b, "menu.disconnect") || isKey(b, "menu.returnToMenu")) {
                b.setMessage(Text.literal("Rage Quit"));
                rage = b;
            }
        }
        ClickableWidget rageButton = rage;
        String joke = Jokes.pick(Jokes.SPLASHES);
        ScreenMouseEvents.beforeMouseClick(screen).register((s, click) -> {
            if (rageButton != null && rageButton.isHovered()) rageArmed = true;
        });
        ScreenEvents.afterBackground(screen).register((s, g, mx, my, d) -> {
            g.fillGradient(0, 0, w, h, 0x660370FF, 0x99020B35);
            Theme.sprinkles(g, 0, 0, w, 30, 7, 30);
            Theme.sprinkles(g, 0, h - 30, w, 28, 11, 30);
        });
        ScreenEvents.afterRender(screen).register((s, g, mx, my, d) -> {
            Textures.draw(g, Textures.DONUT, 6, 6, 20, 20, Textures.DONUT_SIZE, Textures.DONUT_SIZE, 0xFFFFFFFF);
            g.drawTextWithShadow(client.textRenderer, "Sprinkle Client", 30, 12, Theme.WHITE);
            g.drawCenteredTextWithShadow(client.textRenderer, joke, w / 2, h - 14, 0xFFFFFF55);
        });
        Screens.getButtons(screen).add(new SprinkleButton(w - 24, h - 24, 20, 20, Text.empty(),
                b -> client.setScreen(new ModsScreen(screen))).icon(Textures.GEAR, Textures.ICON_SIZE));
    }
}
