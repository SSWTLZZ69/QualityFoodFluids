package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.simibubi.create.content.fluids.transfer.FluidManipulationBehaviour.BlockPosEntry;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import it.unimi.dsi.fastutil.PriorityQueue;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidDrainingBehaviour.class, remap = false)
public abstract class FluidDrainingBehaviourMixin {
    @Shadow
    PriorityQueue<BlockPosEntry> queue;

    @Inject(method = "getDrainableFluid", at = @At("RETURN"), cancellable = true)
    private void qualityFoodFluids$copyWorldQualityToDrainedFluid(BlockPos rootPos, CallbackInfoReturnable<FluidStack> callback) {
        FluidStack result = callback.getReturnValue();
        FluidDrainingBehaviour self = (FluidDrainingBehaviour) (Object) this;

        if (result.isEmpty() || self.isInfinite() || queue.isEmpty()) {
            return;
        }

        BlockPos drainedPos = queue.first().pos();
        callback.setReturnValue(QualityFoodFluidsApi.copyWorldQualityToFluid(self.getWorld(), drainedPos, result));
    }
}
