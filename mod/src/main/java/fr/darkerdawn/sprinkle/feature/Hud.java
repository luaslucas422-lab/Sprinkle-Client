package fr.darkerdawn.sprinkle.feature;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.Textures;
import fr.darkerdawn.sprinkle.Theme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.Map;

/** Draws the Donut mods on screen while you play. */
public final class Hud {
    private Hud() {}

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static void render(DrawContext g, RenderTickCounter tick) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.options.hudHidden) return;
        Config cfg = Config.get();
        TextRenderer tr = c.textRenderer;
        int w = g.getScaledWindowWidth(), h = g.getScaledWindowHeight();
        long now = System.currentTimeMillis();
        int y = 4;

        if (cfg.streamerMode) {
            box(g, tr, 4, y, "§c● §fSTREAMER MODE");
            y += 14;
        }

        if (cfg.moneyHud && Tracker.money >= 0) {
            String line = "§a" + Money.format(Tracker.money);
            if (cfg.moneyDayStart >= 0) {
                double diff = Tracker.money - cfg.moneyDayStart;
                line += diff >= 0 ? "  §a+" + Money.format(diff) + " today" : "  §c" + Money.format(diff) + " today";
            }
            box(g, tr, 4, y, line);
            y += 14;
        }

        if (cfg.pvpHud) {
            long left = Tracker.combatUntil - now;
            if (left > 0) {
                box(g, tr, 4, y, "§c⚔ In combat: §f" + String.format("%.1fs", left / 1000.0));
                y += 14;
            }
            for (Map.Entry<String, Integer> e : Tracker.pops.entrySet()) {
                box(g, tr, 4, y, "§e✦ §f" + e.getKey() + " §7popped §e" + e.getValue() + (e.getValue() == 1 ? " totem" : " totems"));
                y += 12;
            }
            // armour durability, bottom left above the chat line
            int ay = h - 62;
            for (EquipmentSlot slot : ARMOR) {
                ItemStack s = c.player.getEquippedStack(slot);
                if (s.isEmpty()) continue;
                g.drawItem(s, 4, ay);
                if (s.isDamageable()) {
                    int pct = Math.round(100f * (s.getMaxDamage() - s.getDamage()) / s.getMaxDamage());
                    int col = pct > 50 ? Theme.GREEN : pct > 20 ? Theme.GOLD : Theme.RED;
                    g.drawTextWithShadow(tr, pct + "%", 22, ay + 4, col);
                }
                ay -= 17;
            }
        }

        Config.DeathSaverRecord d = Tracker.lastDeathHere(c);
        if (d != null && cfg.showDeathWaypoint) {
            double dx = d.x + 0.5 - c.player.getX(), dz = d.z + 0.5 - c.player.getZ();
            int dist = (int) Math.sqrt(dx * dx + dz * dz);
            float target = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float rel = MathHelper.wrapDegrees(target - c.player.getYaw());
            String[] arrows = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
            String arrow = arrows[Math.floorMod(Math.round(rel / 45f), 8)];
            String where = cfg.streamerMode ? "" : " §7(" + d.x + " " + d.y + " " + d.z + ")";
            String text = "§c☠ §fLast death: §e" + dist + "m " + arrow + where;
            box(g, tr, w / 2 - tr.getWidth(text) / 2 - 3, 4, text);
        }

        if (now < Tracker.flexUntil) drawFlex(g, tr, w, h, now);
    }

    private static void box(DrawContext g, TextRenderer tr, int x, int y, String text) {
        int tw = tr.getWidth(text);
        g.fill(x - 2, y - 2, x + tw + 3, y + 10, 0x99020B35);
        g.fill(x - 2, y - 2, x - 1, y + 10, Theme.FROSTING);
        g.drawTextWithShadow(tr, text, x + 1, y, 0xFFFFFFFF);
    }

    private static void drawFlex(DrawContext g, TextRenderer tr, int w, int h, long now) {
        float left = (Tracker.flexUntil - now) / 5000f; // 1 -> 0
        float alpha = Math.min(1f, left * 3f);
        long seed = Tracker.flexUntil;
        for (int i = 0; i < 60; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int x = (int) Math.floorMod(seed >>> 20, (long) w);
            float fall = (1 - left) * (h + 40) * (0.6f + ((seed >>> 50) & 15) / 25f);
            int y = (int) (fall - ((seed >>> 34) & 63));
            int col = (i % 3 == 0) ? Theme.GOLD : (i % 3 == 1 ? Theme.GREEN : Theme.SPRINKLES[i % Theme.SPRINKLES.length]);
            if (i % 7 == 0) {
                Textures.draw(g, Textures.DONUT, x, y, 10, 10, Textures.DONUT_SIZE, Textures.DONUT_SIZE, Theme.withAlpha(0xFFFFFF, alpha));
            } else {
                g.fill(x, y, x + 3, y + 2, Theme.withAlpha(col, alpha));
            }
        }
        g.getMatrices().pushMatrix();
        g.getMatrices().translate(w / 2f, h / 3f);
        float s = 3.5f + (float) Math.sin(now / 90.0) * 0.25f;
        g.getMatrices().scale(s, s);
        g.drawCenteredTextWithShadow(tr, Tracker.flexTitle, 0, -4, Theme.withAlpha(Theme.GOLD, alpha));
        g.getMatrices().popMatrix();
        g.drawCenteredTextWithShadow(tr, "You have " + Money.format(Tracker.money) + ". Flex responsibly.", w / 2, (int) (h / 3f) + 18, Theme.withAlpha(0xFFFFFF, alpha));
    }
}
