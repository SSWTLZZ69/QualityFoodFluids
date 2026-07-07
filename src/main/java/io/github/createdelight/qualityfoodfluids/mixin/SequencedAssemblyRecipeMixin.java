package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import io.github.createdelight.qualityfoodfluids.internal.SequencedAssemblyQualityContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SequencedAssemblyRecipe.class, remap = false)
public abstract class SequencedAssemblyRecipeMixin {
    @Inject(method = "advance", at = @At("RETURN"), cancellable = true)
    private void qualityFoodFluids$applySequencedAssemblyQuality(ItemStack input, CallbackInfoReturnable<ItemStack> callback) {
        callback.setReturnValue(SequencedAssemblyQualityContext.applyAdvanceQuality(input, callback.getReturnValue()));
    }
}
