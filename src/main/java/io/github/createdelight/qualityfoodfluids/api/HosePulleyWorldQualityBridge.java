package io.github.createdelight.qualityfoodfluids.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.fluids.FluidStack;

public final class HosePulleyWorldQualityBridge {
    private static final ThreadLocal<FluidStack> DEPOSITING = new ThreadLocal<>();

    private HosePulleyWorldQualityBridge() {
    }

    public static void beginDeposit(FluidStack stack) {
        DEPOSITING.set(stack.copy());
    }

    public static void endDeposit() {
        DEPOSITING.remove();
    }

    public static void handlePlacedSource(LevelAccessor level, BlockPos pos) {
        FluidStack stack = DEPOSITING.get();

        if (stack == null || stack.isEmpty()) {
            return;
        }

        if (QualityFoodFluidsApi.canHaveWorldQuality(stack) && QualityFoodFluidsApi.hasQuality(stack)) {
            QualityFoodFluidsApi.copyFluidQualityToWorld(stack, level, pos);
            return;
        }

        QualityFoodFluidsApi.clearWorldQuality(level, pos);
    }
}
