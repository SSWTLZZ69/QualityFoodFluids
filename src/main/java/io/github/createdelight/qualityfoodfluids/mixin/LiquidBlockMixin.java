package io.github.createdelight.qualityfoodfluids.mixin;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.world.WorldFluidQualityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LiquidBlock.class)
public abstract class LiquidBlockMixin {
    private Quality qualityFoodFluids$pickedUpQuality = Quality.NONE;

    @Inject(method = "pickupBlock", at = @At("HEAD"))
    private void qualityFoodFluids$capturePickedUpQuality(LevelAccessor level, BlockPos pos, BlockState state, CallbackInfoReturnable<ItemStack> callback) {
        qualityFoodFluids$pickedUpQuality = QualityFoodFluidsApi.getWorldQuality(level, pos);
    }

    @Inject(method = "pickupBlock", at = @At("RETURN"), cancellable = true)
    private void qualityFoodFluids$applyPickedUpQuality(LevelAccessor level, BlockPos pos, BlockState state, CallbackInfoReturnable<ItemStack> callback) {
        ItemStack result = callback.getReturnValue();

        if (result.isEmpty()) {
            qualityFoodFluids$pickedUpQuality = Quality.NONE;
            return;
        }

        Quality quality = qualityFoodFluids$pickedUpQuality;
        qualityFoodFluids$pickedUpQuality = Quality.NONE;

        if (quality.level() <= 0) {
            return;
        }

        callback.setReturnValue(QualityFoodFluidsApi.applyItemQuality(result, quality));
        WorldFluidQualityData.clear(level, pos);
    }
}
