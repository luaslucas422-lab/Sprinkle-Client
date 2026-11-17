package fr.darkerdawn.sprinkle.mixin;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.feature.Coords;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;

/** Streamer mode: hides coordinates on the F3 screen. */
@Mixin(DebugHud.class)
public abstract class DebugHudMixin {
    @ModifyVariable(method = "drawText", at = @At("HEAD"), argsOnly = true)
    private List<String> sprinkle$hideCoords(List<String> lines) {
        if (!Config.get().streamerMode || lines == null) return lines;
        List<String> out = new ArrayList<>(lines.size());
        for (String l : lines) out.add(l != null && Coords.hasCoords(l) ? Coords.censor(l) : l);
        return out;
    }
}
