package io.github.createdelight.qualityfoodfluids.compat.jei;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.helpers.IPlatformFluidHelper;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class QualityFoodFluidsJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(QualityFoodFluids.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (Item item : ForgeRegistries.ITEMS) {
            ItemStack stack = item.getDefaultInstance();

            if (item instanceof BucketItem || stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent()) {
                registration.registerSubtypeInterpreter(VanillaTypes.ITEM_STACK, item, this::itemSubtype);
            }
        }
    }

    @Override
    public <T> void registerFluidSubtypes(ISubtypeRegistration registration, IPlatformFluidHelper<T> platformFluidHelper) {
        for (Fluid fluid : ForgeRegistries.FLUIDS) {
            registration.registerSubtypeInterpreter(ForgeTypes.FLUID_STACK, fluid, this::fluidSubtype);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<FluidStack> qualityFluids = new ArrayList<>();

        for (Fluid fluid : ForgeRegistries.FLUIDS) {
            FluidStack stack = new FluidStack(fluid, 1000);

            if (QualityFoodFluidsApi.canCarryQuality(stack)) {
                qualityFluids.add(stack);
            }
        }

        if (!qualityFluids.isEmpty()) {
            registration.addIngredientInfo(qualityFluids, ForgeTypes.FLUID_STACK,
                    Component.translatable("jei.info.quality_food_fluids.quality_fluid"));
        }
    }

    private String itemSubtype(ItemStack stack, UidContext context) {
        Quality itemQuality = QualityFoodFluidsApi.getItemQuality(stack);
        FluidStack fluid = containedFluid(stack);
        Quality fluidQuality = QualityFoodFluidsApi.getQuality(fluid);
        int quality = Math.max(itemQuality.level(), fluidQuality.level());

        if (quality <= 0) {
            return IIngredientSubtypeInterpreter.NONE;
        }

        ResourceLocation fluidId = fluid.isEmpty() ? null : ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
        return "quality_food_fluids:quality=" + quality + ";fluid=" + (fluidId == null ? "empty" : fluidId);
    }

    private String fluidSubtype(FluidStack stack, UidContext context) {
        Quality quality = QualityFoodFluidsApi.getQuality(stack);

        if (quality.level() <= 0) {
            return IIngredientSubtypeInterpreter.NONE;
        }

        return "quality_food_fluids:quality=" + quality.level();
    }

    private static FluidStack containedFluid(ItemStack stack) {
        IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).orElse(null);

        if (handler == null) {
            return FluidStack.EMPTY;
        }

        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);

            if (!fluid.isEmpty()) {
                return fluid.copy();
            }
        }

        return FluidStack.EMPTY;
    }
}
