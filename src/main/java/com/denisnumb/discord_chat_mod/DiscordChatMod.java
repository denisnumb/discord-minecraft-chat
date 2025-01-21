package com.denisnumb.discord_chat_mod;


import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
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
import java.util.Optional;

import static com.denisnumb.discord_chat_mod.ModLanguageKey.*;
import static com.denisnumb.discord_chat_mod.ServerStatusController.*;
import static com.denisnumb.discord_chat_mod.DiscordUtils.*;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.*;

@Mod(DiscordChatMod.MODID)
public class DiscordChatMod
{
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MODID = "discord_chat_mod";
    public static JDA jda;
    public static MinecraftServer server;
    public static MessageChannel discordChannel;
    public static Message serverStatusMessage;
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
        sendShortEmbedMessage(getTranslate(SERVER_STARTED, "Server started"), DiscordUtils.Color.GREEN);
    }

    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent ignored) {
        sendShortEmbedMessage(getTranslate(SERVER_CLOSED, "Server closed"), DiscordUtils.Color.RED);
        stopJDA();
    }

    public static void onIntegratedServerStarted(){
        new Thread(() -> {
            initJDA();
            sendShortEmbedMessage(String.format(getTranslate(
                    LOCAL_SERVER_STARTED,
                    "Local server started [`%d`]"
                    ), server.getPort()), DiscordUtils.Color.GREEN);
        }).start();
    }

    public static boolean isDiscordConnected() {
        return jda != null && discordChannel != null;
    }

    private static void initJDA(){

        try {
            jda = JDABuilder.createDefault(Config.discordBotToken)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                    .addEventListeners(new DiscordEvents())
                    .build();

            jda.awaitReady();
            discordChannel = jda.getChannelById(MessageChannel.class, Config.discordChannelId);

            if (discordChannel == null)
                throw new NullPointerException("Invalid Discord Channel ID");

            if (Config.enablePinnedStatusMessage){
                Optional<Message> statusMessage = findPinnedStatusMessage();
                serverStatusMessage = statusMessage.orElseGet(() -> discordChannel.sendMessageEmbeds(createServerStatusMessageEmbed()).complete());

                if (statusMessage.isEmpty() && !serverStatusMessage.isPinned())
                    serverStatusMessage.pin().queue();
                updateServerStatusWithDelay();
            }

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
