package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.foundation.fluid.FluidHelper;
import com.simibubi.create.foundation.fluid.FluidHelper.FluidExchange;
import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidHelper.class, remap = false)
public abstract class FluidHelperMixin {
    private static final ThreadLocal<Quality> QUALITY_FOOD_FLUIDS$EXCHANGE_ITEM_QUALITY = ThreadLocal.withInitial(() -> Quality.NONE);

    @Inject(
            method = "exchange(Lnet/minecraftforge/fluids/capability/IFluidHandler;Lnet/minecraftforge/fluids/capability/IFluidHandlerItem;Lcom/simibubi/create/foundation/fluid/FluidHelper$FluidExchange;ZI)Lcom/simibubi/create/foundation/fluid/FluidHelper$FluidExchange;",
            at = @At("HEAD")
    )
    private static void qualityFoodFluids$captureExchangeItemQuality(IFluidHandler tank, IFluidHandlerItem item, FluidExchange forcedDirection, boolean singleTank, int maxAmount, CallbackInfoReturnable<FluidExchange> callback) {
        QUALITY_FOOD_FLUIDS$EXCHANGE_ITEM_QUALITY.set(QualityFoodFluidsApi.getContainedFluidQuality(item));
    }

    @ModifyArg(
            method = "exchange(Lnet/minecraftforge/fluids/capability/IFluidHandler;Lnet/minecraftforge/fluids/capability/IFluidHandlerItem;Lcom/simibubi/create/foundation/fluid/FluidHelper$FluidExchange;ZI)Lcom/simibubi/create/foundation/fluid/FluidHelper$FluidExchange;",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fluids/capability/IFluidHandler;fill(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I"),
            index = 0
    )
    private static FluidStack qualityFoodFluids$applyExchangeItemQuality(FluidStack stack) {
        if (QualityFoodFluidsApi.hasQuality(stack)) {
            return stack;
        }

        Quality quality = QUALITY_FOOD_FLUIDS$EXCHANGE_ITEM_QUALITY.get();
        return quality.level() > 0 ? QualityFoodFluidsApi.withQuality(stack, quality) : stack;
    }

    @Inject(
            method = "exchange(Lnet/minecraftforge/fluids/capability/IFluidHandler;Lnet/minecraftforge/fluids/capability/IFluidHandlerItem;Lcom/simibubi/create/foundation/fluid/FluidHelper$FluidExchange;ZI)Lcom/simibubi/create/foundation/fluid/FluidHelper$FluidExchange;",
            at = @At("RETURN")
    )
    private static void qualityFoodFluids$clearExchangeItemQuality(IFluidHandler tank, IFluidHandlerItem item, FluidExchange forcedDirection, boolean singleTank, int maxAmount, CallbackInfoReturnable<FluidExchange> callback) {
        QUALITY_FOOD_FLUIDS$EXCHANGE_ITEM_QUALITY.remove();
    }
}
