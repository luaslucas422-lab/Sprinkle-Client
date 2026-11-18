package fr.darkerdawn.sprinkle.mixin;

import fr.darkerdawn.sprinkle.feature.Tooltips;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Shulker Preview and Map Preview: give these items a picture tooltip. */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getTooltipData", at = @At("HEAD"), cancellable = true)
    private void sprinkle$tooltipData(CallbackInfoReturnable<Optional<TooltipData>> cir) {
        Optional<TooltipData> mine = Tooltips.data((ItemStack) (Object) this);
        if (mine != null) cir.setReturnValue(mine);
    }
}
