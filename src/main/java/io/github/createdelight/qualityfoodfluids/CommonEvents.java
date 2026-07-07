package io.github.createdelight.qualityfoodfluids;

import io.github.createdelight.qualityfoodfluids.world.WorldFluidQualityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = QualityFoodFluids.MODID)
public final class CommonEvents {
    private CommonEvents() {
    }

    @SubscribeEvent
    public static void syncWorldFluidQualityOnLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WorldFluidQualityData.syncAll(player);
        }
    }

    @SubscribeEvent
    public static void syncWorldFluidQualityOnDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WorldFluidQualityData.syncAll(player);
        }
    }

    @SubscribeEvent
    public static void syncWorldFluidQualityOnRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WorldFluidQualityData.syncAll(player);
        }
    }
}
