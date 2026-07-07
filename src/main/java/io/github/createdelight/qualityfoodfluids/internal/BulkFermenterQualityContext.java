package io.github.createdelight.qualityfoodfluids.internal;

import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermenterBlockEntity;
import com.jesz.createdieselgenerators.content.bulk_fermenter.BulkFermentingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import de.cadentem.quality_food.core.Quality;
import de.cadentem.quality_food.util.Utils;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsCreateRules;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class BulkFermenterQualityContext {
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private BulkFermenterQualityContext() {
    }

    public static void ensureTicket(BulkFermenterBlockEntity fermenter, BulkFermentingRecipe recipe) {
        if (fermenter == null || recipe == null) {
            return;
        }

        List<ItemStack> itemOutputs = baseItemOutputs(recipe);
        List<FluidStack> fluidOutputs = baseFluidOutputs(recipe);

        if (!hasQualityCapableOutput(itemOutputs, fluidOutputs)) {
            clearTicket(fermenter);
            return;
        }

        List<ItemStack> consumedItems = consumedItems(recipe, itemHandler(fermenter));
        List<FluidStack> consumedFluids = consumedFluids(recipe, fluidHandler(fermenter));
        List<String> sourceSignature = sourceSignature(consumedItems, consumedFluids);
        BasinProcessingTicket existing = currentTicket(fermenter);

        if (existing != null && existing.matches(recipe.getId(), sourceSignature)) {
            return;
        }

        setTicket(fermenter, createTicket(recipe.getId(), sourceSignature, consumedItems, consumedFluids, fluidOutputs));
    }

    public static void begin(BulkFermenterBlockEntity fermenter, BulkFermentingRecipe recipe, boolean simulate) {
        CURRENT.remove();

        if (simulate || fermenter == null || recipe == null) {
            return;
        }

        List<ItemStack> consumedItems = consumedItems(recipe, itemHandler(fermenter));
        List<FluidStack> consumedFluids = consumedFluids(recipe, fluidHandler(fermenter));
        CURRENT.set(new Context(fermenter, recipe, sourceSignature(consumedItems, consumedFluids), consumedItems, consumedFluids));
    }

    public static void end(BulkFermenterBlockEntity fermenter, boolean simulate, boolean success) {
        try {
            Context context = CURRENT.get();

            if (context != null && context.fermenter == fermenter && !simulate && success) {
                clearTicket(fermenter);
            }
        } finally {
            CURRENT.remove();
        }
    }

    public static Boolean applyToOutputs(BulkFermenterBlockEntity fermenter, List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, boolean simulate) {
        Context context = CURRENT.get();

        if (context == null || context.fermenter != fermenter || !hasQualityCapableOutput(itemOutputs, fluidOutputs)) {
            return null;
        }

        BasinProcessingTicket ticket = getOrCreateTicket(fermenter, context, fluidOutputs);

        if (ticket == null || !ticket.matches(context.recipe.getId(), context.sourceSignature)) {
            clearTicket(fermenter);
            return false;
        }

        QualityFoodFluidsCreateRules.applyBasinItemOutputQuality(ticket.quality(), itemOutputs);

        if (!fluidOutputs.isEmpty()) {
            replaceOutputs(fluidOutputs, ticket.copyOutputs());
        }

        return acceptOutputs(fermenter, itemOutputs, fluidOutputs, simulate);
    }

    private static BasinProcessingTicket getOrCreateTicket(BulkFermenterBlockEntity fermenter, Context context, List<FluidStack> outputFluids) {
        BasinProcessingTicket ticket = currentTicket(fermenter);

        if (ticket != null) {
            return ticket;
        }

        ticket = createTicket(context.recipe.getId(), context.sourceSignature, context.consumedItems, context.consumedFluids, outputFluids);
        setTicket(fermenter, ticket);
        return ticket;
    }

    private static BasinProcessingTicket createTicket(ResourceLocation recipeId, List<String> sourceSignature, List<ItemStack> consumedItems, List<FluidStack> consumedFluids, List<FluidStack> outputFluids) {
        Quality quality = QualityFoodFluidsCreateRules.rollBasinQuality(consumedItems, consumedFluids);
        return new BasinProcessingTicket(recipeId, sourceSignature, quality,
                QualityFoodFluidsCreateRules.applyBasinFluidOutputQuality(quality, copyFluids(outputFluids)));
    }

    private static boolean acceptOutputs(BulkFermenterBlockEntity fermenter, List<ItemStack> itemOutputs, List<FluidStack> fluidOutputs, boolean simulate) {
        IItemHandler items = itemHandler(fermenter);
        IFluidHandler fluids = fluidHandler(fermenter);

        if (items == null || fluids == null) {
            return false;
        }

        List<FluidPlacement> fluidPlacements = planFluidPlacements(fermenter, fluids, fluidOutputs);

        if (!canAcceptItems(items, itemOutputs) || fluidPlacements == null) {
            return false;
        }

        if (simulate) {
            return true;
        }

        for (ItemStack output : itemOutputs) {
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(items, output.copy(), false);

            if (!remaining.isEmpty()) {
                return false;
            }
        }

        boolean fluidsChanged = false;

        for (FluidPlacement placement : fluidPlacements) {
            IFluidTank tank = fermenter.getTank(placement.tank());
            FluidStack stack = placement.stack().copy();

            if (tank.fill(stack, FluidAction.EXECUTE) != stack.getAmount()) {
                return false;
            }

            fluidsChanged = true;
        }

        if (fluidsChanged && fermenter instanceof BulkFermenterQualityHooks hooks) {
            hooks.qualityFoodFluids$onFluidOutputsChanged();
        }

        return true;
    }

    private static boolean canAcceptItems(IItemHandler items, List<ItemStack> outputs) {
        List<ItemStack> simulated = new ArrayList<>();

        for (int slot = 0; slot < items.getSlots(); slot++) {
            simulated.add(items.getStackInSlot(slot).copy());
        }

        for (ItemStack output : outputs) {
            if (!placeItem(items, simulated, output.copy())) {
                return false;
            }
        }

        return true;
    }

    private static boolean placeItem(IItemHandler items, List<ItemStack> simulated, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }

        for (int slot = 0; slot < simulated.size(); slot++) {
            ItemStack current = simulated.get(slot);

            if (current.isEmpty() || !ItemStack.isSameItemSameTags(current, stack)) {
                continue;
            }

            int limit = Math.min(items.getSlotLimit(slot), current.getMaxStackSize());
            int accepted = Math.min(stack.getCount(), limit - current.getCount());

            if (accepted <= 0) {
                continue;
            }

            current.grow(accepted);
            stack.shrink(accepted);

            if (stack.isEmpty()) {
                return true;
            }
        }

        for (int slot = 0; slot < simulated.size(); slot++) {
            ItemStack current = simulated.get(slot);

            if (!current.isEmpty()) {
                continue;
            }

            int accepted = Math.min(stack.getCount(), Math.min(items.getSlotLimit(slot), stack.getMaxStackSize()));
            ItemStack placed = stack.copy();
            placed.setCount(accepted);
            simulated.set(slot, placed);
            stack.shrink(accepted);

            if (stack.isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private static List<FluidPlacement> planFluidPlacements(BulkFermenterBlockEntity fermenter, IFluidHandler fluids, List<FluidStack> outputs) {
        List<FluidPlacement> placements = new ArrayList<>();
        List<FluidStack> simulated = new ArrayList<>();
        List<Integer> capacities = new ArrayList<>();

        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            simulated.add(fluids.getFluidInTank(tank).copy());
            capacities.add(fermenter.getTankSize(tank));
        }

        for (FluidStack output : outputs) {
            if (output.isEmpty()) {
                continue;
            }

            FluidStack stack = output.copy();
            int target = findMatchingTank(simulated, capacities, stack);

            if (target < 0) {
                target = findEmptyTank(simulated, capacities, stack);
            }

            if (target < 0) {
                return null;
            }

            FluidStack current = simulated.get(target);

            if (current.isEmpty()) {
                simulated.set(target, stack.copy());
            } else {
                current.setAmount(current.getAmount() + stack.getAmount());
            }

            placements.add(new FluidPlacement(target, stack));
        }

        return placements;
    }

    private static int findMatchingTank(List<FluidStack> simulated, List<Integer> capacities, FluidStack stack) {
        for (int tank = 0; tank < simulated.size(); tank++) {
            FluidStack current = simulated.get(tank);

            if (current.isEmpty() || !sameFluidAndTag(current, stack)) {
                continue;
            }

            if (capacities.get(tank) - current.getAmount() >= stack.getAmount()) {
                return tank;
            }
        }

        return -1;
    }

    private static int findEmptyTank(List<FluidStack> simulated, List<Integer> capacities, FluidStack stack) {
        for (int tank = 0; tank < simulated.size(); tank++) {
            if (simulated.get(tank).isEmpty() && capacities.get(tank) >= stack.getAmount()) {
                return tank;
            }
        }

        return -1;
    }

    private static boolean sameFluidAndTag(FluidStack first, FluidStack second) {
        return first.getFluid() == second.getFluid() && Objects.equals(first.getTag(), second.getTag());
    }

    private static List<ItemStack> consumedItems(BulkFermentingRecipe recipe, IItemHandler items) {
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

                ItemStack extracted = stack.copy();
                extracted.setCount(1);

                if (!ingredient.test(extracted)) {
                    continue;
                }

                usedFromSlot[slot]++;
                stacks.add(extracted);
                continue Ingredients;
            }
        }

        return stacks;
    }

    private static List<FluidStack> consumedFluids(BulkFermentingRecipe recipe, IFluidHandler fluids) {
        List<FluidStack> stacks = new ArrayList<>();

        if (fluids == null) {
            return stacks;
        }

        int[] usedFromTank = new int[fluids.getTanks()];

        FluidIngredients:
        for (FluidIngredient ingredient : recipe.getFluidIngredients()) {
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

    private static IItemHandler itemHandler(BulkFermenterBlockEntity fermenter) {
        return fermenter.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
    }

    private static IFluidHandler fluidHandler(BulkFermenterBlockEntity fermenter) {
        return fermenter.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
    }

    private static BasinProcessingTicket currentTicket(BulkFermenterBlockEntity fermenter) {
        return fermenter instanceof BasinQualityTicketHolder holder ? holder.qualityFoodFluids$getTicket() : null;
    }

    private static void setTicket(BulkFermenterBlockEntity fermenter, BasinProcessingTicket ticket) {
        if (fermenter instanceof BasinQualityTicketHolder holder) {
            holder.qualityFoodFluids$setTicket(ticket);
        }
    }

    private static void clearTicket(BulkFermenterBlockEntity fermenter) {
        if (fermenter instanceof BasinQualityTicketHolder holder) {
            holder.qualityFoodFluids$clearTicket();
        }
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

    private static boolean hasQualityCapableOutput(List<ItemStack> outputItems, List<FluidStack> outputFluids) {
        return hasQualityCapableItemOutput(outputItems) || hasQualityCapableFluidOutput(outputFluids);
    }

    private static boolean hasQualityCapableItemOutput(List<ItemStack> outputItems) {
        for (ItemStack stack : outputItems) {
            if (!stack.isEmpty() && Utils.isValidItem(stack)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasQualityCapableFluidOutput(List<FluidStack> outputFluids) {
        for (FluidStack stack : outputFluids) {
            if (QualityFoodFluidsApi.canCarryQuality(stack)) {
                return true;
            }
        }

        return false;
    }

    private static List<ItemStack> baseItemOutputs(BulkFermentingRecipe recipe) {
        List<ItemStack> outputs = new ArrayList<>();

        for (ProcessingOutput output : recipe.getRollableResults()) {
            ItemStack stack = output.getStack();

            if (!stack.isEmpty()) {
                outputs.add(stack.copy());
            }
        }

        return outputs;
    }

    private static List<FluidStack> baseFluidOutputs(BulkFermentingRecipe recipe) {
        List<FluidStack> outputs = new ArrayList<>();

        for (FluidStack stack : recipe.getFluidResults()) {
            if (!stack.isEmpty()) {
                outputs.add(stack.copy());
            }
        }

        return outputs;
    }

    private static List<FluidStack> copyFluids(List<FluidStack> fluids) {
        List<FluidStack> copies = new ArrayList<>();

        for (FluidStack stack : fluids) {
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

    private record Context(BulkFermenterBlockEntity fermenter, BulkFermentingRecipe recipe, List<String> sourceSignature, List<ItemStack> consumedItems, List<FluidStack> consumedFluids) {
    }

    private record FluidPlacement(int tank, FluidStack stack) {
    }
}
