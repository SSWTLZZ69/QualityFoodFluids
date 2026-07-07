package io.github.createdelight.qualityfoodfluids.internal;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import de.cadentem.quality_food.core.Quality;
import de.cadentem.quality_food.util.Utils;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsCreateRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public final class BasinRecipeQualityContext {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private BasinRecipeQualityContext() {
    }

    public static void begin(BasinBlockEntity basin, Recipe<?> recipe, boolean test) {
        List<ItemStack> consumedItems = consumedItems(recipe, itemHandler(basin));
        List<FluidStack> consumedFluids = consumedFluids(recipe, fluidHandler(basin));
        CURRENT.set(new Context(
                basin,
                recipe,
                test,
                sourceSignature(consumedItems, consumedFluids),
                copyItems(consumedItems),
                copyFluids(consumedFluids)
        ));
    }

    public static void end() {
        CURRENT.remove();
    }

    public static boolean applyToOutputs(BasinBlockEntity basin, List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate) {
        Context context = CURRENT.get();

        if (context == null || context.basin != basin || !hasQualityCapableOutput(outputItems, outputFluids)) {
            return true;
        }

        BasinProcessingTicket ticket = getOrCreateTicket(basin, context, outputFluids, simulate);

        if (ticket == null || !ticket.matches(context.recipe.getId(), context.sourceSignature)) {
            clearTicket(basin);
            return false;
        }

        QualityFoodFluidsCreateRules.applyBasinItemOutputQuality(ticket.quality(), outputItems);

        if (!outputFluids.isEmpty()) {
            replaceOutputs(outputFluids, ticket.copyOutputs());
        }

        return true;
    }

    public static void finishOutputs(BasinBlockEntity basin, List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate, boolean success) {
        Context context = CURRENT.get();

        if (context == null || context.basin != basin || simulate || !success) {
            return;
        }

        if (hasQualityCapableOutput(outputItems, outputFluids)) {
            clearTicket(basin);
        }
    }

    public static void ensureTicket(BasinBlockEntity basin, Recipe<?> recipe) {
        if (basin == null || recipe == null) {
            return;
        }

        List<ItemStack> itemOutputs = baseItemOutputs(recipe);
        List<FluidStack> fluidOutputs = baseFluidOutputs(recipe);

        if (!hasQualityCapableOutput(itemOutputs, fluidOutputs)) {
            clearTicket(basin);
            return;
        }

        List<String> sourceSignature = sourceSignature(recipe, basin);
        BasinProcessingTicket existing = currentTicket(basin);

        if (existing != null && existing.matches(recipe.getId(), sourceSignature)) {
            return;
        }

        setTicket(basin, createTicket(recipe.getId(), sourceSignature, recipe, basin, fluidOutputs));
    }

    private static BasinProcessingTicket getOrCreateTicket(BasinBlockEntity basin, Context context, List<FluidStack> outputFluids, boolean simulate) {
        BasinProcessingTicket ticket = currentTicket(basin);

        if (ticket != null && ticket.matches(context.recipe.getId(), context.sourceSignature)) {
            return ticket;
        }

        if (ticket != null && !simulate && !context.test) {
            return ticket;
        }

        ticket = createTicket(context.recipe.getId(), context.sourceSignature, context.consumedItems, context.consumedFluids, copyFluids(outputFluids));
        setTicket(basin, ticket);
        return ticket;
    }

    private static BasinProcessingTicket createTicket(ResourceLocation recipeId, List<String> sourceSignature, Recipe<?> recipe, BasinBlockEntity basin, List<FluidStack> outputFluids) {
        List<ItemStack> consumedItems = consumedItems(recipe, itemHandler(basin));
        List<FluidStack> consumedFluids = consumedFluids(recipe, fluidHandler(basin));
        return createTicket(recipeId, sourceSignature, consumedItems, consumedFluids, outputFluids);
    }

    private static BasinProcessingTicket createTicket(ResourceLocation recipeId, List<String> sourceSignature, List<ItemStack> consumedItems, List<FluidStack> consumedFluids, List<FluidStack> outputFluids) {
        Quality quality = QualityFoodFluidsCreateRules.rollBasinQuality(consumedItems, consumedFluids);
        return new BasinProcessingTicket(recipeId, sourceSignature, quality,
                QualityFoodFluidsCreateRules.applyBasinFluidOutputQuality(quality, outputFluids));
    }

    private static BasinProcessingTicket currentTicket(BasinBlockEntity basin) {
        return basin instanceof BasinQualityTicketHolder holder ? holder.qualityFoodFluids$getTicket() : null;
    }

    private static void setTicket(BasinBlockEntity basin, BasinProcessingTicket ticket) {
        if (basin instanceof BasinQualityTicketHolder holder) {
            holder.qualityFoodFluids$setTicket(ticket);
        }
    }

    private static void clearTicket(BasinBlockEntity basin) {
        if (basin instanceof BasinQualityTicketHolder holder) {
            holder.qualityFoodFluids$clearTicket();
        }
    }

    private static IItemHandler itemHandler(BasinBlockEntity basin) {
        return basin.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
    }

    private static IFluidHandler fluidHandler(BasinBlockEntity basin) {
        return basin.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
    }

    private static List<String> sourceSignature(Recipe<?> recipe, BasinBlockEntity basin) {
        return sourceSignature(consumedItems(recipe, itemHandler(basin)), consumedFluids(recipe, fluidHandler(basin)));
    }

    private static List<String> sourceSignature(List<ItemStack> consumedItems, List<FluidStack> consumedFluids) {
        List<String> signature = new ArrayList<>();

        for (ItemStack stack : consumedItems) {
            signature.add("item:" + ForgeRegistries.ITEMS.getKey(stack.getItem()) + ":" + stack.getCount() + ":" + tagString(stack));
        }

        for (FluidStack stack : consumedFluids) {
            signature.add("fluid:" + ForgeRegistries.FLUIDS.getKey(stack.getFluid()) + ":" + stack.getAmount() + ":" + tagString(stack));
        }

        return signature;
    }

    private static String tagString(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().toString() : "";
    }

    private static String tagString(FluidStack stack) {
        return stack.hasTag() ? stack.getTag().toString() : "";
    }

    private static List<FluidStack> baseFluidOutputs(Recipe<?> recipe) {
        List<FluidStack> outputs = new ArrayList<>();

        if (recipe instanceof BasinRecipe basinRecipe) {
            for (FluidStack stack : basinRecipe.getFluidResults()) {
                if (!stack.isEmpty()) {
                    outputs.add(stack.copy());
                }
            }
        }

        return outputs;
    }

    private static List<ItemStack> baseItemOutputs(Recipe<?> recipe) {
        List<ItemStack> outputs = new ArrayList<>();

        if (recipe instanceof BasinRecipe basinRecipe) {
            for (ItemStack stack : basinRecipe.getRollableResultsAsItemStacks()) {
                if (!stack.isEmpty()) {
                    outputs.add(stack.copy());
                }
            }
        }

        return outputs;
    }

    private static boolean hasQualityCapableOutput(List<ItemStack> outputItems, List<FluidStack> outputFluids) {
        return hasQualityCapableItemOutput(outputItems) || hasQualityCapableOutput(outputFluids);
    }

    private static boolean hasQualityCapableItemOutput(List<ItemStack> outputItems) {
        for (ItemStack stack : outputItems) {
            if (!stack.isEmpty() && Utils.isValidItem(stack)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasQualityCapableOutput(List<FluidStack> outputFluids) {
        for (FluidStack stack : outputFluids) {
            if (QualityFoodFluidsApi.canCarryQuality(stack)) {
                return true;
            }
        }

        return false;
    }

    private static List<FluidStack> copyFluids(List<FluidStack> fluids) {
        List<FluidStack> copies = new ArrayList<>();

        for (FluidStack stack : fluids) {
            copies.add(stack.copy());
        }

        return copies;
    }

    private static List<ItemStack> copyItems(List<ItemStack> items) {
        List<ItemStack> copies = new ArrayList<>();

        for (ItemStack stack : items) {
            copies.add(stack.copy());
        }

        return copies;
    }

    private static void replaceOutputs(List<FluidStack> outputFluids, List<FluidStack> replacement) {
        outputFluids.clear();

        for (FluidStack stack : replacement) {
            outputFluids.add(stack.copy());
        }
    }

    private static List<ItemStack> consumedItems(Recipe<?> recipe, IItemHandler items) {
        List<ItemStack> stacks = new ArrayList<>();

        if (items == null) {
            return stacks;
        }

        int[] usedFromSlot = new int[items.getSlots()];

        Ingredients:
        for (Ingredient ingredient : recipe.getIngredients()) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                ItemStack stack = items.getStackInSlot(slot);

                if (stack.isEmpty() || stack.getCount() <= usedFromSlot[slot]) {
                    continue;
                }

                ItemStack single = stack.copy();
                single.setCount(1);

                if (!ingredient.test(single)) {
                    continue;
                }

                usedFromSlot[slot]++;
                stacks.add(single);
                continue Ingredients;
            }
        }

        return stacks;
    }

    private static List<FluidStack> consumedFluids(Recipe<?> recipe, IFluidHandler fluids) {
        List<FluidStack> stacks = new ArrayList<>();

        if (fluids == null || !(recipe instanceof BasinRecipe basinRecipe)) {
            return stacks;
        }

        int[] usedFromTank = new int[fluids.getTanks()];

        FluidIngredients:
        for (FluidIngredient ingredient : basinRecipe.getFluidIngredients()) {
            int amountRequired = ingredient.getRequiredAmount();

            for (int tank = 0; tank < fluids.getTanks(); tank++) {
                FluidStack stack = fluids.getFluidInTank(tank);

                if (stack.isEmpty() || stack.getAmount() <= usedFromTank[tank] || !ingredient.test(stack)) {
                    continue;
                }

                int amount = Math.min(amountRequired, stack.getAmount() - usedFromTank[tank]);
                FluidStack consumed = stack.copy();
                consumed.setAmount(amount);
                stacks.add(consumed);
                usedFromTank[tank] += amount;
                amountRequired -= amount;

                if (amountRequired <= 0) {
                    continue FluidIngredients;
                }
            }
        }

        return stacks;
    }

    private record Context(BasinBlockEntity basin, Recipe<?> recipe, boolean test, List<String> sourceSignature, List<ItemStack> consumedItems, List<FluidStack> consumedFluids) {
    }
}
