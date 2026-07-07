package io.github.createdelight.qualityfoodfluids.network;

import de.cadentem.quality_food.core.Quality;
import io.github.createdelight.qualityfoodfluids.world.WorldFluidQualityData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record ClientboundWorldFluidQualityPacket(ResourceLocation dimension, BlockPos pos, ResourceLocation fluidId, int quality, boolean remove) {
    public static ClientboundWorldFluidQualityPacket set(ResourceLocation dimension, BlockPos pos, ResourceLocation fluidId, Quality quality) {
        return new ClientboundWorldFluidQualityPacket(dimension, pos, fluidId, quality.level(), false);
    }

    public static ClientboundWorldFluidQualityPacket clear(ResourceLocation dimension, BlockPos pos) {
        return new ClientboundWorldFluidQualityPacket(dimension, pos, new ResourceLocation("minecraft", "empty"), 0, true);
    }

    public static void encode(ClientboundWorldFluidQualityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeResourceLocation(packet.dimension);
        buffer.writeBlockPos(packet.pos);
        buffer.writeResourceLocation(packet.fluidId);
        buffer.writeVarInt(packet.quality);
        buffer.writeBoolean(packet.remove);
    }

    public static ClientboundWorldFluidQualityPacket decode(FriendlyByteBuf buffer) {
        return new ClientboundWorldFluidQualityPacket(
                buffer.readResourceLocation(),
                buffer.readBlockPos(),
                buffer.readResourceLocation(),
                buffer.readVarInt(),
                buffer.readBoolean()
        );
    }

    public static void handle(ClientboundWorldFluidQualityPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> WorldFluidQualityData.applyClientSync(packet.dimension, packet.pos, packet.fluidId, Quality.get(packet.quality), packet.remove));
        ctx.setPacketHandled(true);
    }
}
