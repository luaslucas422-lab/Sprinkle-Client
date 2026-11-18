package fr.darkerdawn.sprinkle.feature;

import fr.darkerdawn.sprinkle.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a made-up Donut-style sidebar instead of the server's one.
 * Only on your screen: turning it off brings the real scoreboard straight back.
 */
public final class FakeScoreboard {
    private FakeScoreboard() {}

    public static final String WARNING = "§c§lThese are not real stats";

    public static void render(DrawContext g) {
        MinecraftClient c = MinecraftClient.getInstance();
        TextRenderer tr = c.textRenderer;
        Config cfg = Config.get();
        Config.FakeBoard f = cfg.fake;
        boolean old = cfg.fakeOldStyle;

        String title;
        List<String[]> rows = new ArrayList<>(); // left, right
        if (old) {
            title = "§b§l" + f.title;
            rows.add(new String[]{"§7Name: §f" + f.name, ""});
            rows.add(new String[]{"§7Money: §a" + f.money, ""});
            rows.add(new String[]{"§7Shards: §d" + f.shards, ""});
            rows.add(new String[]{"§7Kills: §c" + f.kills, ""});
            rows.add(new String[]{"§7Deaths: §c" + f.deaths, ""});
            rows.add(new String[]{"§7Playtime: §e" + f.playtime, ""});
            rows.add(new String[]{"§7Team: §b" + f.team, ""});
            rows.add(new String[]{"§7" + f.footer, ""});
            for (int i = 0; i < rows.size(); i++) rows.get(i)[1] = "§c" + (rows.size() - i);
        } else {
            title = "§f§l" + f.title;
            rows.add(new String[]{"§f" + f.name, ""});
            rows.add(new String[]{"", ""});
            rows.add(new String[]{"§a$ §7Money", "§a" + f.money});
            rows.add(new String[]{"§d★ §7Shards", "§d" + f.shards});
            rows.add(new String[]{"§c⚔ §7Kills", "§c" + f.kills});
            rows.add(new String[]{"§4☠ §7Deaths", "§4" + f.deaths});
            rows.add(new String[]{"§e⌛ §7Keyall", "§e" + f.keyall});
            rows.add(new String[]{"§6⌚ §7Playtime", "§6" + f.playtime});
            rows.add(new String[]{"§b⚑ §7Team", "§b" + f.team});
            rows.add(new String[]{"", ""});
            rows.add(new String[]{"§9" + f.footer, ""});
        }
        rows.add(new String[]{WARNING, ""});

        int width = tr.getWidth(title);
        for (String[] r : rows) width = Math.max(width, tr.getWidth(r[0]) + (r[1].isEmpty() ? 0 : tr.getWidth(r[1]) + 10));
        int lh = 9;
        int height = rows.size() * lh;
        int sw = g.getScaledWindowWidth(), sh = g.getScaledWindowHeight();
        int bottom = sh / 2 + height / 3;
        int right = sw - 3;
        int left = right - width - 2;
        int top = bottom - height;

        g.fill(left - 2, top - lh - 1, right, top - 1, old ? 0x66000000 : 0xCC0256C4);
        g.fill(left - 2, top - 1, right, bottom, old ? 0x4C000000 : 0x99020B35);
        g.drawText(tr, title, left + width / 2 - tr.getWidth(title) / 2, top - lh, 0xFFFFFFFF, false);
        for (int i = 0; i < rows.size(); i++) {
            int y = top + i * lh;
            String[] r = rows.get(i);
            boolean warn = r[0].equals(WARNING);
            int x = warn ? left + width / 2 - tr.getWidth(r[0]) / 2 : left;
            g.drawText(tr, r[0], x, y, 0xFFFFFFFF, false);
            if (!r[1].isEmpty()) g.drawText(tr, r[1], right - tr.getWidth(r[1]), y, 0xFFFFFFFF, false);
        }
    }
}
