package io.github.createdelight.qualityfoodfluids.mixin;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.compat.jei.JeiQualityComparison;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.library.ingredients.subtypes.SubtypeManager;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SubtypeManager.class, remap = false)
public abstract class JeiSubtypeManagerMixin {
    @ModifyVariable(
            method = "getSubtypeInfo(Lmezz/jei/api/ingredients/IIngredientTypeWithSubtypes;Ljava/lang/Object;Lmezz/jei/api/ingredients/subtypes/UidContext;)Ljava/lang/String;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0
    )
    private Object qualityFoodFluids$normalizeRecipeLookup(
            Object ingredient, IIngredientTypeWithSubtypes<?, ?> ingredientType,
            Object originalIngredient, UidContext context
    ) {
        // Normalize before any native interpreter sees the stack, including
        // interpreters that use the full fluid/item NBT for recipe matching.
        return context == UidContext.Recipe ? JeiQualityComparison.withoutQuality(ingredient) : ingredient;
    }

    @Inject(
            method = "getSubtypeInfo(Lmezz/jei/api/ingredients/IIngredientTypeWithSubtypes;Ljava/lang/Object;Lmezz/jei/api/ingredients/subtypes/UidContext;)Ljava/lang/String;",
            at = @At("RETURN"),
            cancellable = true
    )
    private void qualityFoodFluids$keepQualitySubtype(
            IIngredientTypeWithSubtypes<?, ?> ingredientType,
            Object ingredient,
            UidContext context,
            CallbackInfoReturnable<String> callback
    ) {
        if (context == UidContext.Recipe) {
            return;
        }

        Quality quality;

        if (ingredient instanceof FluidStack fluid && QualityFoodFluidsApi.canCarryQuality(fluid)) {
            quality = QualityFoodFluidsApi.getQuality(fluid);
        } else if (ingredient instanceof ItemStack item && QualityFoodFluidsApi.isAutomaticallyQualityCapableItem(item)) {
            quality = QualityFoodFluidsApi.getItemOrContainedFluidQuality(item);
        } else {
            return;
        }

        if (quality.level() <= 0) {
            return;
        }

        String subtype = callback.getReturnValue();
        String qualitySubtype = "quality_food_fluids:quality=" + quality.level();

        if (subtype.equals(qualitySubtype) || subtype.startsWith(qualitySubtype + ";")
                || subtype.endsWith(";" + qualitySubtype)) {
            return;
        }

        callback.setReturnValue(subtype.isEmpty() ? qualitySubtype : subtype + ";" + qualitySubtype);
    }
}
