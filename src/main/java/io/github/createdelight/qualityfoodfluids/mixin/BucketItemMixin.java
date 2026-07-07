package io.github.createdelight.qualityfoodfluids.mixin;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.world.WorldFluidQualityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BucketItem.class)
public abstract class BucketItemMixin {
    @Inject(method = "emptyContents(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/item/ItemStack;)Z", at = @At("RETURN"), remap = false)
    private void qualityFoodFluids$storePlacedQuality(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult hitResult, ItemStack container, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue()) {
            return;
        }

        BucketItem bucket = (BucketItem) (Object) this;
        FluidStack fluid = new FluidStack(bucket.getFluid(), 1000);
        Quality quality = QualityFoodFluidsApi.getItemQuality(container);

        if (quality.level() <= 0 || !QualityFoodFluidsApi.canHaveWorldQuality(fluid)) {
            WorldFluidQualityData.clear(level, pos);
            return;
        }

        WorldFluidQualityData.setQuality(level, pos, fluid, quality, "bucket");
    }
}
