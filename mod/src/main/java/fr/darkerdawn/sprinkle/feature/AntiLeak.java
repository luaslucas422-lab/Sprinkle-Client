package fr.darkerdawn.sprinkle.feature;

import fr.darkerdawn.sprinkle.Config;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

/**
 * Anti-Leak: blocks like stone, grass and dirt normally get a random rotation from their position,
 * which people use to find bases from screenshots. For the blocks in your list we always use the same rotation.
 */
public final class AntiLeak {
    private AntiLeak() {}

    private static volatile Set<Block> blocks;

    public static boolean covers(Block b) {
        if (!Config.get().antiLeak) return false;
        Set<Block> s = blocks;
        if (s == null) s = rebuild();
        return s.contains(b);
    }

    private static Set<Block> rebuild() {
        Set<Block> s = new HashSet<>();
        for (String id : Config.get().antiLeakBlocks) {
            Identifier i = Identifier.tryParse(id.contains(":") ? id : "minecraft:" + id);
            if (i != null && Registries.BLOCK.containsId(i)) s.add(Registries.BLOCK.get(i));
        }
        blocks = s;
        return s;
    }

    public static String normalize(String id) {
        String t = id.trim().toLowerCase().replace(' ', '_');
        return t.contains(":") ? t : "minecraft:" + t;
    }

    public static boolean valid(String id) {
        Identifier i = Identifier.tryParse(normalize(id));
        return i != null && Registries.BLOCK.containsId(i);
    }

    /** Call after the list or the switch changes: rebuilds the chunks so you see it straight away. */
    public static void changed() {
        Config.save();
        rebuild();
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.worldRenderer != null && c.world != null) c.worldRenderer.reload();
    }
}
