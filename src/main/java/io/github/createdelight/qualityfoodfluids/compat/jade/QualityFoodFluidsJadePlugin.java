package io.github.createdelight.qualityfoodfluids.compat.jade;

import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import io.github.createdelight.qualityfoodfluids.util.FluidQualityNames;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraftforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.Identifiers;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

@WailaPlugin(QualityFoodFluids.MODID)
public class QualityFoodFluidsJadePlugin implements IWailaPlugin {
    private static final ResourceLocation WORLD_FLUID_QUALITY = new ResourceLocation(QualityFoodFluids.MODID, "world_fluid_quality");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(WORLD_FLUID_QUALITY, true);
        registration.registerBlockComponent(WorldFluidQualityProvider.INSTANCE, LiquidBlock.class);
    }

    private enum WorldFluidQualityProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockState().getFluidState().isEmpty()
                    || tooltip.get(Identifiers.CORE_OBJECT_NAME).isEmpty()) {
                return;
            }

            FluidStack fluid = new FluidStack(accessor.getBlockState().getFluidState().getType(), 1000);

            if (!QualityFoodFluidsApi.canHaveWorldQuality(fluid)) {
                return;
            }

            fluid = QualityFoodFluidsApi.copyWorldQualityToFluid(accessor.getLevel(), accessor.getPosition(), fluid);
            // Jade's title formatter strips all child colors. Theme the base
            // name first, then add the quality prefix so its color survives.
            Component baseName = fluid.getFluid().getFluidType().getDescription(fluid);
            Component name = FluidQualityNames.withQuality(IThemeHelper.get().title(baseName),
                    QualityFoodFluidsApi.getQuality(fluid));
            tooltip.remove(Identifiers.CORE_OBJECT_NAME);
            tooltip.add(0, name, Identifiers.CORE_OBJECT_NAME);
        }

        @Override
        public ResourceLocation getUid() {
            return WORLD_FLUID_QUALITY;
        }
    }
}
