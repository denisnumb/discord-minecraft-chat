package com.denisnumb.discord_chat_mod;

import net.dv8tion.jda.api.EmbedBuilder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.awt.*;
import java.io.IOException;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;

@Mod.EventBusSubscriber(modid = DiscordChatMod.MODID)
public class MinecraftEvents {
    @SubscribeEvent
    public static void onChatMessage(ServerChatEvent event) {
        discordChannel.sendMessage(String.format("`<%s>` %s", event.getPlayer().getName().getString(), event.getRawText())).queue();
    }

    @SubscribeEvent
    public static void onPlayerDieEvent(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription(event.getSource().getLocalizedDeathMessage(event.getEntity()).getString());
        embedBuilder.setColor(0);
        discordChannel.sendMessageEmbeds(embedBuilder.build()).queue();
    }

    @SubscribeEvent
    public static void onAdvancementMade(AdvancementEvent.AdvancementEarnEvent event) {
        DisplayInfo displayInfo = event.getAdvancement().getDisplay();
        if (displayInfo == null)
            return;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription(String.format(
                "**%s** получил достижение **%s**",
                event.getEntity().getName().getString(),
                displayInfo.getTitle().getString()
        ));
        embedBuilder.setColor(0xf1c40f);
        discordChannel.sendMessageEmbeds(embedBuilder.build()).queue();
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
        String message = event instanceof PlayerEvent.PlayerLoggedInEvent
                ? "зашел на сервер"
                : "вышел с сервера";

        int color = event instanceof PlayerEvent.PlayerLoggedInEvent
                ? 0x2ECC71
                : 0xE74C3C;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription(String.format(
                "**%s** %s",
                event.getEntity().getName().getString(),
                message
        ));
        embedBuilder.setColor(color);
        discordChannel.sendMessageEmbeds(embedBuilder.build()).queue();
    }
}
