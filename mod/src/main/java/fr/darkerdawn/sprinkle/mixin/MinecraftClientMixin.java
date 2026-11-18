package fr.darkerdawn.sprinkle.mixin;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.Sounds;
import fr.darkerdawn.sprinkle.screen.SprinkleTitleScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.sound.MusicSound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @Shadow public Screen currentScreen;

    /** Swap the vanilla title screen for the Donut SMP one. */
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true, require = 1)
    private Screen sprinkle$title(Screen screen) {
        if (screen instanceof TitleScreen && !Config.get().vanillaMenu) return new SprinkleTitleScreen();
        return screen;
    }

    /** Our own menu music on the Donut menu. */
    @Inject(method = {"getMusicInstance", "getMusicType"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void sprinkle$music(CallbackInfoReturnable<Object> cir) {
        if (currentScreen instanceof SprinkleTitleScreen && Config.get().menuMusic) cir.setReturnValue(fr.darkerdawn.sprinkle.compat.Compat.menuMusic());
    }

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true)
    private void sprinkle$windowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Sprinkle Client - " + cir.getReturnValue());
    }
}
