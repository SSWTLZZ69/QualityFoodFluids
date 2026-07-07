package io.github.createdelight.qualityfoodfluids.client;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

@Mod.EventBusSubscriber(modid = QualityFoodFluids.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void addFluidQualityTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (stack.isEmpty() || stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().isEmpty()) {
            return;
        }

        IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);

        if (handler == null) {
            return;
        }

        boolean containsFluid = false;

        for (int tank = 0; tank < handler.getTanks(); tank++) {
            if (!handler.getFluidInTank(tank).isEmpty()) {
                containsFluid = true;
                break;
            }
        }

        if (!containsFluid) {
            return;
        }

        Quality quality = QualityFoodFluidsApi.getItemOrContainedFluidQuality(stack);

        if (quality.level() <= 0) {
            return;
        }

        event.getToolTip().add(Component.translatable("tooltip.quality_food_fluids.fluid_quality")
                .append(quality.getTranslation())
                .withStyle(ChatFormatting.GRAY));
    }
}
