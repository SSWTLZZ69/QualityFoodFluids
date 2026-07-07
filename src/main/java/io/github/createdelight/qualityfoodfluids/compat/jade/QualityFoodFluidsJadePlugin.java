package io.github.createdelight.qualityfoodfluids.compat.jade;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.view.ViewGroup;

import java.util.List;
import java.util.function.Function;

@WailaPlugin(QualityFoodFluids.MODID)
public class QualityFoodFluidsJadePlugin implements IWailaPlugin {
    private static final String JADE_FLUID_STORAGE = "JadeFluidStorage";
    private static final ResourceLocation WORLD_FLUID_QUALITY = new ResourceLocation(QualityFoodFluids.MODID, "world_fluid_quality");
    private static final ResourceLocation FLUID_STORAGE_QUALITY = new ResourceLocation(QualityFoodFluids.MODID, "fluid_storage_quality");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(WORLD_FLUID_QUALITY, true);
        registration.addConfig(FLUID_STORAGE_QUALITY, true);
        registration.registerBlockComponent(WorldFluidQualityProvider.INSTANCE, LiquidBlock.class);
        registration.registerBlockComponent(FluidStorageQualityProvider.INSTANCE, Block.class);
    }

    private enum WorldFluidQualityProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            if (accessor.getBlockState().getFluidState().isEmpty()) {
                return;
            }

            FluidStack fluid = new FluidStack(accessor.getBlockState().getFluidState().getType(), 1000);

            if (!QualityFoodFluidsApi.canHaveWorldQuality(fluid)) {
                return;
            }

            Quality quality = QualityFoodFluidsApi.getWorldQuality(accessor.getLevel(), accessor.getPosition());

            if (quality.level() <= 0) {
                return;
            }

            tooltip.add(Component.translatable("tooltip.quality_food_fluids.fluid_quality")
                    .append(quality.getTranslation())
                    .withStyle(ChatFormatting.GRAY));
        }

        @Override
        public ResourceLocation getUid() {
            return WORLD_FLUID_QUALITY;
        }
    }

    private enum FluidStorageQualityProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag serverData = accessor.getServerData();

            if (!serverData.contains(JADE_FLUID_STORAGE, Tag.TAG_LIST)) {
                return;
            }

            List<ViewGroup<CompoundTag>> groups = ViewGroup.readList(serverData, JADE_FLUID_STORAGE, Function.identity());

            if (groups == null || groups.isEmpty()) {
                return;
            }

            for (ViewGroup<CompoundTag> group : groups) {
                for (CompoundTag fluidData : group.views) {
                    FluidStack stack = readFluidStack(fluidData);

                    if (stack.isEmpty() || !QualityFoodFluidsApi.canCarryQuality(stack)) {
                        continue;
                    }

                    Quality quality = QualityFoodFluidsApi.getQuality(stack);

                    if (quality.level() <= 0) {
                        continue;
                    }

                    Component fluidName = stack.getDisplayName();
                    tooltip.add(Component.translatable("tooltip.quality_food_fluids.storage_fluid_quality", fluidName, quality.getTranslation())
                            .withStyle(ChatFormatting.GRAY));
                }
            }
        }

        private static FluidStack readFluidStack(CompoundTag fluidData) {
            ResourceLocation fluidId = ResourceLocation.tryParse(fluidData.getString("fluid"));

            if (fluidId == null || !BuiltInRegistries.FLUID.containsKey(fluidId)) {
                return FluidStack.EMPTY;
            }

            Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);

            if (fluid == Fluids.EMPTY) {
                return FluidStack.EMPTY;
            }

            int amount = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, fluidData.getLong("amount")));
            FluidStack stack = new FluidStack(fluid, amount);

            if (fluidData.contains("tag", Tag.TAG_COMPOUND)) {
                stack.setTag(fluidData.getCompound("tag"));
            }

            return stack;
        }

        @Override
        public ResourceLocation getUid() {
            return FLUID_STORAGE_QUALITY;
        }

        @Override
        public int getDefaultPriority() {
            return 1001;
        }
    }
}
