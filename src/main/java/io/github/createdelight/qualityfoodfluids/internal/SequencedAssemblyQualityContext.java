package io.github.createdelight.qualityfoodfluids.internal;

import de.cadentem.quality_food.config.QualityConfig;
import de.cadentem.quality_food.core.Quality;
import de.cadentem.quality_food.util.Utils;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsCreateRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayDeque;
import java.util.Deque;

public final class SequencedAssemblyQualityContext {
    private static final String CREATE_ASSEMBLY_TAG = "SequencedAssembly";
    private static final String QUALITY_TAG = "QualityFoodFluidsSequencedAssembly";
    private static final String TOTAL_WEIGHT_KEY = "TotalWeight";
    private static final String SOURCE_COUNT_KEY = "SourceCount";
    private static final String HAS_QUALITY_SOURCE_KEY = "HasQualitySource";
    private static final String FINAL_PENDING_KEY = "FinalPending";

    private static final ThreadLocal<FluidStack> FILLING_SOURCE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> FILLING_USED_BY_SEQUENCE = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Deque<ItemStack>> DEPLOYING_SOURCES = ThreadLocal.withInitial(ArrayDeque::new);
    private static final ThreadLocal<Boolean> RECIPE_APPLIER_ACTIVE = ThreadLocal.withInitial(() -> false);

    private SequencedAssemblyQualityContext() {
    }

    public static void captureFillingSource(FluidStack source) {
        FILLING_SOURCE.set(source == null ? FluidStack.EMPTY : source.copy());
        FILLING_USED_BY_SEQUENCE.set(false);
    }

    public static boolean consumeFillingUsedBySequence() {
        boolean used = FILLING_USED_BY_SEQUENCE.get();
        FILLING_USED_BY_SEQUENCE.remove();
        FILLING_SOURCE.remove();
        return used;
    }

    public static void captureDeployingSource(ItemStack source) {
        DEPLOYING_SOURCES.get().push(source == null ? ItemStack.EMPTY : source.copy());
    }

    public static void clearDeployingSource() {
        Deque<ItemStack> sources = DEPLOYING_SOURCES.get();
        if (!sources.isEmpty()) {
            sources.pop();
        }

        if (sources.isEmpty()) {
            DEPLOYING_SOURCES.remove();
        }
    }

    public static void enterRecipeApplier() {
        RECIPE_APPLIER_ACTIVE.set(true);
    }

    public static void exitRecipeApplier() {
        RECIPE_APPLIER_ACTIVE.remove();
    }

    public static ItemStack applyAdvanceQuality(ItemStack input, ItemStack result) {
        if (result.isEmpty()) {
            return result;
        }

        State state = State.read(input);

        if (!state.hasStoredData) {
            state.addItemSource(input);
        }

        state.addFluidSource(FILLING_SOURCE.get());
        state.addItemSource(currentDeployingSource());

        if (FILLING_SOURCE.get() != null) {
            FILLING_USED_BY_SEQUENCE.set(true);
        }

        if (isSequencedAssembly(result)) {
            ItemStack copy = result.copy();
            state.write(copy);
            return copy;
        }

        ItemStack copy = result.copy();
        if (RECIPE_APPLIER_ACTIVE.get()) {
            state.write(copy, true);
            return applyFinalQuality(copy, state, false);
        }

        return applyFinalQuality(copy, state, true);
    }

    public static ItemStack applyPendingFinalQuality(ItemStack stack) {
        State state = State.read(stack);
        if (!state.finalPending) {
            return stack;
        }

        return applyFinalQuality(stack.copy(), state, true);
    }

    private static ItemStack applyFinalQuality(ItemStack stack, State state, boolean clearInternal) {
        Quality quality = QualityFoodFluidsCreateRules.rollQuality(
                state.totalWeight,
                state.sourceCount,
                state.hasQualitySource
        );

        ItemStack result = stack;
        if (Utils.isValidItem(result)) {
            result = quality.level() > 0
                    ? QualityFoodFluidsApi.applyItemQuality(result, quality)
                    : QualityFoodFluidsApi.clearItemQuality(result);
        }

        if (clearInternal) {
            clearInternalTag(result);
        }

        return result;
    }

    private static boolean isSequencedAssembly(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(CREATE_ASSEMBLY_TAG);
    }

    private static ItemStack currentDeployingSource() {
        Deque<ItemStack> sources = DEPLOYING_SOURCES.get();
        return sources.isEmpty() ? ItemStack.EMPTY : sources.peek();
    }

    private static void clearInternalTag(ItemStack stack) {
        if (!stack.hasTag()) {
            return;
        }

        stack.getTag().remove(QUALITY_TAG);

        if (stack.getTag().isEmpty()) {
            stack.setTag(null);
        }
    }

    private static final class State {
        private double totalWeight;
        private int sourceCount;
        private boolean hasQualitySource;
        private boolean finalPending;
        private boolean hasStoredData;

        private static State read(ItemStack stack) {
            State state = new State();

            if (!stack.hasTag() || !stack.getTag().contains(QUALITY_TAG)) {
                return state;
            }

            CompoundTag tag = stack.getTag().getCompound(QUALITY_TAG);
            state.totalWeight = tag.getDouble(TOTAL_WEIGHT_KEY);
            state.sourceCount = tag.getInt(SOURCE_COUNT_KEY);
            state.hasQualitySource = tag.getBoolean(HAS_QUALITY_SOURCE_KEY);
            state.finalPending = tag.getBoolean(FINAL_PENDING_KEY);
            state.hasStoredData = true;
            return state;
        }

        private void write(ItemStack stack) {
            write(stack, false);
        }

        private void write(ItemStack stack, boolean finalPending) {
            CompoundTag tag = new CompoundTag();
            tag.putDouble(TOTAL_WEIGHT_KEY, totalWeight);
            tag.putInt(SOURCE_COUNT_KEY, sourceCount);
            tag.putBoolean(HAS_QUALITY_SOURCE_KEY, hasQualitySource);
            tag.putBoolean(FINAL_PENDING_KEY, finalPending);
            stack.getOrCreateTag().put(QUALITY_TAG, tag);
        }

        private void addItemSource(ItemStack stack) {
            if (stack == null || stack.isEmpty() || !Utils.isValidItem(stack)) {
                return;
            }

            addQuality(QualityFoodFluidsApi.getItemOrContainedFluidQuality(stack));
        }

        private void addFluidSource(FluidStack stack) {
            if (stack == null || stack.isEmpty() || !QualityFoodFluidsApi.canCarryQuality(stack)) {
                return;
            }

            addQuality(QualityFoodFluidsApi.getQuality(stack));
        }

        private void addQuality(Quality quality) {
            if (quality == null) {
                quality = Quality.NONE;
            }

            totalWeight += QualityConfig.getWeight(quality);
            sourceCount++;
            hasQualitySource |= quality.level() > 0;
        }
    }
}
