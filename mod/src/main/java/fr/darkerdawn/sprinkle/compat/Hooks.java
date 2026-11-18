package fr.darkerdawn.sprinkle.compat;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.SkyChanger;
import fr.darkerdawn.sprinkle.Sounds;
import fr.darkerdawn.sprinkle.feature.FakeScoreboard;
import fr.darkerdawn.sprinkle.feature.Hud;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.Identifier;

/** HUD + world rendering registration (this copy: 1.21.11). */
public final class Hooks {
    private Hooks() {}

    public static void register() {
        HudElementRegistry.addLast(Sounds.id("donut_mods"), (g, tick) -> Hud.render(g));
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> SkyChanger.render(ctx.matrices(), ctx.consumers()));
    }

    public static RenderLayer skyLayer(Identifier id) {
        return RenderLayers.text(id);
    }
}
