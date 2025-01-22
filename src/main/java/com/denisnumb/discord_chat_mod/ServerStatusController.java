package com.denisnumb.discord_chat_mod;

import com.denisnumb.discord_chat_mod.utils.DiscordUtils;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.server;
import static com.denisnumb.discord_chat_mod.utils.DiscordUtils.buildEmbed;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.getTranslate;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.*;

public class ServerStatusController {
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static long lastInvocationTime = 0;

    public static void updateServerStatusWithDelay() {
        if (!isDiscordConnected())
            return;
        lastInvocationTime = System.currentTimeMillis();
        scheduler.schedule(() -> {
            if (System.currentTimeMillis() - lastInvocationTime >= 10000) {
                updateServerStatus();
            }
        }, 10, TimeUnit.SECONDS);
    }

    private static void updateServerStatus(){
        if (Config.enablePinnedStatusMessage)
            serverStatusMessage.editMessageEmbeds(createServerStatusMessageEmbed()).queue();
        jda.getPresence().setActivity(Activity.customStatus(getOnlineCountString()));
    }

    public static void updateServerStatusMessageToUnavailable(){
        if (!isDiscordConnected() || !Config.enablePinnedStatusMessage)
            return;
        scheduler.shutdown();
        serverStatusMessage.editMessageEmbeds(buildEmbed(getTranslate(SERVER_UNAVAILABLE, "Server is unavailable"), DiscordUtils.Color.RED)).queue();
    }

    public static MessageEmbed createServerStatusMessageEmbed(){
        return server.getPlayerCount() == 0
                ? buildEmbed(getTranslate(SERVER_AVAILABLE, "There is no one on the server"), DiscordUtils.Color.DARK_GREEN)
                : buildEmbed(getOnlineCountString(), String.join("\n", server.getPlayerNames()), DiscordUtils.Color.GREEN);
    }

    private static String getOnlineCountString(){
        return String.format(
                getTranslate(ONLINE_PLAYERS,"Online [%d/%d]"),
                server.getPlayerCount(),
                server.getMaxPlayers()
        );
    }
}
