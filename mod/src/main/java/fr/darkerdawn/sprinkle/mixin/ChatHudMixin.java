package fr.darkerdawn.sprinkle.mixin;

import fr.darkerdawn.sprinkle.Config;
import fr.darkerdawn.sprinkle.feature.Coords;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Streamer mode: coordinates in chat become ???. */
@Mixin(ChatHud.class)
public abstract class ChatHudMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), argsOnly = true)
    private Text sprinkle$hideCoords(Text message) {
        if (!Config.get().streamerMode || message == null) return message;
        String s = message.getString();
        if (!Coords.hasCoords(s)) return message;
        return Text.literal(Coords.censor(s));
    }
}
