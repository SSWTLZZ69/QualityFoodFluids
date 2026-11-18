package io.github.createdelight.qualityfoodfluids.mixin;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.yision.fluidlogistics.content.processing.copperBasin.CopperBasinBlockEntity;
import io.github.createdelight.qualityfoodfluids.internal.BasinRecipeQualityContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 铜工作盆（FluidLogistics）产出流体时丢失品质。
 *
 * BasinBlockEntityMixin 的品质注入挂在 Create 原版 BasinBlockEntity#acceptOutputs 的 HEAD 上，
 * 而 CopperBasinBlockEntity 覆写了该方法：有流体产物时把流体从 super 调用里剥掉
 * （传 Collections.emptyList()），改由自己的 acceptFluidsIntoOutputTank 灌进 outputTank。
 * 于是父类的注入永远拿到空流体列表，守卫条件两个都不成立时直接放行，品质被静默跳过。
 *
 * 这里在子类方法开头主动走一次同样的上下文，让品质写回 outputFluids，
 * 铜工作盆随后灌罐即为带品质流体。返回值语义与父类保持一致：上下文判定不通过时取消本次接受。
 */
@Mixin(value = CopperBasinBlockEntity.class, remap = false)
public abstract class CopperBasinBlockEntityMixin {

    @Inject(method = "acceptOutputs", at = @At("HEAD"), cancellable = true, remap = false)
    private void qualityFoodFluids$applyCopperBasinFluidQuality(List<ItemStack> outputItems,
                                                                List<FluidStack> outputFluids,
                                                                boolean simulate,
                                                                CallbackInfoReturnable<Boolean> callback) {
        if (!BasinRecipeQualityContext.applyToOutputs((BasinBlockEntity) (Object) this, outputItems, outputFluids, simulate)) {
            callback.setReturnValue(false);
        }
    }
}
