package com.denisnumb.discord_chat_mod;


import com.denisnumb.discord_chat_mod.discord.DiscordEvents;
import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static com.denisnumb.discord_chat_mod.ColorUtils.Color.GREEN;
import static com.denisnumb.discord_chat_mod.ColorUtils.Color.RED;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.*;
import static com.denisnumb.discord_chat_mod.discord.ServerStatusController.*;
import static com.denisnumb.discord_chat_mod.discord.DiscordUtils.*;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.*;

@Mod(DiscordChatMod.MODID)
public class DiscordChatMod
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "discord_chat_mod";
    public static JDA jda;
    public static MinecraftServer server;
    public static GuildMessageChannel discordChannel;
    public static final Map<String, Map<String, String>> localeStorage = new HashMap<>();


    public DiscordChatMod()
    {
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        server = event.getServer();
        if (server.isPublished())
            initJDA();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent ignored) {
        sendShortEmbedMessage(getTranslate(SERVER_STARTED, "Server started"), GREEN);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent ignored) {
        sendShortEmbedMessage(getTranslate(SERVER_CLOSED, "Server closed"), RED);
        stopJDA();
    }

    public static void onIntegratedServerStarted(){
        new Thread(() -> {
            initJDA();
            sendShortEmbedMessage(String.format(getTranslate(
                    LOCAL_SERVER_STARTED,
                    "Local server started [`%d`]"
                    ), server.getPort()), GREEN);
        }).start();
    }

    public static boolean isDiscordConnected() {
        return jda != null && discordChannel != null;
    }

    private static void initJDA(){

        try {
            jda = JDABuilder.create(Config.discordBotToken,
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_PRESENCES,
                            GatewayIntent.GUILD_MESSAGES)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .addEventListeners(new DiscordEvents())
                    .build();

            jda.awaitReady();
            discordChannel = jda.getChannelById(GuildMessageChannel.class, Config.discordChannelId);

            if (discordChannel == null)
                throw new NullPointerException("Invalid Discord Channel ID");

            if (Config.enablePinnedStatusMessage)
                initServerStatusMessage();

            LOGGER.info("Discord connected");
        } catch (Exception e) {
            logErrorToServer(String.format("DiscordConnectError: %s", e.getMessage()));
            stopJDA();
        }
    }

    private static void stopJDA() {
        if (jda != null) {
            updateServerStatusMessageToUnavailable();
            jda.shutdown();
            jda = null;
            LOGGER.info("Discord disconnected");
        }
    }
}
