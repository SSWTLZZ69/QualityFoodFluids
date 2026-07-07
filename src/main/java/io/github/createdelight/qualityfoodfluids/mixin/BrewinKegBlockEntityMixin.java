package io.github.createdelight.qualityfoodfluids.mixin;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsCreateRules;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import umpaz.brewinandchewin.common.block.entity.KegBlockEntity;
import umpaz.brewinandchewin.common.crafting.KegFermentingRecipe;
import umpaz.brewinandchewin.common.crafting.KegPouringRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(value = KegBlockEntity.class, remap = false)
public abstract class BrewinKegBlockEntityMixin {
    @Shadow
    @Final
    private ItemStackHandler inventory;

    @Shadow
    @Final
    private FluidTank fluidTank;

    @Shadow
    private int fermentTime;

    @Shadow
    public abstract Optional<KegPouringRecipe> getPouringRecipe(ItemStack stack);

    @Unique
    private Quality qualityFoodFluids$fermentingQuality;

    @Unique
    private FluidStack qualityFoodFluids$extractFluidBefore = FluidStack.EMPTY;

    @Unique
    private ItemStack qualityFoodFluids$extractSourceBefore = ItemStack.EMPTY;

    @Unique
    private ItemStack qualityFoodFluids$pouringOutput = ItemStack.EMPTY;

    @Inject(method = "processFermenting", at = @At("HEAD"))
    private void qualityFoodFluids$captureFermentingQuality(KegFermentingRecipe recipe, KegBlockEntity keg, CallbackInfoReturnable<Boolean> callback) {
        qualityFoodFluids$fermentingQuality = null;

        if (fermentTime + 1 < recipe.getFermentTime()) {
            return;
        }

        List<ItemStack> consumedItems = new ArrayList<>();
        for (int slot = 0; slot < 4; slot++) {
            consumedItems.add(inventory.getStackInSlot(slot).copy());
        }

        List<FluidStack> consumedFluids = new ArrayList<>();
        FluidStack fluidIngredient = recipe.getFluidIngredient();
        if (fluidIngredient != null && !fluidIngredient.isEmpty()) {
            FluidStack consumedFluid = fluidTank.getFluid().copy();
            consumedFluid.setAmount(fluidIngredient.getAmount());
            consumedFluids.add(consumedFluid);
        }

        qualityFoodFluids$fermentingQuality = QualityFoodFluidsCreateRules.rollBasinQuality(consumedItems, consumedFluids);
    }

    @ModifyArg(
            method = "processFermenting",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/items/ItemStackHandler;insertItem(ILnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;"),
            index = 1
    )
    private ItemStack qualityFoodFluids$applyFermentingItemQuality(ItemStack stack) {
        return qualityFoodFluids$fermentingQuality == null
                ? stack
                : QualityFoodFluidsCreateRules.applyItemOutputQuality(qualityFoodFluids$fermentingQuality, stack);
    }

    @ModifyArg(
            method = "processFermenting",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/capability/templates/FluidTank;setFluid(Lnet/minecraftforge/fluids/FluidStack;)V"),
            index = 0
    )
    private FluidStack qualityFoodFluids$applyFermentingFluidOutputQuality(FluidStack stack) {
        return qualityFoodFluids$fermentingQuality == null
                ? stack
                : QualityFoodFluidsCreateRules.applyFluidOutputQuality(qualityFoodFluids$fermentingQuality, stack);
    }

    @Inject(method = "processFermenting", at = @At("RETURN"))
    private void qualityFoodFluids$clearFermentingQuality(KegFermentingRecipe recipe, KegBlockEntity keg, CallbackInfoReturnable<Boolean> callback) {
        qualityFoodFluids$fermentingQuality = null;
    }

    @Inject(method = "fluidExtract", at = @At("HEAD"))
    private void qualityFoodFluids$captureFluidExtract(ItemStack stack, int servings, boolean simulate, boolean forceStack, CallbackInfoReturnable<List<ItemStack>> callback) {
        qualityFoodFluids$extractFluidBefore = fluidTank.getFluid().copy();
        qualityFoodFluids$extractSourceBefore = stack.copy();
        qualityFoodFluids$pouringOutput = ItemStack.EMPTY;

        Optional<KegPouringRecipe> recipe = getPouringRecipe(stack);
        if (recipe.isPresent() && ItemStack.isSameItemSameTags(stack, recipe.get().getContainer())) {
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
    private void qualityFoodFluids$applyFluidExtractQuality(ItemStack stack, int servings, boolean simulate, boolean forceStack, CallbackInfoReturnable<List<ItemStack>> callback) {
        try {
            if (simulate) {
                return;
            }

            FluidStack current = fluidTank.getFluid();
            int beforeAmount = qualityFoodFluids$extractFluidBefore.getAmount();
            int currentAmount = current.getAmount();

            if (currentAmount < beforeAmount && !qualityFoodFluids$pouringOutput.isEmpty()) {
                List<ItemStack> results = new ArrayList<>(callback.getReturnValue());
                Quality quality = QualityFoodFluidsApi.getQuality(qualityFoodFluids$extractFluidBefore);
                for (int i = 0; i < results.size(); i++) {
                    ItemStack result = results.get(i);
                    if (ItemStack.isSameItem(result, qualityFoodFluids$pouringOutput)) {
                        results.set(i, QualityFoodFluidsCreateRules.applyFillingQuality(quality, result));
                    }
                }
                callback.setReturnValue(results);
            } else if (currentAmount > beforeAmount && !current.isEmpty()) {
                fluidTank.setFluid(QualityFoodFluidsApi.copyContainerQualityToFluid(qualityFoodFluids$extractSourceBefore, current));
            }
        } finally {
            qualityFoodFluids$extractFluidBefore = FluidStack.EMPTY;
            qualityFoodFluids$extractSourceBefore = ItemStack.EMPTY;
            qualityFoodFluids$pouringOutput = ItemStack.EMPTY;
        }
    }
}
