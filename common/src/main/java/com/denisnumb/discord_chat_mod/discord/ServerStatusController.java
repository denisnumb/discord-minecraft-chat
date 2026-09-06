package com.denisnumb.discord_chat_mod.discord;

import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.config.IConfigProvider;
import com.denisnumb.discord_chat_mod.discord.chat_style.MessageType;
import com.denisnumb.discord_chat_mod.chat_style.Parameters;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.discord.model.DiscordGuildContext;
import com.denisnumb.discord_chat_mod.locale.DiscordLocaleProvider;
import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.requests.restaction.pagination.PinnedMessagePaginationAction;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.COUNTER;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.PLAYER;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils.*;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.*;

public final class ServerStatusController {
    private ServerStatusController() {}

    private static List<@Nullable Message> serverStatusMessages;
    @Nullable
    private static ScheduledExecutorService scheduler;
    private static long lastInvocationTime = 0;
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void updateServerStatusWithDelay() {
        if (!isDiscordConnected() || scheduler == null)
            return;
        lastInvocationTime = System.currentTimeMillis();
        scheduler.schedule(() -> {
            if (System.currentTimeMillis() - lastInvocationTime >= 10000) {
                updateServerStatus();
            }
        }, 10, TimeUnit.SECONDS);
    }

    public static void initServerStatusController() {
        scheduler = Executors.newSingleThreadScheduledExecutor();

        Map<DiscordGuildContext, Optional<Message>> existingStatusMessages = findPinnedStatusMessages();
        serverStatusMessages = existingStatusMessages.entrySet().stream().map(entry ->
            entry.getValue().orElseGet(() ->
                    sendPinnedStatusMessage(entry.getKey(), createServerStatusMessageComponents()).orElse(null)
            )
        ).toList();

        for (Message statusMessage : serverStatusMessages){
            if (statusMessage != null && !statusMessage.isPinned()) {
                try {
                    statusMessage.pin().queue();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage());
                }
            }
        }

        updateServerStatusWithDelay();
    }

    public static void updateServerStatusMessageToUnavailable() {
        if (scheduler != null){
            scheduler.close();
            scheduler.shutdownNow();
        }

        if (isDiscordConnected())
        {
            serverStatusMessages.forEach(statusMessage -> {
                if (statusMessage != null)
                    editMessage(statusMessage, getDiscordMessageComponents(MessageType.PINNED_STATUS_UNAVAILABLE, Map.of()).orElseThrow());
            });
        }
    }

    public static DiscordMessageComponents createServerStatusMessageComponents() {
        return getServerPlayerCount(server) == 0
                ? getDiscordMessageComponents(MessageType.PINNED_STATUS_AVAILABLE, Map.of()).orElseThrow()
                : getDiscordMessageComponents(
                MessageType.PINNED_STATUS_PLAYERS,
                Map.of(
                        Parameters.Translatable.ONLINE_PLAYERS, getOnlineCountString(),
                        Parameters.PLAYER_LIST, buildPlayerList(),
                        Parameters.PLAYER_COUNT, String.valueOf(getServerPlayerCount(server)),
                        Parameters.MAX_PLAYERS, String.valueOf(getServerMaxPlayers(server))
                )
        ).orElseThrow();
    }

    private static String buildPlayerList(){
        IConfigProvider config = ConfigProvider.getConfig();
        int maxNicknames = 50;

        String[] players = getServerPlayerNames(server);
        String delimiter = config.discordPinnedStatusMessagePlayerListDelimiter();
        String nicknameStyle = config.discordPinnedStatusMessagePlayerListNicknameStyle();
        boolean escapeUnderscore = shouldEscape(nicknameStyle);

        String result = IntStream.range(0, Math.min(maxNicknames, players.length))
                .mapToObj(i -> {
                    String player = players[i];
                    if (escapeUnderscore) {
                        player = player.replace("_", "\\_");
                    }

                    return nicknameStyle
                            .replace(PLAYER, player)
                            .replace(COUNTER, String.valueOf(i + 1));
                })
                .collect(Collectors.joining(delimiter));

        if (players.length > maxNicknames || players.length == 0)
            result += delimiter + ". . .";

        return result;
    }

    private static boolean shouldEscape(String style) {
        int playerIndex = style.indexOf(PLAYER);
        if (playerIndex == -1)
            return true;

        int lastBacktickBefore = style.lastIndexOf('`', playerIndex);
        int firstBacktickAfter = style.indexOf('`', playerIndex);

        boolean insideBackticks = lastBacktickBefore != -1
                && firstBacktickAfter != -1
                && lastBacktickBefore < playerIndex
                && firstBacktickAfter > playerIndex;

        return !insideBackticks;
    }

    private static void updateServerStatus() {
        if (isDiscordConnected()){
            serverStatusMessages.forEach(statusMessage -> {
                if (statusMessage != null)
                    editMessage(statusMessage, createServerStatusMessageComponents());
            });
        }

        if (ConfigProvider.getConfig().isBotPresenceStatusEnabled()) {
            jda.getPresence().setActivity(Activity.customStatus(getOnlineCountString()));
        } else {
            jda.getPresence().setActivity(null);
        }
    }

    private static String getOnlineCountString() {
        return DiscordLocaleProvider.Server.Status.onlinePlayers(
                getServerPlayerCount(server),
                getServerMaxPlayers(server)
        );
    }

    public static Optional<Message> sendPinnedStatusMessage(DiscordGuildContext guildContext, DiscordMessageComponents messageComponents) {
        if (!guildContext.enablePinnedStatusMessage)
            return Optional.empty();

        GuildMessageChannel channel = guildContext.getChannel(ChannelCategory.PINNED_STATUS);
        return prepareDiscordMessage(channel, messageComponents)
                .flatMap(mca -> sendDiscordMessage(mca, channel, true));
    }

    private static Map<DiscordGuildContext, Optional<Message>> findPinnedStatusMessages() {
        try {
            return DiscordChannelRegistry.getAllContexts()
                    .stream()
                    .map(ctx -> {
                        Optional<Message> statusMessage = ctx.getChannel(ChannelCategory.PINNED_STATUS).retrievePinnedMessages()
                                .complete()
                                .stream()
                                .map(PinnedMessagePaginationAction.PinnedMessage::getMessage)
                                .filter(message -> message.getAuthor().getId().equals(jda.getSelfUser().getId()))
                                .findFirst();

                        return Map.entry(ctx, statusMessage);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            return Map.of();
        }
    }
}
