package io.github.createdelight.qualityfoodfluids.network;

import io.github.createdelight.qualityfoodfluids.QualityFoodFluids;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class QualityFoodFluidsNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static int id;

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(QualityFoodFluids.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private QualityFoodFluidsNetwork() {
    }

    public static void register() {
        CHANNEL.registerMessage(id++, ClientboundWorldFluidQualityPacket.class,
                ClientboundWorldFluidQualityPacket::encode,
                ClientboundWorldFluidQualityPacket::decode,
                ClientboundWorldFluidQualityPacket::handle);
    }

    public static void sendToPlayer(ServerPlayer player, ClientboundWorldFluidQualityPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToDimension(Iterable<ServerPlayer> players, ClientboundWorldFluidQualityPacket packet) {
        for (ServerPlayer player : players) {
            sendToPlayer(player, packet);
        }
    }
}
