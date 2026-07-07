package io.github.createdelight.qualityfoodfluids.mixin;

import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.RecipeMatcher;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.brewinandchewin.common.crafting.KegFermentingRecipe;
import umpaz.brewinandchewin.common.utility.KegRecipeWrapper;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = KegFermentingRecipe.class, remap = false)
public abstract class BrewinKegFermentingRecipeMixin {
    @Shadow
    @Final
    private NonNullList<Ingredient> inputItems;

    @Shadow
    @Final
    private FluidStack fluidIngredient;

    @Shadow
    @Final
    private int amount;

    @Inject(method = "matches(Lumpaz/brewinandchewin/common/utility/KegRecipeWrapper;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"), cancellable = true)
    private void qualityFoodFluids$matchIgnoringFluidQuality(KegRecipeWrapper wrapper, Level level, CallbackInfoReturnable<Boolean> callback) {
        List<ItemStack> inputs = new ArrayList<>();
        int inputCount = 0;

        for (int slot = 0; slot < 4; slot++) {
            ItemStack stack = wrapper.getItem(slot);
            if (!stack.isEmpty()) {
                inputCount++;
                inputs.add(stack);
            }
        }

        if (inputCount != inputItems.size() || RecipeMatcher.findMatches(inputs, inputItems) == null) {
            callback.setReturnValue(false);
            return;
        }

        FluidStack tankFluid = wrapper.getFluid(0);

        if (fluidIngredient == null) {
            callback.setReturnValue(tankFluid.isEmpty());
            return;
        }

        callback.setReturnValue(!tankFluid.isEmpty()
                && QualityFoodFluidsApi.isFluidEqualIgnoringQuality(tankFluid, fluidIngredient)
                && tankFluid.getAmount() % amount == 0);
    }
}
