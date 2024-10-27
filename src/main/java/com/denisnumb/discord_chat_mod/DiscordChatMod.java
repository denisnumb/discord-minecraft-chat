package com.denisnumb.discord_chat_mod;


import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.io.IOException;
import org.slf4j.Logger;

@Mod(DiscordChatMod.MODID)
public class DiscordChatMod
{
    private final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "discord_chat_mod";
    public static JDA jda;
    public static MinecraftServer server;
    public static TextChannel discordChannel;

    public DiscordChatMod()
    {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();

        try {
            jda = JDABuilder.createDefault(Config.discordBotToken)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new DiscordEvents())
                    .build();

            jda.awaitReady();
            discordChannel = jda.getTextChannelById(Config.discordChannelId);

            if (discordChannel == null)
                throw new NullPointerException("Invalid Discord Channel ID");
            else
                LOGGER.info("Discord Connected");
        } catch (Exception e) {
            LOGGER.error(String.format("DiscordConnectError: %s", e.getMessage()));
        }
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        jda.shutdown();
    }
}
