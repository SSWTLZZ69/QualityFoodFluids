package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.fluids.OpenEndedPipe;
import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OpenEndedPipe.class, remap = false)
public abstract class OpenEndedPipeMixin {
    @Shadow
    private Level world;

    @Shadow
    private BlockPos outputPos;

    private Quality qualityFoodFluids$removedQuality = Quality.NONE;

    @Inject(method = "removeFluidFromSpace", at = @At("HEAD"))
    private void qualityFoodFluids$captureRemovedWorldQuality(boolean simulate, CallbackInfoReturnable<FluidStack> callback) {
        qualityFoodFluids$removedQuality = world == null || outputPos == null
                ? Quality.NONE
                : QualityFoodFluidsApi.getWorldQuality(world, outputPos);
    }

    @Inject(method = "removeFluidFromSpace", at = @At("RETURN"), cancellable = true)
    private void qualityFoodFluids$applyRemovedWorldQuality(boolean simulate, CallbackInfoReturnable<FluidStack> callback) {
        FluidStack result = callback.getReturnValue();
        Quality quality = qualityFoodFluids$removedQuality;
        qualityFoodFluids$removedQuality = Quality.NONE;

        if (result.isEmpty() || quality.level() <= 0) {
            return;
        }

        callback.setReturnValue(QualityFoodFluidsApi.withQuality(result, quality));

        if (!simulate && world != null && outputPos != null) {
            QualityFoodFluidsApi.clearWorldQuality(world, outputPos);
        }
    }

    @Inject(method = "provideFluidToSpace", at = @At("RETURN"))
    private void qualityFoodFluids$storeProvidedWorldQuality(FluidStack fluid, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        if (simulate || !callback.getReturnValue() || world == null || outputPos == null || fluid.isEmpty()) {
            return;
        }

        if (!world.getFluidState(outputPos).isSource() || !world.getFluidState(outputPos).getType().isSame(fluid.getFluid())) {
            return;
        }

        if (QualityFoodFluidsApi.canHaveWorldQuality(fluid) && QualityFoodFluidsApi.hasQuality(fluid)) {
            QualityFoodFluidsApi.copyFluidQualityToWorld(fluid, world, outputPos);
            return;
        }

        QualityFoodFluidsApi.clearWorldQuality(world, outputPos);
    }
}
