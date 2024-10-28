package com.denisnumb.discord_chat_mod;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.DiscordUtils.sendEmbedMessage;

@Mod.EventBusSubscriber(modid = DiscordChatMod.MODID)
public class MinecraftEvents {
    @SubscribeEvent
    public static void onChatMessage(ServerChatEvent event) {
        if (!isDiscordConnected() || !isServerStarted())
            return;

        discordChannel.sendMessage(String.format("`<%s>` %s", event.getPlayer().getName().getString(), event.getRawText())).queue();
    }

    @SubscribeEvent
    public static void onPlayerDieEvent(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        sendEmbedMessage(event.getSource().getLocalizedDeathMessage(event.getEntity()).getString(), 0);
    }

    @SubscribeEvent
    public static void onAdvancementMade(AdvancementEvent.AdvancementEarnEvent event) {
        DisplayInfo displayInfo = event.getAdvancement().getDisplay();
        if (displayInfo == null)
            return;

        sendEmbedMessage(String.format(
                "**%s** получил достижение **%s**",
                event.getEntity().getName().getString(),
                displayInfo.getTitle().getString()
        ), 0xf1c40f);
    }

    @SubscribeEvent
    public static void onPlayerJoinEvent(PlayerEvent.PlayerLoggedInEvent event) {
        joinLeaveEvent(event);
    }

    @SubscribeEvent
    public static void onPlayerLeaveEvent(PlayerEvent.PlayerLoggedOutEvent event){
        joinLeaveEvent(event);
    }

    private static void joinLeaveEvent(PlayerEvent event) {
        boolean isJoin = event instanceof PlayerEvent.PlayerLoggedInEvent;
        String message = isJoin ? "зашел на сервер" : "вышел с сервера";
        int color = isJoin ? 0x2ECC71 : 0xE74C3C;

        sendEmbedMessage(String.format("**%s** %s", event.getEntity().getName().getString(), message), color);
    }
}
