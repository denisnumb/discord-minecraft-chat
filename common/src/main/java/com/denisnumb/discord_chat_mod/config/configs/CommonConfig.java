package com.denisnumb.discord_chat_mod.config.configs;

import com.electronwill.nightconfig.core.CommentedConfig;

import static com.denisnumb.discord_chat_mod.config.ConfigComments.*;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.*;

public final class CommonConfig {
    private CommonConfig() {}

    public static String discordBotToken;
    public static String modLocale;
    public static int utcOffsetHours;
    public static boolean enableBotPresenceStatus;
    public static boolean mentionBots;

    public static void loadCommonConfig(CommentedConfig commonConfig){
        discordBotToken = commonConfig.getOrElse("discordBotToken", DISCORD_BOT_TOKEN_DEFAULT);
        commonConfig.set("discordBotToken", discordBotToken);
        commonConfig.setComment("discordBotToken", DISCORD_BOT_TOKEN_COMMENT);

        modLocale = commonConfig.getOrElse("modLocale", MOD_LOCALE_DEFAULT);
        commonConfig.set("modLocale", modLocale);
        commonConfig.setComment("modLocale", MOD_LOCALE_COMMENT);

        utcOffsetHours = commonConfig.getOrElse("utcOffsetHours", UTC_OFFSET_HOURS_DEFAULT);
        if (utcOffsetHours < -12) utcOffsetHours = -12;
        if (utcOffsetHours > 14) utcOffsetHours = 14;
        commonConfig.set("utcOffsetHours", utcOffsetHours);
        commonConfig.setComment("utcOffsetHours", UTC_OFFSET_HOURS_COMMENT + String.format("\n Default: %d\n Range: -12 ~ 14", UTC_OFFSET_HOURS_DEFAULT));

        enableBotPresenceStatus = commonConfig.getOrElse("enableBotPresenceStatus", ENABLE_BOT_PRESENCE_STATUS_DEFAULT);
        commonConfig.set("enableBotPresenceStatus", enableBotPresenceStatus);
        commonConfig.setComment("enableBotPresenceStatus", ENABLE_BOT_PRESENCE_STATUS_COMMENT);

        mentionBots = commonConfig.getOrElse("mentionBots", MENTION_BOTS_DEFAULT);
        commonConfig.set("mentionBots", mentionBots);
        commonConfig.setComment("mentionBots", MENTION_BOTS_COMMENT);
    }
}
