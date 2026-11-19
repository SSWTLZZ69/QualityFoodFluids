package io.github.createdelight.qualityfoodfluids.mixin;

import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.util.FluidQualityNames;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void qualityFoodFluids$prefixContainedQuality(CallbackInfoReturnable<Component> callback) {
        ItemStack stack = (ItemStack) (Object) this;

        if (FluidQualityNames.shouldPrefixItemName(stack)) {
            callback.setReturnValue(FluidQualityNames.withQuality(callback.getReturnValue(),
                    QualityFoodFluidsApi.getItemOrContainedFluidQuality(stack)));
        }
    }
}
