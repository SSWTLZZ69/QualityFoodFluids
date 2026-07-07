package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.foundation.recipe.RecipeApplier;
import io.github.createdelight.qualityfoodfluids.internal.SequencedAssemblyQualityContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = RecipeApplier.class, priority = 900, remap = false)
public abstract class RecipeApplierMixin {
    @Inject(method = "applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;", at = @At("HEAD"))
    private static void qualityFoodFluids$enterRecipeApplier(
            Level level,
            ItemStack stack,
            Recipe<?> recipe,
            boolean award,
            CallbackInfoReturnable<List<ItemStack>> callback
    ) {
        SequencedAssemblyQualityContext.enterRecipeApplier();
    }

    @Inject(method = "applyRecipeOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/crafting/Recipe;Z)Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private static void qualityFoodFluids$applyPendingSequencedAssemblyQuality(
            Level level,
            ItemStack stack,
            Recipe<?> recipe,
            boolean award,
            CallbackInfoReturnable<List<ItemStack>> callback
    ) {
        try {
            List<ItemStack> results = callback.getReturnValue();
            if (results == null || results.isEmpty()) {
                return;
            }

            List<ItemStack> copies = new ArrayList<>(results.size());
            boolean changed = false;

            for (ItemStack result : results) {
                ItemStack applied = SequencedAssemblyQualityContext.applyPendingFinalQuality(result);
                copies.add(applied);
                changed |= applied != result;
            }

            if (changed) {
                callback.setReturnValue(copies);
            }
        } finally {
            SequencedAssemblyQualityContext.exitRecipeApplier();
        }
    }
}
