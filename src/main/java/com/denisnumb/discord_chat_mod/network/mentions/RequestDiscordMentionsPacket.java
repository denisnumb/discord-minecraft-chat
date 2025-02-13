package com.denisnumb.discord_chat_mod.network.mentions;

import com.google.gson.Gson;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;


import static net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion.MOD_ID;

public class RequestDiscordMentionsPacket implements CustomPacketPayload {
    private static final Gson gson = new Gson();
    public static final CustomPacketPayload.Type<RequestDiscordMentionsPacket> TYPE
            = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "/network/request_discord_mentions_packet"));


    public static final StreamCodec<ByteBuf, RequestDiscordMentionsPacket> STREAM_CODEC = new StreamCodec<>() {
        public void encode(@NotNull ByteBuf buffer, @NotNull RequestDiscordMentionsPacket requestDiscordMentionsPacket) {}
        public @NotNull RequestDiscordMentionsPacket decode(@NotNull ByteBuf buffer) {
            return new RequestDiscordMentionsPacket();
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
