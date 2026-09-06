package com.denisnumb.discord_chat_mod.config.configs;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.denisnumb.discord_chat_mod.config.ConfigComments.*;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.*;

public final class LogsConfig {
    private LogsConfig() {}
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean logDiscordMessages;
    public static boolean logDiscordErrorsToServerChat;
    public static String discordErrorsChatPlayerSelector;
    public static boolean serverLogsToDiscordEnabled;
    public static String serverLogsToDiscordLoggingLevel;
    public static String serverLogsPattern;
    public static boolean commandLogEnabled;
    public static int commandLogMinPermissionLevel;
    public static Set<String> commandLogIgnoredCommands;

    public static CommentedConfig loadLogsConfig(CommentedConfig commonConfig){
        CommentedConfig existedLogsConfig = commonConfig.getOrElse("logsConfig", commonConfig.createSubConfig());
        CommentedConfig logsConfig = commonConfig.createSubConfig();

        logDiscordMessages = migrateOrElse(existedLogsConfig, commonConfig, "logDiscordMessages", LOG_DISCORD_MESSAGES_DEFAULT);
        logsConfig.set("logDiscordMessages", logDiscordMessages);
        logsConfig.setComment("logDiscordMessages", LOG_DISCORD_MESSAGES_COMMENT);

        logDiscordErrorsToServerChat = migrateOrElse(existedLogsConfig, commonConfig, "logDiscordErrorsToServerChat", LOG_DISCORD_ERRORS_TO_SERVER_CHAT_DEFAULT);
        logsConfig.set("logDiscordErrorsToServerChat", logDiscordErrorsToServerChat);
        logsConfig.setComment("logDiscordErrorsToServerChat", LOG_DISCORD_ERRORS_TO_SERVER_CHAT_COMMENT);

        discordErrorsChatPlayerSelector = migrateOrElse(existedLogsConfig, commonConfig, "discordErrorsChatPlayerSelector", DISCORD_ERRORS_CHAT_PLAYER_SELECTOR_DEFAULT);
        logsConfig.set("discordErrorsChatPlayerSelector", discordErrorsChatPlayerSelector);
        logsConfig.setComment("discordErrorsChatPlayerSelector", DISCORD_ERRORS_CHAT_PLAYER_SELECTOR_COMMENT);

        serverLogsToDiscordEnabled = migrateOrElse(existedLogsConfig, commonConfig, "serverLogsToDiscordEnabled", SERVER_LOGS_TO_DISCORD_ENABLED_DEFAULT);
        logsConfig.set("serverLogsToDiscordEnabled", serverLogsToDiscordEnabled);
        logsConfig.setComment("serverLogsToDiscordEnabled", SERVER_LOGS_TO_DISCORD_ENABLED_COMMENT);

        serverLogsToDiscordLoggingLevel = migrateOrElse(existedLogsConfig, commonConfig, "serverLogsToDiscordLoggingLevel", SERVER_LOGS_TO_DISCORD_LOGGING_LEVEL_DEFAULT);
        if (!serverLogsToDiscordLoggingLevel.matches("(?i)^(INFO|WARN|ERROR)$"))
            serverLogsToDiscordLoggingLevel = SERVER_LOGS_TO_DISCORD_LOGGING_LEVEL_DEFAULT;
        logsConfig.set("serverLogsToDiscordLoggingLevel", serverLogsToDiscordLoggingLevel);
        logsConfig.setComment("serverLogsToDiscordLoggingLevel", SERVER_LOGS_TO_DISCORD_LOGGING_LEVEL_COMMENT);

        serverLogsPattern = migrateOrElse(existedLogsConfig, commonConfig, "serverLogsPattern", SERVER_LOGS_PATTERN_DEFAULT);
        logsConfig.set("serverLogsPattern", serverLogsPattern);
        logsConfig.setComment("serverLogsPattern", SERVER_LOGS_PATTERN_COMMENT);

        commandLogEnabled = migrateOrElse(existedLogsConfig, commonConfig, "commandLogEnabled", COMMAND_LOG_ENABLED_DEFAULT);
        logsConfig.set("commandLogEnabled", commandLogEnabled);
        logsConfig.setComment("commandLogEnabled", COMMAND_LOG_ENABLED_COMMENT);

        commandLogMinPermissionLevel = migrateOrElse(existedLogsConfig, commonConfig, "commandLogMinPermissionLevel", COMMAND_LOG_MIN_PERMISSION_LEVEL_DEFAULT);
        if (commandLogMinPermissionLevel < 0) commandLogMinPermissionLevel = 0;
        if (commandLogMinPermissionLevel > 4) commandLogMinPermissionLevel = 4;
        logsConfig.set("commandLogMinPermissionLevel", commandLogMinPermissionLevel);
        logsConfig.setComment("commandLogMinPermissionLevel", COMMAND_LOG_MIN_PERMISSION_LEVEL_COMMENT);

        List<String> ignoredCommandsRaw = migrateOrElse(existedLogsConfig, commonConfig, "commandLogIgnoredCommands", COMMAND_LOG_IGNORED_COMMANDS_DEFAULT);
        commandLogIgnoredCommands = ignoredCommandsRaw.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        logsConfig.set("commandLogIgnoredCommands", ignoredCommandsRaw);
        logsConfig.setComment("commandLogIgnoredCommands", COMMAND_LOG_IGNORED_COMMANDS_COMMENT);

        return logsConfig;
    }

    /**
     * Migration helper: reads a value first from {@code newConfig}, then falls back to {@code legacyConfig}
     * (removing it there to avoid duplication). Used to migrate fields that were moved from
     * commonConfig root into logsConfig in version 2.6.3.
     *
     * @deprecated Migration from pre-2.6.4 configs. Remove this method in version 2.6.4
     */
    @Deprecated(since = "2.6.3", forRemoval = true)
    private static <T> T migrateOrElse(CommentedConfig logsConfig, CommentedConfig commonConfig, String key, T defaultValue) {
        if (logsConfig.contains(key))
            return logsConfig.get(key);

        if (commonConfig.contains(key)) {
            try{
                LOGGER.info("Migrating \"{}\" from commonConfig to logsConfig", key);
                T value = commonConfig.get(key);
                commonConfig.remove(key);
                return value;
            } catch (Exception ignored) {}
        }

        return defaultValue;
    }
}
