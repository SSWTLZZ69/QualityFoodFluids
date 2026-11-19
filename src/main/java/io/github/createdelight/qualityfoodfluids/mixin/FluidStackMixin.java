package io.github.createdelight.qualityfoodfluids.mixin;

import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.util.FluidQualityNames;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidStack.class, remap = false)
public abstract class FluidStackMixin {
    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void qualityFoodFluids$prefixQuality(CallbackInfoReturnable<Component> callback) {
        FluidStack stack = (FluidStack) (Object) this;

        if (QualityFoodFluidsApi.canCarryQuality(stack)) {
            callback.setReturnValue(FluidQualityNames.withQuality(
                    callback.getReturnValue(), QualityFoodFluidsApi.getQuality(stack)));
        }
    }
}
