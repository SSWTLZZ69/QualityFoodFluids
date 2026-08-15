package io.github.createdelight.qualityfoodfluids.mixin;

import de.cadentem.quality_food.data.QFItemTags;
import de.cadentem.quality_food.util.Utils;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Utils.class, remap = false)
public abstract class QualityFoodUtilsMixin {
    @Inject(
            method = "isValidItem(Lnet/minecraft/world/item/ItemStack;Z)Z",
            at = @At("RETURN"),
            cancellable = true
    )
    private static void qualityFoodFluids$allowDrinksAndQualityFluidBuckets(
            ItemStack stack,
            boolean checkBlock,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (callback.getReturnValue() || stack.is(QFItemTags.BLACKLIST)) {
            return;
        }

        if (QualityFoodFluidsApi.isAutomaticallyQualityCapableItem(stack)) {
            callback.setReturnValue(true);
        }
    }
}
