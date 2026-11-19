package io.github.createdelight.qualityfoodfluids.util;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;

/**
 * Quality/name translation composition adapted from Create Gears and Tavern
 * (Copyright (c) 2025, Yison), licensed under BSD-3-Clause.
 * Modified for QFF's quality tiers and prefix colors. See
 * META-INF/licenses/GearsAndTavern-BSD-3-Clause.txt for the full notice.
 * Upstream: https://github.com/yision1/GearsandTavern/tree/78b948173b07f8bee25e22668892a229cc57d2e1
 */
public final class FluidQualityNames {
    public static final String WITH_QUALITY = "fluid.quality_food_fluids.with_quality";

    private FluidQualityNames() {
    }

    public static boolean shouldPrefixItemName(ItemStack stack) {
        return QualityFoodFluidsApi.isAutomaticallyQualityCapableItem(stack);
    }

    public static MutableComponent withQuality(Component originalName, Quality quality) {
        MutableComponent prefix = switch (quality) {
            case IRON -> Component.translatable("quality_type.quality_food_fluids.iron")
                    .withStyle(ChatFormatting.WHITE);
            case GOLD -> Component.translatable("quality_type.quality_food_fluids.gold")
                    .withStyle(ChatFormatting.GOLD);
            case DIAMOND -> Component.translatable("quality_type.quality_food_fluids.diamond")
                    .withStyle(ChatFormatting.AQUA);
            default -> Component.translatable("quality_type.quality_food_fluids.none")
                    .withStyle(ChatFormatting.GRAY);
        };

        return Component.translatable(WITH_QUALITY, prefix, withoutQuality(originalName));
    }

    private static MutableComponent withoutQuality(Component name) {
        MutableComponent result;

        if (name.getContents() instanceof TranslatableContents contents) {
            Object[] arguments = contents.getArgs();

            if (WITH_QUALITY.equals(contents.getKey())
                    && arguments.length == 2
                    && arguments[1] instanceof Component originalName) {
                // Container names can already contain a fluid's normal prefix.
                // Replace it with the container's actual quality, rather than
                // displaying two prefixes or retaining the inner normal grade.
                result = withoutQuality(originalName);
                result.withStyle(style -> style.applyTo(name.getStyle()));
            } else {
                Object[] cleanArguments = arguments.clone();

                for (int index = 0; index < cleanArguments.length; index++) {
                    if (cleanArguments[index] instanceof Component argument) {
                        cleanArguments[index] = withoutQuality(argument);
                    }
                }

                result = Component.translatableWithFallback(contents.getKey(), contents.getFallback(), cleanArguments)
                        .setStyle(name.getStyle());
            }
        } else {
            result = name.plainCopy().setStyle(name.getStyle());
        }

        for (Component sibling : name.getSiblings()) {
            result.append(withoutQuality(sibling));
        }

        return result;
    }
}
