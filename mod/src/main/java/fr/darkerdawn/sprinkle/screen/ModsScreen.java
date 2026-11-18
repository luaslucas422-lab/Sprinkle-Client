package fr.darkerdawn.sprinkle.screen;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.SkyChanger;
import fr.darkerdawn.sprinkle.Theme;
import fr.darkerdawn.sprinkle.feature.Features;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

/** The Donut mods popup (Right Shift): switch each one on/off, see its keybind and what it does. */
public class ModsScreen extends SprinkleScreen {
    private static final int ROW = 24;
    private int page, perPage, top;
    private String note = "";

    public ModsScreen(Screen parent) {
        super("Donut Mods", parent);
    }

    @Override
    protected void init() {
        layoutPanel(400, 300);
        top = py + 26;
        perPage = Math.max(3, (ph - 26 - 34) / ROW);
        int pages = (Features.ALL.size() + perPage - 1) / perPage;
        page = Math.min(page, pages - 1);
        for (int i = page * perPage; i < Math.min(Features.ALL.size(), (page + 1) * perPage); i++) {
            Features.Feature f = Features.ALL.get(i);
            int y = top + (i - page * perPage) * ROW;
            button(px + 8, y, 44, onOff(f.on()), b -> { f.toggle(); clearAndInit(); });
            int ex = px + pw - 64;
            switch (f.id) {
                case "fake_scoreboard" -> button(ex, y, 56, "Edit", b -> client.setScreen(new ScoreboardEditorScreen(this)));
                case "base_alarm" -> button(ex, y, 56, "Set base", b -> setBase());
                case "anti_leak" -> button(ex, y, 56, "Blocks", b -> client.setScreen(new AntiLeakScreen(this)));
                case "sky_changer" -> button(ex, y, 56, "Folder", b -> Util.getOperatingSystem().open(SkyChanger.dir().toFile()));
                default -> { }
            }
        }
        int by = py + ph - 26;
        if (pages > 1) {
            button(px + 8, by, 20, "<", b -> { page = (page + pages - 1) % pages; clearAndInit(); });
            button(px + 30, by, 20, ">", b -> { page = (page + 1) % pages; clearAndInit(); });
        }
        button(width / 2 - 120, by, 76, "Soundboard", b -> client.setScreen(new SoundboardScreen(this)));
        button(width / 2 - 40, by, 76, "Keys", b -> client.setScreen(new KeybindsScreen(this, client.options)));
        button(width / 2 + 40, by, 76, "Done", b -> close());
    }

    private void setBase() {
        Config c = Config.get();
        if (client.player == null || client.world == null) {
            note = "Join a world first, then stand in your base.";
            return;
        }
        c.baseX = (int) Math.floor(client.player.getX());
        c.baseZ = (int) Math.floor(client.player.getZ());
        c.baseDimension = client.world.getRegistryKey().getValue().toString();
        Config.save();
        note = c.streamerMode ? "Base saved." : "Base saved at " + c.baseX + ", " + c.baseZ + ".";
    }

    @Override
    public void render(DrawContext g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        for (int i = page * perPage; i < Math.min(Features.ALL.size(), (page + 1) * perPage); i++) {
            Features.Feature f = Features.ALL.get(i);
            int y = top + (i - page * perPage) * ROW;
            int x = px + 58;
            g.drawTextWithShadow(textRenderer, f.name, x, y + 1, f.on() ? Theme.WHITE : Theme.MUTED);
            String key = "[" + Features.keyName(f.key) + "]";
            g.drawTextWithShadow(textRenderer, key, x + textRenderer.getWidth(f.name) + 5, y + 1, Theme.DOUGH_LIGHT);
            int maxW = pw - 58 - 70;
            String desc = textRenderer.trimToWidth(f.desc, maxW);
            g.drawTextWithShadow(textRenderer, desc, x, y + 11, 0xFF8FB4DE);
        }
        if (!note.isEmpty()) g.drawCenteredTextWithShadow(textRenderer, note, width / 2, py + ph - 38, Theme.GOLD);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
