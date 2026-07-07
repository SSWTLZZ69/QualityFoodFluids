package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.fluids.transfer.FluidFillingBehaviour;
import io.github.createdelight.qualityfoodfluids.api.HosePulleyWorldQualityBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidFillingBehaviour.class, remap = false)
public abstract class FluidFillingBehaviourMixin {
    @Inject(
            method = "tryDeposit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z", shift = At.Shift.AFTER)
    )
    private void qualityFoodFluids$storePlacedFluidQuality(Fluid fluid, BlockPos root, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        if (!simulate) {
            Level world = ((FluidFillingBehaviour) (Object) this).getWorld();
            findNearbyPlacedSource(world, root, fluid);
        }
    }

    private void findNearbyPlacedSource(Level world, BlockPos root, Fluid fluid) {
        if (world == null || root == null || fluid == null) {
            return;
        }

        if (world.getFluidState(root).isSource() && world.getFluidState(root).getType().isSame(fluid)) {
            HosePulleyWorldQualityBridge.handlePlacedSource(world, root);
            return;
        }

        BlockPos.betweenClosedStream(root.offset(-1, -1, -1), root.offset(1, 1, 1))
                .filter(pos -> world.getFluidState(pos).isSource())
                .filter(pos -> world.getFluidState(pos).getType().isSame(fluid))
                .findFirst()
                .ifPresent(pos -> HosePulleyWorldQualityBridge.handlePlacedSource(world, pos.immutable()));
    }
}
