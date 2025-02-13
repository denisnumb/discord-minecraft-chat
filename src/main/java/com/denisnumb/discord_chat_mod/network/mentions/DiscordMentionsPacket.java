package com.denisnumb.discord_chat_mod.network.mentions;

import com.denisnumb.discord_chat_mod.discord.model.DiscordMemberData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion.MOD_ID;

public record DiscordMentionsPacket(List<DiscordMemberData> memberData) implements CustomPacketPayload {
    private static final Gson gson = new Gson();
    public static final CustomPacketPayload.Type<DiscordMentionsPacket> TYPE
            = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "/network/discord_mentions_packet"));


    public static final StreamCodec<ByteBuf, DiscordMentionsPacket> STREAM_CODEC = new StreamCodec<>() {
        public @NotNull DiscordMentionsPacket decode(@NotNull ByteBuf buffer) {
            return new DiscordMentionsPacket(gson.fromJson(new FriendlyByteBuf(buffer).readUtf(), new TypeToken<List<DiscordMemberData>>(){}.getType()));
        }

        public void encode(@NotNull ByteBuf buffer, DiscordMentionsPacket packet) {
            // ByteBufUtil.writeUtf8(buffer, gson.toJson(packet.memberData));
            new FriendlyByteBuf(buffer).writeUtf(gson.toJson(packet.memberData));
        }
    };

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
