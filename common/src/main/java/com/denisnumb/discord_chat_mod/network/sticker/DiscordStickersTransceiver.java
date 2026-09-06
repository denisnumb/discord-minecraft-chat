package com.denisnumb.discord_chat_mod.network.sticker;


import com.denisnumb.discord_chat_mod.discord.data_providers.StickersProvider;
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

import static com.denisnumb.discord_chat_mod.discord.data_providers.StickersProvider.loadClient;

public final class DiscordStickersTransceiver {
    private DiscordStickersTransceiver() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<Long, ArrayList<byte[]>> receivedParts = new HashMap<>();
    private static final Gson gson = new Gson();
    private static final Type gsonType = new TypeToken<Map<String, String>>(){}.getType();
    private static long lastRequestTime = 0;

    public static void requestDiscordStickers(){
        long requestTime = System.currentTimeMillis();
        if (requestTime - lastRequestTime < 30000)
            return;

        lastRequestTime = requestTime;
        try {
            PlatformPacketDistributor.sendToServer(new RequestDiscordStickersPacket());
        } catch (Exception e){
            LOGGER.error("RequestDiscordStickersError", e);
        }

    }

    public static void sendDiscordStickersDataToPlayer(ServerPlayer player) {
        long sendTime = System.currentTimeMillis();
        byte[] data = gson.toJson(StickersProvider.getNameToUrlMap(), gsonType).getBytes();
        BigPacketsTransceiver.send(data, (partIndex, totalParts, part) ->
                PlatformPacketDistributor.sendToPlayer(player, new DiscordStickersPartPacket(sendTime, partIndex, totalParts, part))
        );
    }

    public static void receiveDiscordStickersPart(DiscordStickersPartPacket packet) {
        BigPacketsTransceiver.receivePart(
                receivedParts,
                packet.sendTime(),
                packet.partIndex(),
                packet.totalParts(),
                packet.data()
        ).ifPresent(data -> loadClient(gson.fromJson(new String(data), gsonType)));
    }
}
