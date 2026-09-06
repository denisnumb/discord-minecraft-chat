package com.denisnumb.discord_chat_mod.network.emoji;


import com.denisnumb.discord_chat_mod.discord.data_providers.CustomEmojiProvider;
import com.denisnumb.discord_chat_mod.network.BigPacketsTransceiver;
import com.denisnumb.discord_chat_mod.network.PlatformPacketDistributor;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.denisnumb.discord_chat_mod.discord.data_providers.CustomEmojiProvider.loadClient;

public final class DiscordEmojisTransceiver {
    private DiscordEmojisTransceiver() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Long, ArrayList<byte[]>> receivedParts = new HashMap<>();
    private static final Gson gson = new Gson();
    private static final Type gsonType = new TypeToken<Map<String, String>>(){}.getType();
    private static long lastRequestTime = 0;

    public static void requestDiscordEmojis(){
        long requestTime = System.currentTimeMillis();
        if (requestTime - lastRequestTime < 30000)
            return;

        lastRequestTime = requestTime;
        try {
            PlatformPacketDistributor.sendToServer(new RequestDiscordEmojisPacket());
        } catch (Exception e){
            LOGGER.error("RequestDiscordEmojisError: " + e.getMessage());
        }

    }

    public static void sendDiscordEmojisDataToPlayer(ServerPlayer player) {
        long sendTime = System.currentTimeMillis();
        byte[] data = gson.toJson(CustomEmojiProvider.getNameToUrlMap(), gsonType).getBytes();
        BigPacketsTransceiver.send(data, (partIndex, totalParts, part) ->
                PlatformPacketDistributor.sendToPlayer(player, new DiscordEmojisPartPacket(sendTime, partIndex, totalParts, part))
        );
    }

    public static void receiveDiscordEmojisPart(DiscordEmojisPartPacket packet) {
        BigPacketsTransceiver.receivePart(
                receivedParts,
                packet.sendTime(),
                packet.partIndex(),
                packet.totalParts(),
                packet.data()
        ).ifPresent(data -> loadClient(gson.fromJson(new String(data), gsonType)));
    }
}
