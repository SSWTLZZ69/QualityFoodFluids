package io.github.createdelight.qualityfoodfluids.world;

import de.cadentem.quality_food.core.Quality;
import net.minecraft.resources.ResourceLocation;

public record WorldFluidQuality(ResourceLocation fluidId, Quality quality, String source, long placedGameTime, int volume) {
}
