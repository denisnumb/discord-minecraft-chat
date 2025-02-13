package com.denisnumb.discord_chat_mod.network;

import com.denisnumb.discord_chat_mod.discord.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.network.mentions.DiscordMentionsPacket;
import com.denisnumb.discord_chat_mod.network.mentions.RequestDiscordMentionsPacket;
import com.denisnumb.discord_chat_mod.network.screenshot.ScreenshotPartPacket;
import com.denisnumb.discord_chat_mod.network.screenshot.ScreenshotReceiver;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.discordChannel;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.isDiscordConnected;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.getTranslate;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.SERVER_IS_NOT_CONNECTED_TO_DISCORD;

public class PacketHandler {
    public static void handleDiscordMentionsPacket(final DiscordMentionsPacket data, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (data.memberData() != null)
                ChannelMembersProvider.clientMemberData = data.memberData();
        });
    }

    public static void handleRequestDiscordMentionsPacket(final RequestDiscordMentionsPacket data, final IPayloadContext context){
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                PacketDistributor.sendToPlayer(player, new DiscordMentionsPacket(ChannelMembersProvider.getMemberData(discordChannel)));
        });
    }

    public static void handleScreenshotPartPacket(final ScreenshotPartPacket data, final IPayloadContext context){
        context.enqueueWork(() -> {
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player){
                    if (isDiscordConnected())
                        ScreenshotReceiver.receivePart(data, player);
                    else if (data.partIndex() == 0){
                        player.sendSystemMessage(
                                Component.literal(getTranslate(SERVER_IS_NOT_CONNECTED_TO_DISCORD, "Server is not connected to Discord"))
                                        .withStyle(ChatFormatting.RED)
                        );
                    }
                }
            });
        });
    }
}
