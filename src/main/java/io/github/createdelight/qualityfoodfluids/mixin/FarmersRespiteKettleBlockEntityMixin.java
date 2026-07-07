package io.github.createdelight.qualityfoodfluids.mixin;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsCreateRules;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.farmersrespite.common.block.entity.KettleBlockEntity;
import umpaz.farmersrespite.common.crafting.KettlePouringRecipe;
import umpaz.farmersrespite.common.crafting.KettleRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(value = KettleBlockEntity.class, remap = false)
public abstract class FarmersRespiteKettleBlockEntityMixin {
    @Shadow
    @Final
    private ItemStackHandler inventory;

    @Shadow
    @Final
    private FluidTank fluidTank;

    @Shadow
    private int brewTime;

    @Shadow
    public abstract Optional<KettlePouringRecipe> getPouringRecipe(Item item, FluidStack fluid);

    @Unique
    private Quality qualityFoodFluids$brewingQuality;

    @Unique
    private FluidStack qualityFoodFluids$extractFluidBefore = FluidStack.EMPTY;

    @Unique
    private ItemStack qualityFoodFluids$extractSourceBefore = ItemStack.EMPTY;

    @Unique
    private ItemStack qualityFoodFluids$pouringOutput = ItemStack.EMPTY;

    @ModifyArg(
            method = "getMatchingRecipe",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/FluidStack;isFluidEqual(Lnet/minecraftforge/fluids/FluidStack;)Z"),
            index = 0
    )
    private FluidStack qualityFoodFluids$ignoreQualityInCachedRecipeMatch(FluidStack stack) {
        return QualityFoodFluidsApi.stripQualityForComparison(stack);
    }

    @ModifyArg(
            method = "canBrew",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/FluidStack;areFluidStackTagsEqual(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/FluidStack;)Z"),
            index = 0
    )
    private FluidStack qualityFoodFluids$ignoreQualityInInputTagCheck(FluidStack stack) {
        return QualityFoodFluidsApi.stripQualityForComparison(stack);
    }

    @ModifyArg(
            method = "canBrew",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/FluidStack;areFluidStackTagsEqual(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/FluidStack;)Z"),
            index = 1
    )
    private FluidStack qualityFoodFluids$ignoreQualityInOutputTagCheck(FluidStack stack) {
        return QualityFoodFluidsApi.stripQualityForComparison(stack);
    }

    @Inject(method = "processBrewing", at = @At("HEAD"))
    private void qualityFoodFluids$captureBrewingQuality(KettleRecipe recipe, KettleBlockEntity kettle, CallbackInfoReturnable<Boolean> callback) {
        qualityFoodFluids$brewingQuality = null;

        if (brewTime + 1 < recipe.getBrewTime()) {
            return;
        }

        List<ItemStack> consumedItems = new ArrayList<>();
        for (int slot = 0; slot < 2; slot++) {
            consumedItems.add(inventory.getStackInSlot(slot).copy());
        }

        List<FluidStack> consumedFluids = new ArrayList<>();
        FluidStack fluidIngredient = recipe.getFluidIn();
        if (!fluidIngredient.isEmpty()) {
            FluidStack consumedFluid = fluidTank.getFluid().copy();
            consumedFluid.setAmount(fluidIngredient.getAmount());
            consumedFluids.add(consumedFluid);
        }

        qualityFoodFluids$brewingQuality = QualityFoodFluidsCreateRules.rollBasinQuality(consumedItems, consumedFluids);
    }

    @ModifyArg(
            method = "processBrewing",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/capability/templates/FluidTank;setFluid(Lnet/minecraftforge/fluids/FluidStack;)V"),
            index = 0
    )
    private FluidStack qualityFoodFluids$applyBrewingFluidOutputQuality(FluidStack stack) {
        return qualityFoodFluids$brewingQuality == null
                ? stack
                : QualityFoodFluidsCreateRules.applyFluidOutputQuality(qualityFoodFluids$brewingQuality, stack);
    }

    @Inject(method = "processBrewing", at = @At("RETURN"))
    private void qualityFoodFluids$clearBrewingQuality(KettleRecipe recipe, KettleBlockEntity kettle, CallbackInfoReturnable<Boolean> callback) {
        qualityFoodFluids$brewingQuality = null;
    }

    @Inject(method = "fluidExtract", at = @At("HEAD"))
    private void qualityFoodFluids$captureFluidExtract(KettleBlockEntity kettle, ItemStack input, ItemStack output, CallbackInfoReturnable<ItemStack> callback) {
        qualityFoodFluids$extractFluidBefore = fluidTank.getFluid().copy();
        qualityFoodFluids$extractSourceBefore = input.copy();
        qualityFoodFluids$pouringOutput = ItemStack.EMPTY;

        Optional<KettlePouringRecipe> recipe = getPouringRecipe(input.getItem(), qualityFoodFluids$extractFluidBefore);
        if (recipe.isPresent() && input.is(recipe.get().getContainer().getItem())) {
            qualityFoodFluids$pouringOutput = recipe.get().getOutput().copy();
        }
    }

    @ModifyArg(
            method = "fluidExtract",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/capability/templates/FluidTank;fill(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I"),
            index = 0
    )
    private FluidStack qualityFoodFluids$applyContainerQualityToFilledFluid(FluidStack stack) {
        return QualityFoodFluidsApi.copyContainerQualityToFluid(qualityFoodFluids$extractSourceBefore, stack);
    }

    @Inject(method = "fluidExtract", at = @At("RETURN"), cancellable = true)
    private void qualityFoodFluids$applyFluidExtractQuality(KettleBlockEntity kettle, ItemStack input, ItemStack output, CallbackInfoReturnable<ItemStack> callback) {
        try {
            FluidStack current = fluidTank.getFluid();
            int beforeAmount = qualityFoodFluids$extractFluidBefore.getAmount();
            int currentAmount = current.getAmount();

            if (currentAmount < beforeAmount && !qualityFoodFluids$pouringOutput.isEmpty()) {
                ItemStack result = callback.getReturnValue();
                if (!result.isEmpty() && ItemStack.isSameItem(result, qualityFoodFluids$pouringOutput) && qualityFoodFluids$canSafelyApplyToOutput(output, qualityFoodFluids$extractFluidBefore)) {
                    callback.setReturnValue(QualityFoodFluidsCreateRules.applyFillingQuality(qualityFoodFluids$extractFluidBefore, result));
                }
            } else if (currentAmount > beforeAmount && !current.isEmpty()) {
                fluidTank.setFluid(QualityFoodFluidsApi.copyContainerQualityToFluid(qualityFoodFluids$extractSourceBefore, current));
            }
        } finally {
            qualityFoodFluids$extractFluidBefore = FluidStack.EMPTY;
            qualityFoodFluids$extractSourceBefore = ItemStack.EMPTY;
            qualityFoodFluids$pouringOutput = ItemStack.EMPTY;
        }
    }

    @Unique
    private static boolean qualityFoodFluids$canSafelyApplyToOutput(ItemStack existingOutput, FluidStack sourceFluid) {
        if (existingOutput.isEmpty()) {
            return true;
        }

        return QualityFoodFluidsApi.getItemQuality(existingOutput).level() == QualityFoodFluidsApi.getQuality(sourceFluid).level();
    }
}
