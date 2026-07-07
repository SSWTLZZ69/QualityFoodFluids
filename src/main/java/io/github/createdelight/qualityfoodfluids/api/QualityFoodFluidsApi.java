package io.github.createdelight.qualityfoodfluids.api;

import de.cadentem.quality_food.core.Quality;
import de.cadentem.quality_food.util.QualityUtils;
import io.github.createdelight.qualityfoodfluids.registry.QualityFoodFluidTags;
import io.github.createdelight.qualityfoodfluids.world.WorldFluidQualityData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public final class QualityFoodFluidsApi {
    public static final String FLUID_QUALITY_TAG = "quality_food_fluids";
    public static final String QUALITY_KEY = "quality";
    public static final String VERSION_KEY = "version";
    public static final int CURRENT_VERSION = 1;

    private QualityFoodFluidsApi() {
    }

    public static Quality getQuality(FluidStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) {
            return Quality.NONE;
        }

        CompoundTag qualityTag = stack.getTag().getCompound(FLUID_QUALITY_TAG);
        return Quality.get(qualityTag.getInt(QUALITY_KEY));
    }

    public static boolean hasQuality(FluidStack stack) {
        return getQuality(stack).level() > 0;
    }

    public static FluidStack stripQualityForComparison(FluidStack stack) {
        return clearQuality(stack);
    }

    public static boolean isFluidEqualIgnoringQuality(FluidStack left, FluidStack right) {
        if (left == null || right == null) {
            return false;
        }

        return stripQualityForComparison(left).isFluidEqual(stripQualityForComparison(right));
    }

    public static FluidStack withQuality(FluidStack stack, Quality quality) {
        FluidStack copy = stack.copy();

        if (copy.isEmpty() || !canCarryQuality(copy) || quality == null || quality.level() <= 0) {
            return clearQuality(copy);
        }

        CompoundTag qualityTag = new CompoundTag();
        qualityTag.putInt(QUALITY_KEY, quality.level());
        qualityTag.putInt(VERSION_KEY, CURRENT_VERSION);
        copy.getOrCreateTag().put(FLUID_QUALITY_TAG, qualityTag);
        return copy;
    }

    public static FluidStack clearQuality(FluidStack stack) {
        FluidStack copy = stack.copy();

        if (copy.hasTag()) {
            copy.getTag().remove(FLUID_QUALITY_TAG);

            if (copy.getTag().isEmpty()) {
                copy.setTag(null);
            }
        }

        return copy;
    }

    public static boolean canCarryQuality(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        Fluid fluid = stack.getFluid();
        return fluid.defaultFluidState().is(QualityFoodFluidTags.QUALITY_FLUIDS)
                && !fluid.defaultFluidState().is(QualityFoodFluidTags.CLEAR_QUALITY_FLUIDS);
    }

    public static boolean canHaveWorldQuality(FluidStack stack) {
        if (!canCarryQuality(stack)) {
            return false;
        }

        return stack.getFluid().defaultFluidState().is(QualityFoodFluidTags.WORLD_QUALITY_FLUIDS);
    }

    public static Quality getItemQuality(ItemStack stack) {
        return QualityUtils.getQuality(stack);
    }

    public static Quality getContainedFluidQuality(ItemStack stack) {
        if (stack.isEmpty()) {
            return Quality.NONE;
        }

        IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
        return getContainedFluidQuality(handler);
    }

    public static Quality getContainedFluidQuality(IFluidHandlerItem handler) {
        if (handler == null) {
            return Quality.NONE;
        }

        for (int tank = 0; tank < handler.getTanks(); tank++) {
            Quality quality = getQuality(handler.getFluidInTank(tank));

            if (quality.level() > 0) {
                return quality;
            }
        }

        return getItemQuality(handler.getContainer());
    }

    public static Quality getItemOrContainedFluidQuality(ItemStack stack) {
        Quality quality = getContainedFluidQuality(stack);

        if (quality.level() > 0) {
            return quality;
        }

        return getItemQuality(stack);
    }

    public static ItemStack applyItemQuality(ItemStack stack, Quality quality) {
        ItemStack copy = stack.copy();

        if (copy.isEmpty() || quality == null || quality.level() <= 0) {
            return clearItemQuality(copy);
        }

        CompoundTag qualityTag = new CompoundTag();
        qualityTag.putInt(QualityUtils.QUALITY_KEY, quality.level());
        copy.getOrCreateTag().put(QualityUtils.QUALITY_TAG, qualityTag);
        return copy;
    }

    public static ItemStack clearItemQuality(ItemStack stack) {
        ItemStack copy = stack.copy();

        if (copy.hasTag()) {
            copy.getTag().remove(QualityUtils.QUALITY_TAG);

            if (copy.getTag().isEmpty()) {
                copy.setTag(null);
            }
        }

        return copy;
    }

    public static FluidStack copyItemQualityToFluid(ItemStack source, FluidStack fluid) {
        return withQuality(fluid, getItemQuality(source));
    }

    public static FluidStack copyContainerQualityToFluid(ItemStack source, FluidStack fluid) {
        Quality quality = getQuality(fluid);

        if (quality.level() > 0) {
            return withQuality(fluid, quality);
        }

        quality = getContainedFluidQuality(source);

        if (quality.level() > 0) {
            return withQuality(fluid, quality);
        }

        return copyItemQualityToFluid(source, fluid);
    }

    public static FluidStack copyContainerQualityToFluid(IFluidHandlerItem source, FluidStack fluid) {
        Quality quality = getQuality(fluid);

        if (quality.level() > 0) {
            return withQuality(fluid, quality);
        }

        return withQuality(fluid, getContainedFluidQuality(source));
    }

    public static ItemStack copyFluidQualityToItem(FluidStack source, ItemStack item) {
        return applyItemQuality(item, getQuality(source));
    }

    public static Quality getWorldQuality(LevelAccessor level, BlockPos pos) {
        return WorldFluidQualityData.getQuality(level, pos);
    }

    public static void setWorldQuality(LevelAccessor level, BlockPos pos, Quality quality) {
        WorldFluidQualityData.setQuality(level, pos, quality);
    }

    public static void clearWorldQuality(LevelAccessor level, BlockPos pos) {
        WorldFluidQualityData.clear(level, pos);
    }

    public static FluidStack copyWorldQualityToFluid(LevelAccessor level, BlockPos pos, FluidStack fluid) {
        return withQuality(fluid, getWorldQuality(level, pos));
    }

    public static void copyFluidQualityToWorld(FluidStack source, LevelAccessor level, BlockPos pos) {
        if (canHaveWorldQuality(source)) {
            WorldFluidQualityData.setQuality(level, pos, source, getQuality(source), "fluid_stack");
        }
    }
}
