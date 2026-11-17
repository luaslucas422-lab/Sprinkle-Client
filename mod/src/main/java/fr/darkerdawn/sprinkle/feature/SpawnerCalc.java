package fr.darkerdawn.sprinkle.feature;

import fr.darkerdawn.sprinkle.Config;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Spawner Calculator: shows an estimated value on spawner tooltips (prices are your own, in the config). */
public final class SpawnerCalc {
    private SpawnerCalc() {}

    public static double priceOf(ItemStack stack) {
        String name = stack.getName().getString().toLowerCase(Locale.ROOT);
        Map<String, Double> prices = Config.get().spawnerPrices;
        String best = null;
        for (String key : prices.keySet()) {
            if (key.equals("default")) continue;
            String k = key.replace('_', ' ');
            if (name.contains(k) && (best == null || k.length() > best.length())) best = key;
        }
        Double p = best != null ? prices.get(best) : prices.get("default");
        return p == null ? 0 : p;
    }

    public static void tooltip(ItemStack stack, List<Text> lines) {
        if (!Config.get().spawnerCalc || !stack.isOf(Items.SPAWNER)) return;
        double each = priceOf(stack);
        int n = stack.getCount();
        lines.add(Text.literal("§9[Sprinkle] §6≈ " + Money.format(each) + " each" + (n > 1 ? " §7(" + Money.format(each * n) + " for " + n + ")" : "")));
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player != null) {
            double total = 0;
            int count = 0;
            for (int i = 0; i < c.player.getInventory().size(); i++) {
                ItemStack s = c.player.getInventory().getStack(i);
                if (s.isOf(Items.SPAWNER)) {
                    total += priceOf(s) * s.getCount();
                    count += s.getCount();
                }
            }
            if (count > n) lines.add(Text.literal("§9[Sprinkle] §7All " + count + " spawners: §6≈ " + Money.format(total)));
        }
    }
}
