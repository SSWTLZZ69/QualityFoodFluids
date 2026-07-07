package io.github.createdelight.qualityfoodfluids.world;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.network.ClientboundWorldFluidQualityPacket;
import io.github.createdelight.qualityfoodfluids.network.QualityFoodFluidsNetwork;
import io.github.createdelight.qualityfoodfluids.registry.QualityFoodFluidTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidStack;

import java.util.HashMap;
import java.util.Map;

public class WorldFluidQualityData extends SavedData {
    private static final String DATA_NAME = "quality_food_fluids_world_fluid_quality";
    private static final Map<ResourceLocation, Map<Long, WorldFluidQuality>> CLIENT_QUALITIES = new HashMap<>();

    private final Map<Long, WorldFluidQuality> qualities = new HashMap<>();

    public static WorldFluidQualityData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(WorldFluidQualityData::load, WorldFluidQualityData::new, DATA_NAME);
    }

    public static Quality getQuality(LevelAccessor level, BlockPos pos) {
        WorldFluidQualityData data = dataOrNull(level);

        if (pos == null) {
            return Quality.NONE;
        }

        if (data == null) {
            return getClientQuality(level, pos);
        }

        WorldFluidQuality entry = data.qualities.get(pos.asLong());

        if (entry == null) {
            return Quality.NONE;
        }

        if (!matchesCurrentSource(level, pos, entry)) {
            data.qualities.remove(pos.asLong());
            data.setDirty();
            return Quality.NONE;
        }

        return entry.quality();
    }

    public static void setQuality(LevelAccessor level, BlockPos pos, Quality quality) {
        if (level == null || pos == null) {
            return;
        }

        FluidState state = level.getFluidState(pos);
        setQuality(level, pos, new FluidStack(state.getType(), 1000), quality, "world");
    }

    public static void setQuality(LevelAccessor level, BlockPos pos, FluidStack stack, Quality quality, String source) {
        WorldFluidQualityData data = dataOrNull(level);

        if (data == null || pos == null || stack == null || stack.isEmpty() || quality == null || quality.level() <= 0) {
            clear(level, pos);
            return;
        }

        Fluid fluid = stack.getFluid();

        if (!fluid.defaultFluidState().is(QualityFoodFluidTags.WORLD_QUALITY_FLUIDS)) {
            clear(level, pos);
            return;
        }

        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        long time = level instanceof ServerLevel serverLevel ? serverLevel.getGameTime() : 0L;
        data.qualities.put(pos.asLong(), new WorldFluidQuality(fluidId, quality, source, time, stack.getAmount()));
        data.setDirty();

        if (level instanceof ServerLevel serverLevel) {
            QualityFoodFluidsNetwork.sendToDimension(serverLevel.players(),
                    ClientboundWorldFluidQualityPacket.set(serverLevel.dimension().location(), pos, fluidId, quality));
        }
    }

    public static void clear(LevelAccessor level, BlockPos pos) {
        WorldFluidQualityData data = dataOrNull(level);

        if (pos == null) {
            return;
        }

        if (data != null && data.qualities.remove(pos.asLong()) != null) {
            data.setDirty();

            if (level instanceof ServerLevel serverLevel) {
                QualityFoodFluidsNetwork.sendToDimension(serverLevel.players(),
                        ClientboundWorldFluidQualityPacket.clear(serverLevel.dimension().location(), pos));
            }
        }
    }

    public static void syncAll(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        ResourceLocation dimension = level.dimension().location();
        WorldFluidQualityData data = get(level);

        data.qualities.forEach((pos, entry) -> QualityFoodFluidsNetwork.sendToPlayer(player,
                ClientboundWorldFluidQualityPacket.set(dimension, BlockPos.of(pos), entry.fluidId(), entry.quality())));
    }

    public static void applyClientSync(ResourceLocation dimension, BlockPos pos, ResourceLocation fluidId, Quality quality, boolean remove) {
        if (dimension == null || pos == null) {
            return;
        }

        Map<Long, WorldFluidQuality> qualities = CLIENT_QUALITIES.computeIfAbsent(dimension, $ -> new HashMap<>());

        if (remove || quality == null || quality.level() <= 0) {
            qualities.remove(pos.asLong());
            return;
        }

        qualities.put(pos.asLong(), new WorldFluidQuality(fluidId, quality, "sync", 0L, 1000));
    }

    public static WorldFluidQualityData load(CompoundTag tag) {
        WorldFluidQualityData data = new WorldFluidQualityData();
        CompoundTag entries = tag.getCompound("entries");

        for (String key : entries.getAllKeys()) {
            CompoundTag entry = entries.getCompound(key);
            ResourceLocation fluidId = ResourceLocation.tryParse(entry.getString("fluid"));

            if (fluidId == null) {
                continue;
            }

            Quality quality = Quality.get(entry.getInt("quality"));

            if (quality.level() <= 0) {
                continue;
            }

            data.qualities.put(Long.parseLong(key), new WorldFluidQuality(
                    fluidId,
                    quality,
                    entry.getString("source"),
                    entry.getLong("placedGameTime"),
                    entry.getInt("volume")
            ));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag entries = new CompoundTag();

        qualities.forEach((pos, entry) -> {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("fluid", entry.fluidId().toString());
            entryTag.putInt("quality", entry.quality().level());
            entryTag.putString("source", entry.source());
            entryTag.putLong("placedGameTime", entry.placedGameTime());
            entryTag.putInt("volume", entry.volume());
            entries.put(String.valueOf(pos), entryTag);
        });

        tag.put("entries", entries);
        return tag;
    }

    private static WorldFluidQualityData dataOrNull(LevelAccessor level) {
        if (level instanceof ServerLevel serverLevel) {
            return get(serverLevel);
        }

        return null;
    }

    private static Quality getClientQuality(LevelAccessor level, BlockPos pos) {
        if (!(level instanceof Level clientLevel)) {
            return Quality.NONE;
        }

        Map<Long, WorldFluidQuality> qualities = CLIENT_QUALITIES.get(clientLevel.dimension().location());

        if (qualities == null) {
            return Quality.NONE;
        }

        WorldFluidQuality entry = qualities.get(pos.asLong());

        if (entry == null) {
            return Quality.NONE;
        }

        if (!matchesCurrentSource(level, pos, entry)) {
            qualities.remove(pos.asLong());
            return Quality.NONE;
        }

        return entry.quality();
    }

    private static boolean matchesCurrentSource(LevelAccessor level, BlockPos pos, WorldFluidQuality entry) {
        FluidState state = level.getFluidState(pos);

        if (!state.isSource()) {
            return false;
        }

        ResourceLocation currentFluidId = BuiltInRegistries.FLUID.getKey(state.getType());
        return entry.fluidId().equals(currentFluidId);
    }
}
