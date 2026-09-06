package com.denisnumb.discord_chat_mod.config.configs;

import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.discord.slash_commands.permissions.SlashCommandPermissions;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.*;

import static com.denisnumb.discord_chat_mod.config.ConfigComments.*;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.*;

public final class DiscordGuildsConfig {
    private DiscordGuildsConfig() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    public static List<DiscordGuildConfig> discordGuildConfigs;

    public record SlashCommandPermissionsConfig(
            Mode mode,
            SlashCommandPermissions defaultPermissions,
            Map<String, SlashCommandPermissions> roleMap
    ) {
        public enum Mode {
            MERGE,
            TOP_ROLE;

            public static boolean isValid(String value) {
                return value != null && switch (value.trim().toLowerCase()) {
                    case "merge", "top-role" -> true;
                    default -> false;
                };
            }

            public static Mode fromString(String value) {
                if (value == null) return MERGE;
                return switch (value.trim().toLowerCase()) {
                    case "top-role" -> TOP_ROLE;
                    default -> MERGE;
                };
            }
        }
    }

    public record DiscordGuildConfig(
            String guildId,
            String defaultChannelId,
            String serverLogsChannelId,
            boolean duplicateMessages,
            boolean enablePinnedStatusMessage,
            boolean enableSlashCommands,
            SlashCommandPermissionsConfig slashCommandPermissionsConfig,
            Map<String, String> channelOverrides
    ) { }

    public static List<CommentedConfig> loadDiscordGuildsConfig(CommentedConfig commonConfig){
        List<CommentedConfig> guildList = commonConfig.getOrElse("guilds", getDefaultGuildConfig(commonConfig));
        discordGuildConfigs = new ArrayList<>();

        for (CommentedConfig guildConfig : guildList) {
            String guildId = guildConfig.getOrElse("guildId", GUILD_ID_DEFAULT);
            if (discordGuildConfigs.stream().map(DiscordGuildConfig::guildId).toList().contains(guildId))
                continue;

            prepareGuildConfig(guildConfig, guildId);

            String mainChannelId = guildConfig.get("defaultChannelId");
            String serverLogsChannelId = guildConfig.get("serverLogsChannelId");
            boolean enablePinnedStatusMessage = guildConfig.get("enablePinnedStatusMessage");
            boolean enableSlashCommands = guildConfig.get("enableSlashCommands");

            CommentedConfig slashCommandPermissions = guildConfig.get("slashCommandPermissions");
            SlashCommandPermissionsConfig.Mode slashCommandPermissionsMode
                    = SlashCommandPermissionsConfig.Mode.fromString(slashCommandPermissions.get("mode"));
            SlashCommandPermissions defaultSlashCommandPermissions = new SlashCommandPermissions(
                    slashCommandPermissions.get("allow"),
                    slashCommandPermissions.get("deny")
            );

            Map<String, SlashCommandPermissions> rolePermissionsMap = new LinkedHashMap<>();
            for (CommentedConfig.Entry entry : slashCommandPermissions.entrySet()) {
                if (entry.getValue() instanceof CommentedConfig roleConfig) {
                    String roleName = entry.getKey();
                    List<String> allow = roleConfig.getOrElse("allow", SLASH_COMMAND_PERMISSIONS_DEFAULT_ALLOW_DEFAULT);
                    List<String> deny = roleConfig.getOrElse("deny",  SLASH_COMMAND_PERMISSIONS_DEFAULT_DENY_DEFAULT);
                    rolePermissionsMap.put(roleName, new SlashCommandPermissions(allow, deny));
                }
            }

            SlashCommandPermissionsConfig slashCommandPermissionsConfig = new SlashCommandPermissionsConfig(
                    slashCommandPermissionsMode,
                    defaultSlashCommandPermissions,
                    rolePermissionsMap
            );

            CommentedConfig overrides = guildConfig.get("channelOverrides");
            boolean duplicateMessages = overrides.get("duplicateMessages");
            Map<String, String> channelOverrides = new HashMap<>();

            for (CommentedConfig.Entry entry : overrides.entrySet()) {
                if (entry.getKey().equals("duplicateMessages"))
                    continue;

                String key = entry.getKey();
                String value = entry.getValue();

                boolean valid = false;
                for (ChannelCategory cat : ChannelCategory.values()) {
                    if (cat.getConfigName().equalsIgnoreCase(key)) {
                        valid = true;
                        channelOverrides.put(cat.getConfigName(), value);
                        break;
                    }
                }

                if (!valid)
                    LOGGER.error("Unknown channel key \"{}\" in channel overrides for guildId: {}", key, guildId);
            }

            discordGuildConfigs.add(
                    new DiscordGuildConfig(
                            guildId,
                            mainChannelId,
                            serverLogsChannelId,
                            duplicateMessages,
                            enablePinnedStatusMessage,
                            enableSlashCommands,
                            slashCommandPermissionsConfig,
                            channelOverrides
                    )
            );
        }

        return guildList;
    }

    private static void prepareGuildConfig(CommentedConfig guildConfig, String guildId) {
        createFieldIfNotExists(guildConfig, guildId, "defaultChannelId", DEFAULT_CHANNEL_ID_DEFAULT, DEFAULT_CHANNEL_ID_COMMENT);
        createFieldIfNotExists(guildConfig, guildId, "serverLogsChannelId", SERVER_LOGS_CHANNEL_ID_DEFAULT, SERVER_LOGS_CHANNEL_ID_COMMENT);
        createFieldIfNotExists(guildConfig, guildId, "enablePinnedStatusMessage", ENABLE_PINNED_STATUS_MESSAGE_DEFAULT, ENABLE_PINNED_STATUS_MESSAGE_COMMENT);
        createFieldIfNotExists(guildConfig, guildId, "enableSlashCommands", ENABLE_SLASH_COMMANDS_DEFAULT, ENABLE_SLASH_COMMANDS_COMMENT);

        boolean addExampleRole = false;
        if (!guildConfig.contains("slashCommandPermissions")) {
            LOGGER.info("[guildId: {}] Adding missing section \"slashCommandPermissions\"", guildId);
            guildConfig.set("slashCommandPermissions", guildConfig.createSubConfig());
            guildConfig.setComment("slashCommandPermissions", SLASH_COMMAND_PERMISSIONS_DEFAULT_COMMENT);
            addExampleRole = true;
        }

        CommentedConfig slashCommandPermissions = guildConfig.get("slashCommandPermissions");
        createFieldIfNotExists(slashCommandPermissions, guildId, "mode", SLASH_COMMAND_PERMISSIONS_MODE_DEFAULT, SLASH_COMMAND_PERMISSIONS_MODE_COMMENT);
        if (!SlashCommandPermissionsConfig.Mode.isValid(slashCommandPermissions.get("mode"))){
            LOGGER.warn("[guildId: {}] Invalid value for field \"slashCommandPermissions.mode\". Resetting to default ({})", guildId, SLASH_COMMAND_PERMISSIONS_MODE_DEFAULT);
            slashCommandPermissions.set("mode", SLASH_COMMAND_PERMISSIONS_MODE_DEFAULT);
        }
        createFieldIfNotExists(slashCommandPermissions, guildId, "allow", SLASH_COMMAND_PERMISSIONS_DEFAULT_ALLOW_DEFAULT, SLASH_COMMAND_PERMISSIONS_DEFAULT_ALLOW_COMMENT);
        createFieldIfNotExists(slashCommandPermissions, guildId, "deny", SLASH_COMMAND_PERMISSIONS_DEFAULT_DENY_DEFAULT, SLASH_COMMAND_PERMISSIONS_DEFAULT_DENY_COMMENT);

        if (addExampleRole){
            CommentedConfig exampleRole = slashCommandPermissions.createSubConfig();
            createFieldIfNotExists(exampleRole, guildId, "allow", SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_ALLOW_DEFAULT, SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_ALLOW_COMMENT);
            createFieldIfNotExists(exampleRole, guildId, "deny", SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_DENY_DEFAULT, SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_DENY_COMMENT);
            slashCommandPermissions.set("ExampleRole", exampleRole);
            slashCommandPermissions.setComment("ExampleRole", SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_COMMENT);
        }

        if (!guildConfig.contains("channelOverrides")) {
            LOGGER.info("[guildId: {}] Adding missing section \"channelOverrides\"", guildId);
            guildConfig.set("channelOverrides", guildConfig.createSubConfig());
        }

        CommentedConfig overrides = guildConfig.get("channelOverrides");
        createFieldIfNotExists(overrides, guildId, "duplicateMessages", DUPLICATE_MESSAGES_DEFAULT, DUPLICATE_MESSAGES_COMMENT);
        for (ChannelCategory category : ChannelCategory.values())
            createFieldIfNotExists(overrides, guildId, category.getConfigName(), OVERRIDE_CHANNEL_ID_DEFAULT, category.getConfigComment());

        // Migrate from old config versions
        migrateSlashCommandsConfig(guildConfig, guildId);
        migrateScreenshotChannelId(guildConfig, guildId);
    }

    private static <T> void createFieldIfNotExists(CommentedConfig config, String guildId, String key, T defaultValue, String comment) {
        if (!config.contains(key)) {
            LOGGER.info("[guildId: {}] Adding missing field \"{}\"", guildId, key);
            config.set(key, defaultValue);
            config.setComment(key, comment);
        }
    }

    private static List<CommentedConfig> getDefaultGuildConfig(CommentedConfig commonConfig){
        CommentedConfig exampleServer = commonConfig.createSubConfig();
        exampleServer.set("guildId", GUILD_ID_DEFAULT);
        exampleServer.setComment("guildId", GUILD_ID_COMMENT);
        exampleServer.set("defaultChannelId", DEFAULT_CHANNEL_ID_DEFAULT);
        exampleServer.setComment("defaultChannelId", DEFAULT_CHANNEL_ID_COMMENT);
        exampleServer.set("serverLogsChannelId", SERVER_LOGS_CHANNEL_ID_DEFAULT);
        exampleServer.setComment("serverLogsChannelId", SERVER_LOGS_CHANNEL_ID_COMMENT);
        exampleServer.set("enablePinnedStatusMessage", ENABLE_PINNED_STATUS_MESSAGE_DEFAULT);
        exampleServer.setComment("enablePinnedStatusMessage", ENABLE_PINNED_STATUS_MESSAGE_COMMENT);
        exampleServer.set("enableSlashCommands", ENABLE_SLASH_COMMANDS_DEFAULT);
        exampleServer.setComment("enableSlashCommands",ENABLE_SLASH_COMMANDS_COMMENT);

        CommentedConfig slashCommandPermissions = exampleServer.createSubConfig();
        slashCommandPermissions.set("mode", SLASH_COMMAND_PERMISSIONS_MODE_DEFAULT);
        slashCommandPermissions.setComment("mode", SLASH_COMMAND_PERMISSIONS_MODE_COMMENT);
        slashCommandPermissions.set("allow", SLASH_COMMAND_PERMISSIONS_DEFAULT_ALLOW_DEFAULT);
        slashCommandPermissions.setComment("allow", SLASH_COMMAND_PERMISSIONS_DEFAULT_ALLOW_COMMENT);
        slashCommandPermissions.set("deny", SLASH_COMMAND_PERMISSIONS_DEFAULT_DENY_DEFAULT);
        slashCommandPermissions.setComment("deny", SLASH_COMMAND_PERMISSIONS_DEFAULT_DENY_COMMENT);

        CommentedConfig exampleRole = slashCommandPermissions.createSubConfig();
        exampleRole.set("allow", SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_ALLOW_DEFAULT);
        exampleRole.setComment("allow", SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_ALLOW_COMMENT);
        exampleRole.set("deny", SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_DENY_DEFAULT);
        exampleRole.setComment("deny", SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_DENY_COMMENT);
        slashCommandPermissions.set("ExampleRole", exampleRole);
        slashCommandPermissions.setComment("ExampleRole", SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_COMMENT);

        CommentedConfig overrides = exampleServer.createSubConfig();
        overrides.set("duplicateMessages", DUPLICATE_MESSAGES_DEFAULT);
        overrides.setComment("duplicateMessages", DUPLICATE_MESSAGES_COMMENT);

        for (ChannelCategory category : ChannelCategory.values()){
            overrides.set(category.getConfigName(), OVERRIDE_CHANNEL_ID_DEFAULT);
            overrides.setComment(category.getConfigName(), category.getConfigComment());
        }

        exampleServer.set("slashCommandPermissions", slashCommandPermissions);
        exampleServer.setComment("slashCommandPermissions", SLASH_COMMAND_PERMISSIONS_DEFAULT_COMMENT);

        exampleServer.set("channelOverrides", overrides);

        List<CommentedConfig> guildList = List.of(exampleServer);
        commonConfig.add("guilds", guildList);

        return guildList;
    }

    /**
     * Migration method for configs generated with version 2.7.0 or less
     * @since 2.8.0
     */
    private static void migrateSlashCommandsConfig(CommentedConfig guildConfig, String guildId){
        CommentedConfig slashCommands = guildConfig.getOrElse("slashCommands", CommentedConfig.inMemory());

        if (slashCommands.contains("enableSlashCommands")) {
            boolean oldValue = slashCommands.get("enableSlashCommands");
            LOGGER.info("[guildId: {}] Migrating \"slashCommands.enableSlashCommands\" → \"enableSlashCommands\"", guildId);
            guildConfig.set("enableSlashCommands", oldValue);
            guildConfig.setComment("enableSlashCommands", ENABLE_SLASH_COMMANDS_COMMENT);
        }

        if (slashCommands.contains("slashCommandAllowedRoles")) {
            List<String> oldRoles = slashCommands.getOrElse("slashCommandAllowedRoles", List.of());
            if (!oldRoles.isEmpty()) {
                LOGGER.info("[guildId: {}] Migrating \"slashCommandAllowedRoles\" → \"slashCommandPermissions\"", guildId);
                CommentedConfig slashCommandPermissions = guildConfig.get("slashCommandPermissions");

                for (String role : oldRoles) {
                    CommentedConfig roleConfig = slashCommandPermissions.createSubConfig();
                    roleConfig.set("allow", List.of("*"));
                    slashCommandPermissions.set(role, roleConfig);
                }
            }
        }

        guildConfig.remove("slashCommands");
    }

    /**
     * Migration method for configs generated with version 2.7.0 or less
     * @since 2.8.0
     */
    private static void migrateScreenshotChannelId(CommentedConfig guildConfig, String guildId){
        CommentedConfig channelOverrides = guildConfig.getOrElse("channelOverrides", CommentedConfig.inMemory());

        if (channelOverrides.contains("screenshotsChannelId")){
            String oldValue = channelOverrides.get("screenshotsChannelId");
            channelOverrides.set(ChannelCategory.IMAGES.getConfigName(), oldValue);
            channelOverrides.remove("screenshotsChannelId");
            LOGGER.info("[guildId: {}] Migrating \"channelOverrides.screenshotsChannelId\" → \"channelOverrides.imagesChannelId\"", guildId);
        }
    }
}
