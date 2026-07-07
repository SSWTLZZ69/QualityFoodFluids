package io.github.createdelight.qualityfoodfluids.registry;

import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class QualityFoodItemTags {
    public static final TagKey<Item> QUALITY_CONTAINERS = create("quality_containers");
    public static final TagKey<Item> QUALITY_SOURCES = create("quality_sources");

    private QualityFoodItemTags() {
    }

    private static TagKey<Item> create(String name) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(QualityFoodFluids.MODID, name));
    }
}
