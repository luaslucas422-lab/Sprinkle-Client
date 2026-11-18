package fr.darkerdawn.sprinkle.screen;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.SkyChanger;
import fr.darkerdawn.sprinkle.Textures;
import fr.darkerdawn.sprinkle.Theme;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.nio.file.Files;
import java.nio.file.Path;

/** The bottom-right gear: menu, sounds, Donut mods, links and folders. */
public class SettingsScreen extends SprinkleScreen {
    private static final String[] TABS = {"Menu", "Sounds", "Donut Mods", "Links"};
    private static int tab;
    private String note = "";

    public static void showTab(int t) {
        tab = t;
    }

    public SettingsScreen(Screen parent) {
        super("Sprinkle Settings", parent);
    }

    @Override
    protected void init() {
        layoutPanel(340, 270);
        labels.clear();
        hint = "";
        Config c = Config.get();
        int tw = (pw - 20) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int idx = i;
            String label = (i == tab ? "§e" : "") + TABS[i];
            button(px + 10 + i * tw, py + 22, tw - 2, label, b -> { tab = idx; note = ""; clearAndInit(); });
        }
        int left = px + 14, right = px + pw / 2 + 3, cw = pw / 2 - 17;
        int y = py + 52;
        switch (tab) {
            case 0 -> {
                button(left, y, cw, "Main menu: " + (c.vanillaMenu ? "§7Vanilla" : "§bDonut"), b -> { c.vanillaMenu = !c.vanillaMenu; save(); });
                button(right, y, cw, "Join: " + c.joinStyle.label, b -> { c.joinStyle = c.joinStyle.next(); save(); });
                y += 24;
                button(left, y, cw, "Splash jokes: " + onOff(c.splashes), b -> { c.splashes = !c.splashes; save(); });
                button(right, y, cw, "Slideshow: " + onOff(c.backgroundSlideshow), b -> { c.backgroundSlideshow = !c.backgroundSlideshow; save(); });
                y += 24;
                button(left, y, cw, "Themed pause menu: " + onOff(c.themedPause), b -> { c.themedPause = !c.themedPause; save(); });
                button(right, y, cw, "Menu video: " + onOff(c.menuVideo), b -> { c.menuVideo = !c.menuVideo; save(); });
                y += 24;
                button(left, y, cw, "Video folder", b -> open(fr.darkerdawn.sprinkle.MenuVideo.dir()));
                button(right, y, cw, "Reload pictures", b -> {
                    fr.darkerdawn.sprinkle.MenuVideo.reload();
                    Textures.reload();
                    SkyChanger.reload();
                    note = Textures.backgrounds().size() + " background(s), sky: " + (SkyChanger.hasSky() ? "yes" : "none");
                });
                y += 30;
                note(y, "Put an .mp4 or .gif in the video folder to loop it next to Rage Quit.");
            }
            case 1 -> {
                button(left, y, cw, "Ka-ching clicks: " + onOff(c.cashClick), b -> { c.cashClick = !c.cashClick; save(); });
                button(right, y, cw, "Menu music: " + onOff(c.menuMusic), b -> { c.menuMusic = !c.menuMusic; save(); });
                y += 24;
                button(left, y, cw, "Rage Quit sound: " + onOff(c.rageQuitSound), b -> { c.rageQuitSound = !c.rageQuitSound; save(); });
                y += 30;
                note(y, "Music volume follows Minecraft's Music slider.");
            }
            case 2 -> {
                button(left, y, pw - 28, "Open the Donut Mods menu (Right Shift)", b -> client.setScreen(new ModsScreen(this)));
                y += 24;
                button(left, y, cw, "Edit Fake Scoreboard", b -> client.setScreen(new ScoreboardEditorScreen(this)));
                button(right, y, cw, "Board style: " + (c.fakeOldStyle ? "§7Old" : "§bNew"), b -> { c.fakeOldStyle = !c.fakeOldStyle; save(); });
                y += 24;
                button(left, y, cw, "Combat tag: §e" + c.combatTagSeconds + "s", b -> {
                    int[] opts = {10, 15, 20, 30};
                    int next = opts[0];
                    for (int i = 0; i < opts.length; i++) if (opts[i] == c.combatTagSeconds) next = opts[(i + 1) % opts.length];
                    c.combatTagSeconds = next;
                    save();
                });
                button(right, y, cw, "Alarm radius: §e" + c.baseAlarmRadius + "m", b -> {
                    int[] opts = {32, 64, 96, 128, 192};
                    int next = opts[0];
                    for (int i = 0; i < opts.length; i++) if (opts[i] == c.baseAlarmRadius) next = opts[(i + 1) % opts.length];
                    c.baseAlarmRadius = next;
                    save();
                });
                y += 24;
                button(left, y, cw, "Death arrow: " + onOff(c.showDeathWaypoint), b -> { c.showDeathWaypoint = !c.showDeathWaypoint; save(); });
                button(right, y, cw, "Clear saved deaths", b -> { c.deaths.clear(); Config.save(); note = "Deaths cleared."; });
                y += 30;
                note(y, "Spawner prices are your own guesses: edit them in config/sprinkleclient.json.");
            }
            default -> {
                TextFieldWidget ip = new TextFieldWidget(textRenderer, right, y, cw, 18, Text.literal("Server IP"));
                ip.setMaxLength(100);
                ip.setText(c.serverIp);
                ip.setChangedListener(s -> { if (!s.isBlank()) { c.serverIp = s.trim(); Config.save(); } });
                addDrawableChild(ip);
                labels.add(new Object[]{left, y + 5, "Server IP"});
                y += 22;
                TextFieldWidget dc = new TextFieldWidget(textRenderer, right, y, cw, 18, Text.literal("Discord link"));
                dc.setMaxLength(200);
                dc.setText(c.discordUrl);
                dc.setChangedListener(s -> { if (s.startsWith("http")) { c.discordUrl = s.trim(); Config.save(); } });
                addDrawableChild(dc);
                labels.add(new Object[]{left, y + 5, "Discord link"});
                y += 22;
                TextFieldWidget key = new TextFieldWidget(textRenderer, right, y, cw, 18, Text.literal("DonutSMP API key"));
                key.setMaxLength(200);
                key.setText(c.donutApiKey == null ? "" : c.donutApiKey);
                key.setChangedListener(s -> { c.donutApiKey = s.trim(); Config.save(); });
                addDrawableChild(key);
                labels.add(new Object[]{left, y + 5, "API key (/api)"});
                y += 26;
                button(left, y, cw, "Backgrounds folder", b -> open(Textures.backgroundsDir()));
                button(right, y, cw, "Sky folder", b -> open(SkyChanger.dir()));
                y += 24;
                button(left, y, cw, "Logo folder", b -> open(Config.dir()));
                button(right, y, cw, "Controls / keybinds", b -> client.setScreen(new net.minecraft.client.gui.screen.option.KeybindsScreen(this, client.options)));
                y += 30;
                note(y, "Put logo.png in the logo folder to replace the DONUT SMP logo.");
            }
        }
        button(width / 2 - 50, py + ph - 26, 100, "Done", b -> close());
    }

    private final java.util.List<Object[]> labels = new java.util.ArrayList<>();
    private int noteY;
    private String hint = "";

    private void note(int y, String text) {
        noteY = y;
        hint = text;
    }

    private void save() {
        Config.save();
        clearAndInit();
    }

    private void open(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (Exception ignored) {
        }
        Util.getOperatingSystem().open(dir.toFile());
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        for (Object[] l : labels) g.drawTextWithShadow(textRenderer, (String) l[2], (int) l[0], (int) l[1], Theme.MUTED);
        if (!hint.isEmpty()) g.drawCenteredTextWithShadow(textRenderer, hint, width / 2, noteY, Theme.MUTED);
        if (!note.isEmpty()) g.drawCenteredTextWithShadow(textRenderer, note, width / 2, noteY + 12, Theme.GOLD);
    }
}
