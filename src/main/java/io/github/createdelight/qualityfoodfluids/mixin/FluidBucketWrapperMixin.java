package io.github.createdelight.qualityfoodfluids.mixin;

import de.cadentem.quality_food.util.QualityUtils;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.wrappers.FluidBucketWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidBucketWrapper.class, remap = false)
public abstract class FluidBucketWrapperMixin {
    @Shadow
    public abstract ItemStack getContainer();

    @Inject(method = "getFluid()Lnet/minecraftforge/fluids/FluidStack;", at = @At("RETURN"), cancellable = true)
    private void qualityFoodFluids$copyBucketQualityToFluid(CallbackInfoReturnable<FluidStack> callback) {
        FluidStack result = callback.getReturnValue();

        if (!QualityFoodFluidsApi.canCarryQuality(result)) {
            return;
        }

        // Querying the container's fluid capability here would recurse into getFluid().
        callback.setReturnValue(QualityFoodFluidsApi.withQuality(result, QualityUtils.getQuality(getContainer())));
    }
}
