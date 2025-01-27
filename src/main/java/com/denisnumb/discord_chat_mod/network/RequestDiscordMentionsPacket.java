package com.denisnumb.discord_chat_mod.network;

import com.denisnumb.discord_chat_mod.discord.ChannelMembersProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.discordChannel;

public class RequestDiscordMentionsPacket {
    public RequestDiscordMentionsPacket(){}
    public RequestDiscordMentionsPacket(FriendlyByteBuf buffer) {}
    public void encode(FriendlyByteBuf buffer) {}

    public static void handle(RequestDiscordMentionsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null)
                ModNetworking.sendToPlayer(new DiscordMentionsPacket(ChannelMembersProvider.getMemberData(discordChannel)), player);
        });
        context.setPacketHandled(true);
    }
}
