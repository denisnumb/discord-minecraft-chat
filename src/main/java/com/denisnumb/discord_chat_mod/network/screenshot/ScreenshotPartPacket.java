package com.denisnumb.discord_chat_mod.network.screenshot;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;


import static net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion.MOD_ID;

public record ScreenshotPartPacket(
        long imageId,
        int partIndex,
        int totalParts,
        byte[] data
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ScreenshotPartPacket> TYPE
            = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "/network/screenshot_part_packet"));


    public static final StreamCodec<ByteBuf, ScreenshotPartPacket> STREAM_CODEC = new StreamCodec<>() {
        public @NotNull ScreenshotPartPacket decode(@NotNull ByteBuf buffer) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            return new ScreenshotPartPacket(
                    buf.readLong(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readByteArray()
            );
        }

        public void encode(@NotNull ByteBuf buffer, ScreenshotPartPacket packet) {
            FriendlyByteBuf buf = new FriendlyByteBuf(buffer);
            buf.writeLong(packet.imageId);
            buf.writeInt(packet.partIndex);
            buf.writeInt(packet.totalParts);
            buf.writeByteArray(packet.data);
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
