package com.denisnumb.discord_chat_mod.network.mentions;

import com.denisnumb.discord_chat_mod.discord.data_providers.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.discord.model.DiscordUserData;
import com.denisnumb.discord_chat_mod.network.BigPacketsTransceiver;
import com.denisnumb.discord_chat_mod.network.PlatformPacketDistributor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public final class DiscordMentionsTransceiver {
    private DiscordMentionsTransceiver() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Long, ArrayList<byte[]>> receivedParts = new HashMap<>();
    private static final Gson gson = new Gson();
    private static final Type gsonType = new TypeToken<List<DiscordUserData>>(){}.getType();
    private static long lastRequestTime = 0;

    public static void requestDiscordMemberData(){
        long requestTime = System.currentTimeMillis();
        if (requestTime - lastRequestTime < 5000)
            return;

        lastRequestTime = requestTime;

        try {
            PlatformPacketDistributor.sendToServer(new RequestDiscordMentionsPacket());
        } catch (Exception e){
            LOGGER.error("RequestDiscordMentionsError: " + e.getMessage());
        }
    }

    public static void sendDiscordMemberDataToPlayer(ServerPlayer player) {
        long sendTime = System.currentTimeMillis();

        byte[] data = gson.toJson(ChannelMembersProvider.getMemberData(ChannelCategory.PLAYER_CHAT), gsonType).getBytes(StandardCharsets.UTF_8);
        BigPacketsTransceiver.send(data, (partIndex, totalParts, part) ->
                PlatformPacketDistributor.sendToPlayer(player, new DiscordMentionsPartPacket(sendTime, partIndex, totalParts, part))
        );
    }

    public static void receiveDiscordMentionsPart(DiscordMentionsPartPacket packet) {
        BigPacketsTransceiver.receivePart(
                receivedParts,
                packet.sendTime(),
                packet.partIndex(),
                packet.totalParts(),
                packet.data()
        ).ifPresent(bytes -> ChannelMembersProvider.CLIENT_MEMBER_CACHE = gson.fromJson(new String(bytes, StandardCharsets.UTF_8), gsonType));
    }
}
