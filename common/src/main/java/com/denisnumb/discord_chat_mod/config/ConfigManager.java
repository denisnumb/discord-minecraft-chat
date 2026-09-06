package com.denisnumb.discord_chat_mod.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.denisnumb.discord_chat_mod.config.configs.ClientConfig.loadClientConfig;
import static com.denisnumb.discord_chat_mod.config.configs.CommonConfig.loadCommonConfig;
import static com.denisnumb.discord_chat_mod.config.configs.DiscordChatStyleConfig.loadDiscordChatStyleConfig;
import static com.denisnumb.discord_chat_mod.config.configs.DiscordGuildsConfig.loadDiscordGuildsConfig;
import static com.denisnumb.discord_chat_mod.config.configs.DiscordProxyConfig.loadDiscordProxyConfig;
import static com.denisnumb.discord_chat_mod.config.configs.LogsConfig.loadLogsConfig;
import static com.denisnumb.discord_chat_mod.config.configs.MinecraftChatStyleConfig.loadMinecraftChatStyleConfig;
import static com.denisnumb.discord_chat_mod.config.configs.WebhookModeConfig.loadWebhookModeConfig;

public final class ConfigManager {
    private ConfigManager() {}

    private static final String CONFIG_DIR_NAME = "config";
    private static final String COMMON_PATH = "discord_chat_mod-common.toml";
    private static final String CLIENT_PATH = "discord_chat_mod-client.toml";

    public static void load(boolean loadClient) {
        loadCommon();

        if (loadClient)
            loadClient();
    }

    @Nullable
    private static Path getConfigFile(String fileName){
        Path configDir = Paths.get(CONFIG_DIR_NAME);
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir);
            } catch (IOException e) {
                return null;
            }
        }

        return configDir.resolve(fileName);
    }

    private static void removeDeprecatedParameters(CommentedConfig config){
        config.remove("discordChannelId");
        config.remove("channelOverrides");
        config.remove("enablePinnedStatusMessage");
        config.remove("serverLogsChannelId");
        config.remove("logDiscordMessages");
        config.remove("logDiscordErrorsToServerChat");
        config.remove("discordErrorsChatPlayerSelector");
        config.remove("serverLogsToDiscordLoggingLevel");
        config.remove("serverLogsPattern");
        config.remove("commandLogEnabled");
        config.remove("commandLogMinPermissionLevel");
        config.remove("commandLogIgnoredCommands");
        config.remove("serverLogsCommandsOnly");
        config.remove("slashCommandAllowedRoles");
        config.remove("enableSlashCommands");
    }

    private static void loadCommon() {
        CommentedFileConfig commonConfig = CommentedFileConfig.builder(getConfigFile(COMMON_PATH))
                .autosave()
                .preserveInsertionOrder()
                .sync()
                .build();

        commonConfig.load();

        loadCommonConfig(commonConfig);
        commonConfig.set("guilds", loadDiscordGuildsConfig(commonConfig));
        commonConfig.set("logsConfig", loadLogsConfig(commonConfig));
        commonConfig.set("webhookModeConfig", loadWebhookModeConfig(commonConfig));
        commonConfig.set("discordProxyConfig", loadDiscordProxyConfig(commonConfig));
        commonConfig.set("minecraftChatStyle", loadMinecraftChatStyleConfig(commonConfig));
        commonConfig.set("discordChatStyle", loadDiscordChatStyleConfig(commonConfig));
        removeDeprecatedParameters(commonConfig);

        commonConfig.save();
    }

    private static void loadClient() {
        CommentedFileConfig clientConfig = CommentedFileConfig.builder(getConfigFile(CLIENT_PATH))
                .autosave()
                .preserveInsertionOrder()
                .sync()
                .build();

        clientConfig.load();
        loadClientConfig(clientConfig);
        clientConfig.save();
    }
}
