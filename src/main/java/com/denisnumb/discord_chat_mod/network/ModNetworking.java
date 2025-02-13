package com.denisnumb.discord_chat_mod.network;

import com.denisnumb.discord_chat_mod.DiscordChatMod;
import com.denisnumb.discord_chat_mod.network.mentions.DiscordMentionsPacket;
import com.denisnumb.discord_chat_mod.network.mentions.RequestDiscordMentionsPacket;
import com.denisnumb.discord_chat_mod.network.screenshot.ScreenshotPartPacket;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.MainThreadPayloadHandler;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = DiscordChatMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1").executesOn(HandlerThread.NETWORK);

        registrar.playToServer(
                RequestDiscordMentionsPacket.TYPE,
                RequestDiscordMentionsPacket.STREAM_CODEC,
                new MainThreadPayloadHandler<>(
                        PacketHandler::handleRequestDiscordMentionsPacket
                )
        );

        registrar.playToClient(
                DiscordMentionsPacket.TYPE,
                DiscordMentionsPacket.STREAM_CODEC,
                new MainThreadPayloadHandler<>(
                        PacketHandler::handleDiscordMentionsPacket
                )
        );

        registrar.playToServer(
                ScreenshotPartPacket.TYPE,
                ScreenshotPartPacket.STREAM_CODEC,
                new MainThreadPayloadHandler<>(
                        PacketHandler::handleScreenshotPartPacket
                )
        );
    }
}
