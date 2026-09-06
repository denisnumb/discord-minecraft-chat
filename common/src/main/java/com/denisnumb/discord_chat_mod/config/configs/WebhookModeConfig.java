package com.denisnumb.discord_chat_mod.config.configs;

import com.electronwill.nightconfig.core.CommentedConfig;

import static com.denisnumb.discord_chat_mod.config.ConfigComments.*;
import static com.denisnumb.discord_chat_mod.config.ConfigComments.ENABLE_SET_AVATAR_URL_COMMAND_COMMENT;
import static com.denisnumb.discord_chat_mod.config.ConfigComments.WEBHOOK_PLAYER_AVATAR_URL_COMMENT;
import static com.denisnumb.discord_chat_mod.config.ConfigComments.WEBHOOK_PLAYER_DEFAULT_AVATAR_URL_COMMENT;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.*;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.WEBHOOK_PLAYER_DEFAULT_AVATAR_URL_DEFAULT;

public final class WebhookModeConfig {
    private WebhookModeConfig() {}

    public static boolean enableWebhookMode;
    public static String webhookServerName;
    public static String webhookServerAvatarUrl;
    public static boolean enableSetAvatarUrlCommand;
    public static String webhookPlayerAvatarUrl;
    public static String webhookPlayerDefaultAvatarUrl;

    public static CommentedConfig loadWebhookModeConfig(CommentedConfig commonConfig){
        CommentedConfig existedWebhookModeConfig = commonConfig.getOrElse("webhookModeConfig", commonConfig.createSubConfig());
        CommentedConfig webhookModeConfig = commonConfig.createSubConfig();

        enableWebhookMode = existedWebhookModeConfig.getOrElse("enableWebhookMode", ENABLE_WEBHOOK_MODE_DEFAULT);
        webhookModeConfig.set("enableWebhookMode", enableWebhookMode);
        webhookModeConfig.setComment("enableWebhookMode", ENABLE_WEBHOOK_MODE_COMMENT);

        webhookServerName = existedWebhookModeConfig.getOrElse("webhookServerName", WEBHOOK_SERVER_NAME_DEFAULT);
        webhookModeConfig.set("webhookServerName", webhookServerName);
        webhookModeConfig.setComment("webhookServerName", WEBHOOK_SERVER_NAME_COMMENT);

        webhookServerAvatarUrl = existedWebhookModeConfig.getOrElse("webhookServerAvatarUrl", WEBHOOK_SERVER_AVATAR_URL_DEFAULT);
        webhookModeConfig.set("webhookServerAvatarUrl", webhookServerAvatarUrl);
        webhookModeConfig.setComment("webhookServerAvatarUrl", WEBHOOK_SERVER_AVATAR_URL_COMMENT);

        enableSetAvatarUrlCommand = existedWebhookModeConfig.getOrElse("enableSetAvatarUrlCommand", ENABLE_SET_AVATAR_URL_COMMAND_DEFAULT);
        webhookModeConfig.set("enableSetAvatarUrlCommand", enableSetAvatarUrlCommand);
        webhookModeConfig.setComment("enableSetAvatarUrlCommand", ENABLE_SET_AVATAR_URL_COMMAND_COMMENT);

        webhookPlayerAvatarUrl = existedWebhookModeConfig.getOrElse("webhookPlayerAvatarUrl", WEBHOOK_PLAYER_AVATAR_URL_DEFAULT);
        webhookModeConfig.set("webhookPlayerAvatarUrl", webhookPlayerAvatarUrl);
        webhookModeConfig.setComment("webhookPlayerAvatarUrl", WEBHOOK_PLAYER_AVATAR_URL_COMMENT);

        webhookPlayerDefaultAvatarUrl = existedWebhookModeConfig.getOrElse("webhookPlayerDefaultAvatarUrl", WEBHOOK_PLAYER_DEFAULT_AVATAR_URL_DEFAULT);
        webhookModeConfig.set("webhookPlayerDefaultAvatarUrl", webhookPlayerDefaultAvatarUrl);
        webhookModeConfig.setComment("webhookPlayerDefaultAvatarUrl", WEBHOOK_PLAYER_DEFAULT_AVATAR_URL_COMMENT);

        return webhookModeConfig;
    }
}
