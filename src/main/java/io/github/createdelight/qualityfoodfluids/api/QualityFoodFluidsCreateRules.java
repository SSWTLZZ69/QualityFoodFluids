package io.github.createdelight.qualityfoodfluids.api;

import de.cadentem.quality_food.core.Quality;
import de.cadentem.quality_food.config.QualityConfig;
import de.cadentem.quality_food.util.QualityUtils;
import de.cadentem.quality_food.util.Utils;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public final class QualityFoodFluidsCreateRules {
    private static final RandomSource RANDOM = RandomSource.create();

    private QualityFoodFluidsCreateRules() {
    }

    public static FluidStack applyEmptyingQuality(ItemStack source, FluidStack result) {
        if (result.isEmpty() || !QualityFoodFluidsApi.canCarryQuality(result)) {
            return QualityFoodFluidsApi.clearQuality(result);
        }

        return QualityFoodFluidsApi.copyContainerQualityToFluid(source, result);
    }

    public static ItemStack applyFillingQuality(FluidStack source, ItemStack result) {
        return applyFillingQuality(QualityFoodFluidsApi.getQuality(source), result);
    }

    public static ItemStack applyFillingQuality(Quality quality, ItemStack result) {
        if (result.isEmpty() || !Utils.isValidItem(result)) {
            return result;
        }

        return quality.level() > 0 ? QualityFoodFluidsApi.applyItemQuality(result, quality) : result;
    }

    public static List<FluidStack> applyBasinOutputQuality(List<ItemStack> consumedItems, List<FluidStack> consumedFluids, List<FluidStack> outputs) {
        Quality quality = rollBasinQuality(consumedItems, consumedFluids);
        return applyBasinFluidOutputQuality(quality, outputs);
    }

    public static List<ItemStack> applyBasinItemOutputQuality(Quality quality, List<ItemStack> outputs) {
        if (quality == null) {
            quality = Quality.NONE;
        }

        for (int i = 0; i < outputs.size(); i++) {
            outputs.set(i, applyItemOutputQuality(quality, outputs.get(i)));
        }

        return outputs;
    }

    public static List<FluidStack> applyBasinFluidOutputQuality(Quality quality, List<FluidStack> outputs) {
        if (quality == null) {
            quality = Quality.NONE;
        }

        if (quality.level() <= 0) {
            for (int i = 0; i < outputs.size(); i++) {
                outputs.set(i, applyFluidOutputQuality(quality, outputs.get(i)));
            }
            return outputs;
        }

        for (int i = 0; i < outputs.size(); i++) {
            outputs.set(i, applyFluidOutputQuality(quality, outputs.get(i)));
        }

        return outputs;
    }

    public static ItemStack applyItemOutputQuality(Quality quality, ItemStack output) {
        if (quality == null) {
            quality = Quality.NONE;
        }

        if (output.isEmpty() || !Utils.isValidItem(output)) {
            return output;
        }

        return quality.level() > 0
                ? QualityFoodFluidsApi.applyItemQuality(output, quality)
                : QualityFoodFluidsApi.clearItemQuality(output);
    }

    public static FluidStack applyFluidOutputQuality(Quality quality, FluidStack output) {
        if (quality == null) {
            quality = Quality.NONE;
        }

        if (output.isEmpty()) {
            return output;
        }

        return quality.level() > 0
                ? QualityFoodFluidsApi.withQuality(output, quality)
                : QualityFoodFluidsApi.clearQuality(output);
    }

    public static Quality rollBasinQuality(List<ItemStack> consumedItems, List<FluidStack> consumedFluids) {
        double totalWeight = 0;
        int sourceCount = 0;
        boolean hasQualitySource = false;

        for (ItemStack stack : consumedItems) {
            if (Utils.isValidItem(stack)) {
                Quality quality = QualityFoodFluidsApi.getItemQuality(stack);
                totalWeight += QualityConfig.getWeight(quality);
                hasQualitySource |= quality.level() > 0;
                sourceCount++;
            }
        }

        Quality selected = Quality.NONE;

        for (FluidStack stack : consumedFluids) {
            if (QualityFoodFluidsApi.canCarryQuality(stack)) {
                Quality quality = QualityFoodFluidsApi.getQuality(stack);
                totalWeight += QualityConfig.getWeight(quality);
                hasQualitySource |= quality.level() > 0;
                sourceCount++;
            }
        }

        if (!hasQualitySource) {
            return Quality.NONE;
        }

        return rollQuality(totalWeight, sourceCount, hasQualitySource);
    }

    public static Quality rollQuality(double totalWeight, int sourceCount, boolean hasQualitySource) {
        if (!hasQualitySource) {
            return Quality.NONE;
        }

        double averageWeight = sourceCount == 0 ? 0 : totalWeight / sourceCount;
        Quality selected = Quality.NONE;

        for (Quality quality : Quality.values()) {
            if (!QualityUtils.isValidQuality(quality) || quality.level() <= 0) {
                continue;
            }

            double chance = sourceCount == 0
                    ? QualityConfig.getChance(quality)
                    : QualityConfig.calculateChance(quality, averageWeight);

            if (chance > 0 && chance >= RANDOM.nextDouble()) {
                selected = quality;
            }
        }

        return selected;
    }
}
