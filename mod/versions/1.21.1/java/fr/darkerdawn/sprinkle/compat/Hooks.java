package fr.darkerdawn.sprinkle.compat;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.SkyChanger;
import fr.darkerdawn.sprinkle.Sounds;
import fr.darkerdawn.sprinkle.feature.FakeScoreboard;
import fr.darkerdawn.sprinkle.feature.Hud;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.util.Identifier;

/** HUD + world rendering registration (this copy: 1.21 / 1.21.1). */
public final class Hooks {
    private Hooks() {}

    public static void register() {
        HudRenderCallback.EVENT.register((g, tick) -> Hud.render(g));
        WorldRenderEvents.BEFORE_ENTITIES.register(ctx -> {
            MatrixStack m = new MatrixStack();
            m.multiplyPositionMatrix(ctx.positionMatrix());
            SkyChanger.render(m, ctx.consumers());
        });
    }

    public static RenderLayer skyLayer(Identifier id) {
        return RenderLayer.getText(id);
    }
}
