package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinOperatingBlockEntity;
import io.github.createdelight.qualityfoodfluids.internal.BasinRecipeQualityContext;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = BasinOperatingBlockEntity.class, remap = false)
public abstract class BasinOperatingBlockEntityMixin {
    @Shadow
    protected Recipe<?> currentRecipe;

    @Shadow
    protected abstract Optional<BasinBlockEntity> getBasin();

    @Inject(method = "startProcessingBasin", at = @At("HEAD"))
    private void qualityFoodFluids$ensureBasinTicket(CallbackInfo callback) {
        getBasin().ifPresent(basin -> BasinRecipeQualityContext.ensureTicket(basin, currentRecipe));
    }
}
