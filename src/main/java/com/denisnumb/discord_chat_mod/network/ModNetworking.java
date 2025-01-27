package com.denisnumb.discord_chat_mod.network;

import com.denisnumb.discord_chat_mod.DiscordChatMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

@Mod.EventBusSubscriber(modid = DiscordChatMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder.named(
                    new ResourceLocation(DiscordChatMod.MODID, "main"))
            .serverAcceptedVersions(status -> true)
            .clientAcceptedVersions(status -> true)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();


    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CHANNEL.messageBuilder(RequestDiscordMentionsPacket.class, 0, NetworkDirection.PLAY_TO_SERVER)
                    .encoder(RequestDiscordMentionsPacket::encode)
                    .decoder(RequestDiscordMentionsPacket::new)
                    .consumerMainThread(RequestDiscordMentionsPacket::handle)
                    .add();

            CHANNEL.messageBuilder(DiscordMentionsPacket.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                    .encoder(DiscordMentionsPacket::encode)
                    .decoder(DiscordMentionsPacket::new)
                    .consumerMainThread(DiscordMentionsPacket::handle)
                    .add();
        });
    }

    public static void sendToServer(Object msg){
        CHANNEL.send(PacketDistributor.SERVER.noArg(), msg);
    }

    public static void sendToPlayer(Object msg, ServerPlayer player){
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static void sendToAllClients(Object msg){
        CHANNEL.send(PacketDistributor.ALL.noArg(), msg);
    }
}
