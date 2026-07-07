package io.github.createdelight.qualityfoodfluids.compat.jade;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import io.github.createdelight.qualityfoodfluids.api.QualityFoodFluidsApi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin(QualityFoodFluids.MODID)
public class QualityFoodFluidsJadePlugin implements IWailaPlugin {
    private static final String QUALITY_FLUID_STORAGE = "QualityFoodFluidsFluidStorage";
    private static final ResourceLocation WORLD_FLUID_QUALITY = new ResourceLocation(QualityFoodFluids.MODID, "world_fluid_quality");
    private static final ResourceLocation FLUID_STORAGE_QUALITY = new ResourceLocation(QualityFoodFluids.MODID, "fluid_storage_quality");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(FluidStorageQualityServerProvider.INSTANCE, BlockEntity.class);
    }

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

    private enum FluidStorageQualityServerProvider implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity blockEntity = accessor.getBlockEntity();
            if (blockEntity == null) {
                return;
            }

            IFluidHandler handler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER).orElse(null);
            if (handler == null) {
                return;
            }

            ListTag fluids = new ListTag();
            for (int tank = 0; tank < handler.getTanks(); tank++) {
                FluidStack stack = handler.getFluidInTank(tank);
                Quality quality = QualityFoodFluidsApi.getQuality(stack);

                if (stack.isEmpty() || quality.level() <= 0) {
                    continue;
                }

                ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(stack.getFluid());
                if (fluidId == null) {
                    continue;
                }

                CompoundTag fluidData = new CompoundTag();
                fluidData.putString("fluid", fluidId.toString());
                fluidData.putInt("amount", stack.getAmount());
                fluidData.putInt("quality", quality.level());
                fluids.add(fluidData);
            }

            if (!fluids.isEmpty()) {
                data.put(QUALITY_FLUID_STORAGE, fluids);
            }
        }

        @Override
        public ResourceLocation getUid() {
            return FLUID_STORAGE_QUALITY;
        }
    }

    private enum FluidStorageQualityProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag serverData = accessor.getServerData();

            if (!serverData.contains(QUALITY_FLUID_STORAGE, Tag.TAG_LIST)) {
                return;
            }

            ListTag fluids = serverData.getList(QUALITY_FLUID_STORAGE, Tag.TAG_COMPOUND);
            for (Tag tag : fluids) {
                if (!(tag instanceof CompoundTag fluidData)) {
                    continue;
                }

                FluidStack stack = readFluidStack(fluidData);
                Quality quality = Quality.get(fluidData.getInt("quality"));

                if (stack.isEmpty() || quality.level() <= 0) {
                    continue;
                }

                Component fluidName = stack.getDisplayName();
                tooltip.add(Component.translatable("tooltip.quality_food_fluids.storage_fluid_quality", fluidName, quality.getTranslation())
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        private static FluidStack readFluidStack(CompoundTag fluidData) {
            ResourceLocation fluidId = ResourceLocation.tryParse(fluidData.getString("fluid"));
            if (fluidId == null || !BuiltInRegistries.FLUID.containsKey(fluidId)) {
                return FluidStack.EMPTY;
            }

            Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
            int amount = Math.max(1, fluidData.getInt("amount"));

            return fluid == Fluids.EMPTY ? FluidStack.EMPTY : new FluidStack(fluid, amount);
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
