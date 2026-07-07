package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.fluids.spout.FillingBySpout;
import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsCreateRules;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.internal.SequencedAssemblyQualityContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FillingBySpout.class, remap = false)
public abstract class FillingBySpoutMixin {
    private static final ThreadLocal<Quality> QUALITY_FOOD_FLUIDS$FILLING_QUALITY = ThreadLocal.withInitial(() -> Quality.NONE);

    @Inject(method = "fillItem", at = @At("HEAD"))
    private static void qualityFoodFluids$captureFillingQuality(Level world, int requiredAmount, ItemStack stack, FluidStack availableFluid, CallbackInfoReturnable<ItemStack> callback) {
        QUALITY_FOOD_FLUIDS$FILLING_QUALITY.set(QualityFoodFluidsApi.getQuality(availableFluid));
        SequencedAssemblyQualityContext.captureFillingSource(availableFluid);
    }

    @Inject(method = "fillItem", at = @At("RETURN"), cancellable = true)
    private static void qualityFoodFluids$applyFillingQuality(Level world, int requiredAmount, ItemStack stack, FluidStack availableFluid, CallbackInfoReturnable<ItemStack> callback) {
        Quality quality = QUALITY_FOOD_FLUIDS$FILLING_QUALITY.get();
        QUALITY_FOOD_FLUIDS$FILLING_QUALITY.remove();
        if (SequencedAssemblyQualityContext.consumeFillingUsedBySequence()) {
            return;
        }
        callback.setReturnValue(QualityFoodFluidsCreateRules.applyFillingQuality(quality, callback.getReturnValue()));
    }
}
