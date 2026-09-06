package com.denisnumb.discord_chat_mod.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class PlatformPacketDistributor {
    private PlatformPacketDistributor() {}

    private static IPlatformPacketDistributor impl;

    public static void setHandler(IPlatformPacketDistributor handler) {
        impl = handler;
    }

    public static void sendToServer(CustomPacketPayload payload) {
        impl.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        impl.sendToPlayer(player, payload);
    }
}
