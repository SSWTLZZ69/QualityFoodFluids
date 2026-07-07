package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.fluids.hosePulley.HosePulleyFluidHandler;
import io.github.createdelight.qualityfoodfluids.api.HosePulleyWorldQualityBridge;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = HosePulleyFluidHandler.class, remap = false)
public abstract class HosePulleyFluidHandlerMixin {
    @Inject(method = "fill", at = @At("HEAD"))
    private void qualityFoodFluids$beginDeposit(FluidStack resource, FluidAction action, CallbackInfoReturnable<Integer> callback) {
        HosePulleyWorldQualityBridge.beginDeposit(resource);
    }

    @Inject(method = "fill", at = @At("RETURN"))
    private void qualityFoodFluids$endDeposit(FluidStack resource, FluidAction action, CallbackInfoReturnable<Integer> callback) {
        HosePulleyWorldQualityBridge.endDeposit();
    }
}
