package com.denisnumb.discord_chat_mod;

import com.denisnumb.discord_chat_mod.chat_style.CustomChatTypeRegistry;
import com.denisnumb.discord_chat_mod.commands.ReloadConfigCommand;
import com.denisnumb.discord_chat_mod.commands.set_avatar.AvatarUrlStorage;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.config.IConfigProvider;
import com.denisnumb.discord_chat_mod.discord.*;
import com.denisnumb.discord_chat_mod.discord.chat_style.MessageType;
import com.denisnumb.discord_chat_mod.chat_style.Parameters;
import com.denisnumb.discord_chat_mod.discord.data_providers.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.CustomEmojiProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.StickersProvider;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.locale.DiscordLocaleProvider;
import com.mojang.logging.LogUtils;
import com.neovisionaries.ws.client.ProxySettings;
import com.neovisionaries.ws.client.WebSocketFactory;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.JDALogger;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.*;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils.*;
import static com.denisnumb.discord_chat_mod.discord.ServerStatusController.initServerStatusController;
import static com.denisnumb.discord_chat_mod.discord.ServerStatusController.updateServerStatusMessageToUnavailable;
import static com.denisnumb.discord_chat_mod.discord.utils.WebhookUtils.initWebhookSendExecutor;
import static com.denisnumb.discord_chat_mod.discord.utils.WebhookUtils.stopWebhookSendExecutor;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.*;
import com.denisnumb.discord_chat_mod.discord.slash_commands.DiscordSlashCommands;

public final class DiscordChatMod {
    public static final String MOD_ID = "discord_chat_mod";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static JDA jda;
    public static MinecraftServer server;
    private static final DiscordEvents discordEvents = new DiscordEvents();
    private static final AtomicBoolean serverStartPending = new AtomicBoolean(false);

    private static void trySendServerStartMessage() {
        if (!isDiscordConnected())
            return;
        if (DiscordChannelRegistry.getAllContexts().isEmpty())
            return;
        if (!serverStartPending.compareAndSet(true, false))
            return;

        getDiscordMessageComponents(MessageType.SERVER_START, Map.of())
                .ifPresent(components -> sendMessageFromServer(ChannelCategory.SERVER_START_STOP, DiscordChannelRegistry.getAllContexts(), components));
    }

    public static void onServerStarting(MinecraftServer minecraftServer) {
        server = minecraftServer;
        if (server.isPublished()) {
            Thread t = new Thread(DiscordChatMod::initJDA);
            t.setDaemon(true);
            t.start();
        }

        CustomChatTypeRegistry.registerChatTypes(server.registryAccess().lookupOrThrow(Registries.CHAT_TYPE));
    }

    public static void onServerStarted() {
        if (ConfigProvider.getConfig().isServerLogsToDiscordEnabled())
            ServerLogsRetranslator.start();

        if (server == null || !server.isDedicatedServer())
            return;

        serverStartPending.set(true);
        trySendServerStartMessage();
    }

    public static void onServerStopped() {
        serverStartPending.set(false);
        getDiscordMessageComponents(MessageType.SERVER_STOP, Map.of())
                .ifPresent(components -> sendMessageFromServer(ChannelCategory.SERVER_START_STOP, DiscordChannelRegistry.getAllContexts(), components));
        stopJDA();
    }

    public static void onIntegratedServerStarted(){
        Thread t = new Thread(() -> {
            initJDA();
            getDiscordMessageComponents(MessageType.LOCAL_SERVER_START,
                    Map.of(
                            Parameters.Translatable.LOCAL_SERVER_STARTED, DiscordLocaleProvider.Server.localStarted(server.getPort()),
                            Parameters.SERVER_PORT, String.valueOf(server.getPort())
                    )
            ).ifPresent(components -> sendMessageFromServer(ChannelCategory.SERVER_START_STOP, DiscordChannelRegistry.getAllContexts(), components));

            StickersProvider.loadClient(StickersProvider.getNameToUrlMap());
            CustomEmojiProvider.loadClient(CustomEmojiProvider.getNameToUrlMap());
            ChannelMembersProvider.CLIENT_MEMBER_CACHE = ChannelMembersProvider.getMemberData(ChannelCategory.PLAYER_CHAT);
        });
        t.setDaemon(true);
        t.start();
    }

    public static boolean isDiscordConnected() {
        return jda != null
                && jda.getStatus() == JDA.Status.CONNECTED
                && !ReloadConfigCommand.isReloadingNow;
    }

    public static void initJDA(){
        try {
            JDALogger.setFallbackLoggerEnabled(false);
            IConfigProvider config = ConfigProvider.getConfig();

            WebSocketFactory webSocketFactory = new WebSocketFactory();
            if (!config.proxyHostname().isEmpty()) {
                ProxySettings settings = webSocketFactory.getProxySettings();
                settings.setHost(config.proxyHostname()).setPort(config.proxyPort());
                if (!config.proxyUser().isEmpty()) {
                    settings.setCredentials(config.proxyUser(), config.proxyPassword());
                }
            }

            OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
            if (!config.proxyHostname().isEmpty()) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(config.proxyHostname(), config.proxyPort()));
                httpClientBuilder.proxy(proxy);
                if (!config.proxyUser().isEmpty()) {
                    httpClientBuilder.proxyAuthenticator((proxy1, url) -> {
                        String credential = Credentials.basic(config.proxyUser(), config.proxyPassword());
                        return url.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build();
                    });
                }
            }

            jda = JDABuilder.create(config.discordBotToken(),
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MEMBERS,
                            GatewayIntent.GUILD_PRESENCES,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_EXPRESSIONS)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .disableCache(CacheFlag.VOICE_STATE, CacheFlag.SCHEDULED_EVENTS)
                    .addEventListeners(discordEvents)
                    .setHttpClientBuilder(httpClientBuilder)
                    .setWebsocketFactory(webSocketFactory)
                    .build();

            jda.awaitReady();
            initWebhookSendExecutor();
            initDiscordSendExecutor();
            DiscordChannelRegistry.initDiscordChannels(config.discordGuildConfigs());
            initServerStatusController();
            DiscordSlashCommands.register(jda, DiscordChannelRegistry.getAllContexts());
            AvatarUrlStorage.load(server);

            if (config.isServerLogsToDiscordEnabled())
                ServerLogsRetranslator.init(config.serverLogsToDiscordLoggingLevel(), config.serverLogsPattern());

            LOGGER.info("Discord connected");
            trySendServerStartMessage();
        } catch (Exception e) {
            logErrorToServer(Component.literal(String.format("DiscordConnectError: %s", e.getMessage())));
            LOGGER.error("", e);
            stopJDA();
        }
    }

    public static void stopJDA() {
        if (jda != null) {
            DiscordSlashCommands.unregister(jda);
            updateServerStatusMessageToUnavailable();
            ServerLogsRetranslator.stop();
            stopWebhookSendExecutor();
            stopDiscordSendExecutor();
            AvatarUrlStorage.unload();
            LOGGER.info("Disconnecting from discord...");

            try {
                jda.removeEventListener(discordEvents);
                jda.awaitShutdown(Duration.ofMillis(5000));
                jda.cancelRequests();
                jda.shutdownNow();
                if (jda.getHttpClient() != null) {
                    jda.getHttpClient().dispatcher().executorService().shutdownNow();
                    jda.getHttpClient().connectionPool().evictAll();
                }
                jda.getGatewayPool().shutdownNow();
                jda.getCallbackPool().shutdownNow();
                jda.getRateLimitPool().shutdownNow();
            } catch (Exception ignored) {}
            jda = null;

            LOGGER.info("Discord disconnected");
        }
    }
}
