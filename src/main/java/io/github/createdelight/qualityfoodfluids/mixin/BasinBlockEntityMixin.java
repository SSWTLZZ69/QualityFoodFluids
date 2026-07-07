package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.internal.BasinProcessingTicket;
import io.github.createdelight.qualityfoodfluids.internal.BasinQualityTicketHolder;
import io.github.createdelight.qualityfoodfluids.internal.BasinRecipeQualityContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = BasinBlockEntity.class, remap = false)
public abstract class BasinBlockEntityMixin implements BasinQualityTicketHolder {
    @Unique
    private static final String QUALITY_FOOD_FLUIDS$TICKET_KEY = "QualityFoodFluidsTicket";

    @Unique
    private BasinProcessingTicket qualityFoodFluids$ticket;

    @Inject(method = "acceptOutputs", at = @At("HEAD"), cancellable = true)
    private void qualityFoodFluids$applyBasinFluidQuality(List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        if (!BasinRecipeQualityContext.applyToOutputs((BasinBlockEntity) (Object) this, outputItems, outputFluids, simulate)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "acceptOutputs", at = @At("RETURN"))
    private void qualityFoodFluids$finishBasinFluidQuality(List<ItemStack> outputItems, List<FluidStack> outputFluids, boolean simulate, CallbackInfoReturnable<Boolean> callback) {
        BasinRecipeQualityContext.finishOutputs((BasinBlockEntity) (Object) this, outputItems, outputFluids, simulate, callback.getReturnValue());
    }

    @Inject(method = "acceptFluidOutputsIntoBasin", at = @At("HEAD"), cancellable = true)
    private void qualityFoodFluids$acceptQualityFluidOutputs(List<FluidStack> outputFluids, boolean simulate, IFluidHandler targetTank, CallbackInfoReturnable<Boolean> callback) {
        if (!qualityFoodFluids$hasQualifiedFluid(outputFluids)) {
            return;
        }

        FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;

        for (FluidStack fluidStack : outputFluids) {
            if (qualityFoodFluids$fill(targetTank, fluidStack, action) != fluidStack.getAmount()) {
                callback.setReturnValue(false);
                return;
            }
        }

        callback.setReturnValue(true);
    }

    private boolean qualityFoodFluids$hasQualifiedFluid(List<FluidStack> outputFluids) {
        for (FluidStack fluidStack : outputFluids) {
            if (QualityFoodFluidsApi.hasQuality(fluidStack)) {
                return true;
            }
        }

        return false;
    }

    private int qualityFoodFluids$fill(IFluidHandler targetTank, FluidStack fluidStack, FluidAction action) {
        FluidStack copy = fluidStack.copy();

        if (targetTank instanceof SmartFluidTankBehaviour.InternalFluidHandler internalHandler) {
            return internalHandler.forceFill(copy, action);
        }

        return targetTank.fill(copy, action);
    }

    @Inject(method = "read", at = @At("RETURN"))
    private void qualityFoodFluids$readTicket(CompoundTag compound, boolean clientPacket, CallbackInfo callback) {
        qualityFoodFluids$ticket = compound.contains(QUALITY_FOOD_FLUIDS$TICKET_KEY)
                ? BasinProcessingTicket.load(compound.getCompound(QUALITY_FOOD_FLUIDS$TICKET_KEY))
                : null;
    }

    @Inject(method = "write", at = @At("RETURN"))
    private void qualityFoodFluids$writeTicket(CompoundTag compound, boolean clientPacket, CallbackInfo callback) {
        if (qualityFoodFluids$ticket != null) {
            compound.put(QUALITY_FOOD_FLUIDS$TICKET_KEY, qualityFoodFluids$ticket.save());
        }
    }

    @Override
    public BasinProcessingTicket qualityFoodFluids$getTicket() {
        return qualityFoodFluids$ticket;
    }

    @Override
    public void qualityFoodFluids$setTicket(BasinProcessingTicket ticket) {
        qualityFoodFluids$ticket = ticket;
    }
}
