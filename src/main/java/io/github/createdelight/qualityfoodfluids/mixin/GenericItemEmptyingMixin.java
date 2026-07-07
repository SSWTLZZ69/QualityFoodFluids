package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsCreateRules;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GenericItemEmptying.class, remap = false)
public abstract class GenericItemEmptyingMixin {
    @Inject(method = "emptyItem", at = @At("RETURN"), cancellable = true)
    private static void qualityFoodFluids$applyEmptyingQuality(Level world, ItemStack stack, boolean simulate, CallbackInfoReturnable<Pair<FluidStack, ItemStack>> callback) {
        Pair<FluidStack, ItemStack> result = callback.getReturnValue();
        FluidStack fluid = QualityFoodFluidsCreateRules.applyEmptyingQuality(stack, result.getFirst());
        callback.setReturnValue(Pair.of(fluid, result.getSecond()));
    }
}
