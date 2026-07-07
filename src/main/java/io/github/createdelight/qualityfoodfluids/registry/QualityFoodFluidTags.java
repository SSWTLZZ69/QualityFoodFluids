package io.github.createdelight.qualityfoodfluids.registry;

import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public final class QualityFoodFluidTags {
    public static final TagKey<Fluid> QUALITY_FLUIDS = create("quality_fluids");
    public static final TagKey<Fluid> WORLD_QUALITY_FLUIDS = create("world_quality_fluids");
    public static final TagKey<Fluid> SOURCE_ONLY_WORLD_QUALITY_FLUIDS = create("source_only_world_quality_fluids");
    public static final TagKey<Fluid> CLEAR_QUALITY_FLUIDS = create("clear_quality_fluids");
    public static final TagKey<Fluid> LEVEL_DATA_BACKED_WORLD_FLUIDS = create("level_data_backed_world_fluids");

    private QualityFoodFluidTags() {
    }

    private static TagKey<Fluid> create(String name) {
        return TagKey.create(Registries.FLUID, new ResourceLocation(QualityFoodFluids.MODID, name));
    }
}
