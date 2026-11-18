package fr.darkerdawn.sprinkle;

import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;

/** Turns Sprinkle's built-in resource packs on and off (Anti-Leak, Donut SMP, Blue Buttons). */
public final class Packs {
    private Packs() {}

    private static String find(ResourcePackManager m, String path) {
        for (String id : m.getIds()) if (id.endsWith(path)) return id;
        return null;
    }

    public static boolean enabled(String path) {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c == null || c.getResourcePackManager() == null) return false;
        String id = find(c.getResourcePackManager(), path);
        return id != null && c.getResourcePackManager().getEnabledIds().contains(id);
    }

    public static void set(String path, boolean on) {
        MinecraftClient c = MinecraftClient.getInstance();
        ResourcePackManager m = c.getResourcePackManager();
        String id = find(m, path);
        if (id == null || enabled(path) == on) return;
        if (on) m.enable(id); else m.disable(id);
        c.options.refreshResourcePacks(m);
    }
}
