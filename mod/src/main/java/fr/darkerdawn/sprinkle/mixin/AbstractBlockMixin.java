package fr.darkerdawn.sprinkle.mixin;

import fr.darkerdawn.sprinkle.feature.AntiLeak;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Anti-Leak: same model rotation everywhere for the listed blocks. */
@Mixin(AbstractBlock.class)
public abstract class AbstractBlockMixin {
    @Inject(method = "getRenderingSeed", at = @At("HEAD"), cancellable = true)
    private void sprinkle$antiLeak(BlockState state, BlockPos pos, CallbackInfoReturnable<Long> cir) {
        if ((Object) this instanceof Block b && AntiLeak.covers(b)) cir.setReturnValue(0L);
    }
}
