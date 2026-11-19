package io.github.createdelight.qualityfoodfluids.mixin;

import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(value = FluidUtil.class, remap = false)
public abstract class FluidUtilMixin {
    @Inject(method = "getFluidContained(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;", at = @At("RETURN"), cancellable = true)
    private static void qualityFoodFluids$copyContainerQualityToDisplayedFluid(
            ItemStack container, CallbackInfoReturnable<Optional<FluidStack>> callback
    ) {
        // JEI and other tooltip consumers read this fluid separately from the
        // item name. Only drinks and real fluid buckets may supply a missing
        // grade; storage devices keep each returned fluid's own metadata.
        callback.setReturnValue(callback.getReturnValue()
                .map(fluid -> QualityFoodFluidsApi.copyContainerQualityToFluid(container, fluid)));
    }

    @Inject(method = "getFilledBucket(Lnet/minecraftforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private static void qualityFoodFluids$copyFluidQualityToBucket(FluidStack inputFluid, CallbackInfoReturnable<ItemStack> callback) {
        ItemStack result = callback.getReturnValue();

        if (result.isEmpty() || !QualityFoodFluidsApi.canCarryQuality(inputFluid)) {
            return;
        }

        // QF preserves an existing higher grade when applying quality; replace it with the fluid's exact grade.
        ItemStack resultCopy = QualityFoodFluidsApi.clearItemQuality(result);
        callback.setReturnValue(QualityFoodFluidsApi.copyFluidQualityToItem(inputFluid, resultCopy));
    }
}
