package io.github.createdelight.qualityfoodfluids.mixin;

import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermenterBlockEntity;
import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermentingRecipe;
import io.github.createdelight.qualityfoodfluids.internal.BulkFermenterQualityContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = BulkFermentingRecipe.class, remap = false)
public abstract class CreatedieselBulkFermentingRecipeMixin {
    @Inject(method = "apply", at = @At("HEAD"))
    private void qualityFoodFluids$beginBulkFermenting(BulkFermenterBlockEntity fermenter, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        BulkFermenterQualityContext.begin(fermenter, (BulkFermentingRecipe) (Object) this, simulate);
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void qualityFoodFluids$endBulkFermenting(BulkFermenterBlockEntity fermenter, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        BulkFermenterQualityContext.end(fermenter, simulate, callback.getReturnValue());
    }

    @Inject(method = "applyOutputs", at = @At("HEAD"), cancellable = true)
    private void qualityFoodFluids$applyBulkFermentingOutputQuality(BulkFermenterBlockEntity fermenter, List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        Boolean result = BulkFermenterQualityContext.applyToOutputs(fermenter, itemOutputs, fluidOutputs, simulate);

        if (result != null) {
            callback.setReturnValue(result);
        }
    }
}
