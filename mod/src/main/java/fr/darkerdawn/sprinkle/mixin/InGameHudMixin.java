package fr.darkerdawn.sprinkle.mixin;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.feature.FakeScoreboard;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fake Scoreboard: draws ours instead of the server's sidebar (same on every version). */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V", at = @At("HEAD"), cancellable = true)
    private void sprinkle$fakeBoard(DrawContext g, RenderTickCounter tick, CallbackInfo ci) {
        if (Config.get().fakeScoreboard) {
            FakeScoreboard.render(g);
            ci.cancel();
        }
    }
}
