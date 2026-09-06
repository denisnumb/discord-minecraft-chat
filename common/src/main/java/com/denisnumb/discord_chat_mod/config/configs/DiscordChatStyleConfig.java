package com.denisnumb.discord_chat_mod.config.configs;

import com.denisnumb.discord_chat_mod.chat_style.Parameters;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import static com.denisnumb.discord_chat_mod.config.ConfigComments.*;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.*;

public final class DiscordChatStyleConfig {
    private DiscordChatStyleConfig() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public static String discordPlayerMessageStyle;
    public static String discordPlayerMessageWebhookStyle;
    public static String discordPlayerJoinedStyle;
    public static String discordPlayerLeftStyle;
    public static String discordPlayerDeathCauseStyle;
    public static String discordPlayerDeathNameStyle;
    public static String discordPlayerDeathSecondEntityStyle;
    public static String discordPlayerDeathWeaponStyle;
    public static String discordPlayerDeathMessageStyle;
    public static String discordPlayerAdvancementTaskStyle;
    public static String discordPlayerAdvancementGoalStyle;
    public static String discordPlayerAdvancementChallengeStyle;
    public static String discordSayCommandStyle;
    public static String discordMeCommandStyle;
    public static String discordMeCommandWebhookStyle;
    public static String discordTellrawCommandStyle;
    public static String discordCommandLogStyle;
    public static String discordImageMessageStyle;
    public static String discordImageMessageWebhookStyle;
    public static String discordServerStartedMessageStyle;
    public static String discordLocalServerStartedMessageStyle;
    public static String discordServerClosedMessageStyle;
    public static String discordPinnedStatusMessageServerUnavailableStyle;
    public static String discordPinnedStatusMessageServerAvailableStyle;
    public static String discordPinnedStatusMessagePlayerListDelimiter;
    public static String discordPinnedStatusMessagePlayerListNicknameStyle;
    public static String discordPinnedStatusMessageStyle;
    public static String discordGuildForwardedMessageUserNameStyle;

    public static CommentedConfig loadDiscordChatStyleConfig(CommentedConfig commonConfig){
        CommentedConfig existedDiscordChatStyle = commonConfig.getOrElse("discordChatStyle", commonConfig.createSubConfig());
        CommentedConfig discordChatStyle = commonConfig.createSubConfig();

        discordPlayerMessageStyle = existedDiscordChatStyle.getOrElse("discordPlayerMessageStyle", DISCORD_PLAYER_MESSAGE_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerMessageStyle", discordPlayerMessageStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPlayerMessageStyle", DISCORD_PLAYER_MESSAGE_STYLE_COMMENT);
        discordPlayerMessageStyle = validateJsonValue(discordPlayerMessageStyle, DISCORD_PLAYER_MESSAGE_STYLE_DEFAULT);

        discordPlayerMessageWebhookStyle = existedDiscordChatStyle.getOrElse("discordPlayerMessageWebhookStyle", DISCORD_PLAYER_MESSAGE_WEBHOOK_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerMessageWebhookStyle", discordPlayerMessageWebhookStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPlayerMessageWebhookStyle", DISCORD_PLAYER_MESSAGE_WEBHOOK_STYLE_COMMENT);
        discordPlayerMessageWebhookStyle = validateJsonValue(discordPlayerMessageWebhookStyle, DISCORD_PLAYER_MESSAGE_WEBHOOK_STYLE_DEFAULT);

        discordPlayerJoinedStyle = existedDiscordChatStyle.getOrElse("discordPlayerJoinedStyle", DISCORD_PLAYER_JOINED_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerJoinedStyle", discordPlayerJoinedStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPlayerJoinedStyle", DISCORD_PLAYER_JOINED_STYLE_COMMENT);
        discordPlayerJoinedStyle = validateJsonValue(discordPlayerJoinedStyle, DISCORD_PLAYER_JOINED_STYLE_DEFAULT);

        discordPlayerLeftStyle = existedDiscordChatStyle.getOrElse("discordPlayerLeftStyle", DISCORD_PLAYER_LEFT_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerLeftStyle", discordPlayerLeftStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPlayerLeftStyle", DISCORD_PLAYER_LEFT_STYLE_COMMENT);
        discordPlayerLeftStyle = validateJsonValue(discordPlayerLeftStyle, DISCORD_PLAYER_LEFT_STYLE_DEFAULT);

        discordPlayerDeathCauseStyle = existedDiscordChatStyle.getOrElse("discordPlayerDeathCauseStyle", DISCORD_PLAYER_DEATH_CAUSE_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerDeathCauseStyle", discordPlayerDeathCauseStyle);
        discordChatStyle.setComment("discordPlayerDeathCauseStyle", DISCORD_PLAYER_DEATH_CAUSE_STYLE_COMMENT);

        discordPlayerDeathNameStyle = existedDiscordChatStyle.getOrElse("discordPlayerDeathNameStyle", DISCORD_PLAYER_DEATH_NAME_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerDeathNameStyle", discordPlayerDeathNameStyle);

        discordPlayerDeathSecondEntityStyle = existedDiscordChatStyle.getOrElse("discordPlayerDeathSecondEntityStyle", DISCORD_PLAYER_DEATH_SECOND_ENTITY_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerDeathSecondEntityStyle", discordPlayerDeathSecondEntityStyle);

        discordPlayerDeathWeaponStyle = existedDiscordChatStyle.getOrElse("discordPlayerDeathWeaponStyle", DISCORD_PLAYER_DEATH_WEAPON_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerDeathWeaponStyle", discordPlayerDeathWeaponStyle);

        discordPlayerDeathMessageStyle = existedDiscordChatStyle.getOrElse("discordPlayerDeathMessageStyle", DISCORD_PLAYER_DEATH_MESSAGE_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerDeathMessageStyle", discordPlayerDeathMessageStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPlayerDeathMessageStyle", DISCORD_PLAYER_DEATH_MESSAGE_STYLE_COMMENT);
        discordPlayerDeathMessageStyle = validateJsonValue(discordPlayerDeathMessageStyle, DISCORD_PLAYER_DEATH_MESSAGE_STYLE_DEFAULT);

        discordPlayerAdvancementTaskStyle = existedDiscordChatStyle.getOrElse("discordPlayerAdvancementTaskStyle", DISCORD_PLAYER_ADVANCEMENT_TASK_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerAdvancementTaskStyle", discordPlayerAdvancementTaskStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPlayerAdvancementTaskStyle", DISCORD_PLAYER_ADVANCEMENT_TASK_STYLE_COMMENT);
        discordPlayerAdvancementTaskStyle = validateJsonValue(discordPlayerAdvancementTaskStyle, DISCORD_PLAYER_ADVANCEMENT_TASK_STYLE_DEFAULT);

        discordPlayerAdvancementGoalStyle = existedDiscordChatStyle.getOrElse("discordPlayerAdvancementGoalStyle", DISCORD_PLAYER_ADVANCEMENT_GOAL_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerAdvancementGoalStyle", discordPlayerAdvancementGoalStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPlayerAdvancementGoalStyle", DISCORD_PLAYER_ADVANCEMENT_GOAL_STYLE_COMMENT);
        discordPlayerAdvancementGoalStyle = validateJsonValue(discordPlayerAdvancementGoalStyle, DISCORD_PLAYER_ADVANCEMENT_GOAL_STYLE_DEFAULT);

        discordPlayerAdvancementChallengeStyle = existedDiscordChatStyle.getOrElse("discordPlayerAdvancementChallengeStyle", DISCORD_PLAYER_ADVANCEMENT_CHALLENGE_STYLE_DEFAULT);
        discordChatStyle.set("discordPlayerAdvancementChallengeStyle", discordPlayerAdvancementChallengeStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPlayerAdvancementChallengeStyle", DISCORD_PLAYER_ADVANCEMENT_CHALLENGE_STYLE_COMMENT);
        discordPlayerAdvancementChallengeStyle = validateJsonValue(discordPlayerAdvancementChallengeStyle, DISCORD_PLAYER_ADVANCEMENT_CHALLENGE_STYLE_DEFAULT);

        discordSayCommandStyle = existedDiscordChatStyle.getOrElse("discordSayCommandStyle", DISCORD_SAY_COMMAND_STYLE_DEFAULT);
        discordChatStyle.set("discordSayCommandStyle", discordSayCommandStyle.replace("\r", ""));
        discordChatStyle.setComment("discordSayCommandStyle", DISCORD_SAY_COMMAND_STYLE_COMMENT);
        discordSayCommandStyle = validateJsonValue(discordSayCommandStyle, DISCORD_SAY_COMMAND_STYLE_DEFAULT);

        discordMeCommandStyle = existedDiscordChatStyle.getOrElse("discordMeCommandStyle", DISCORD_ME_COMMAND_STYLE_DEFAULT);
        discordChatStyle.set("discordMeCommandStyle", discordMeCommandStyle.replace("\r", ""));
        discordChatStyle.setComment("discordMeCommandStyle", DISCORD_ME_COMMAND_STYLE_COMMENT);
        discordMeCommandStyle = validateJsonValue(discordMeCommandStyle, DISCORD_ME_COMMAND_STYLE_DEFAULT);

        discordMeCommandWebhookStyle = existedDiscordChatStyle.getOrElse("discordMeCommandWebhookStyle", DISCORD_ME_COMMAND_WEBHOOK_STYLE_DEFAULT);
        discordChatStyle.set("discordMeCommandWebhookStyle", discordMeCommandWebhookStyle.replace("\r", ""));
        discordChatStyle.setComment("discordMeCommandWebhookStyle", DISCORD_ME_COMMAND_WEBHOOK_STYLE_COMMENT);
        discordMeCommandWebhookStyle = validateJsonValue(discordMeCommandWebhookStyle, DISCORD_ME_COMMAND_WEBHOOK_STYLE_DEFAULT);

        discordTellrawCommandStyle = existedDiscordChatStyle.getOrElse("discordTellrawCommandStyle", DISCORD_TELLRAW_COMMAND_STYLE_DEFAULT);
        discordChatStyle.set("discordTellrawCommandStyle", discordTellrawCommandStyle.replace("\r", ""));
        discordChatStyle.setComment("discordTellrawCommandStyle", DISCORD_TELLRAW_COMMAND_STYLE_COMMENT);
        discordTellrawCommandStyle = validateJsonValue(discordTellrawCommandStyle, DISCORD_TELLRAW_COMMAND_STYLE_DEFAULT);

        discordCommandLogStyle = existedDiscordChatStyle.getOrElse("discordCommandLogStyle", DISCORD_COMMAND_LOG_STYLE_DEFAULT);
        discordChatStyle.set("discordCommandLogStyle", discordCommandLogStyle.replace("\r", ""));
        discordChatStyle.setComment("discordCommandLogStyle", DISCORD_COMMAND_LOG_STYLE_COMMENT);
        discordCommandLogStyle = validateJsonValue(discordCommandLogStyle, DISCORD_COMMAND_LOG_STYLE_DEFAULT);

        discordImageMessageStyle = existedDiscordChatStyle.getOrElse("discordImageMessageStyle", DISCORD_IMAGE_MESSAGE_STYLE_DEFAULT);
        discordChatStyle.set("discordImageMessageStyle", discordImageMessageStyle.replace("\r", ""));
        discordChatStyle.setComment("discordImageMessageStyle", DISCORD_IMAGE_MESSAGE_STYLE_COMMENT);
        discordImageMessageStyle = validateJsonValue(discordImageMessageStyle, DISCORD_IMAGE_MESSAGE_STYLE_DEFAULT);

        discordImageMessageWebhookStyle = existedDiscordChatStyle.getOrElse("discordImageMessageWebhookStyle", DISCORD_IMAGE_MESSAGE_WEBHOOK_STYLE_DEFAULT);
        discordChatStyle.set("discordImageMessageWebhookStyle", discordImageMessageWebhookStyle.replace("\r", ""));
        discordChatStyle.setComment("discordImageMessageWebhookStyle", DISCORD_IMAGE_MESSAGE_WEBHOOK_STYLE_COMMENT);
        discordImageMessageWebhookStyle = validateJsonValue(discordImageMessageWebhookStyle, DISCORD_IMAGE_MESSAGE_WEBHOOK_STYLE_DEFAULT);

        discordServerStartedMessageStyle = existedDiscordChatStyle.getOrElse("discordServerStartedMessageStyle", DISCORD_SERVER_STARTED_MESSAGE_STYLE_DEFAULT);
        discordChatStyle.set("discordServerStartedMessageStyle", discordServerStartedMessageStyle.replace("\r", ""));
        discordChatStyle.setComment("discordServerStartedMessageStyle", DISCORD_SERVER_STARTED_MESSAGE_STYLE_COMMENT);
        discordServerStartedMessageStyle = validateJsonValue(discordServerStartedMessageStyle, DISCORD_SERVER_STARTED_MESSAGE_STYLE_DEFAULT);

        discordLocalServerStartedMessageStyle = existedDiscordChatStyle.getOrElse("discordLocalServerStartedMessageStyle", DISCORD_LOCAL_SERVER_STARTED_MESSAGE_STYLE_DEFAULT);
        discordChatStyle.set("discordLocalServerStartedMessageStyle", discordLocalServerStartedMessageStyle.replace("\r", ""));
        discordChatStyle.setComment("discordLocalServerStartedMessageStyle", DISCORD_LOCAL_SERVER_STARTED_MESSAGE_STYLE_COMMENT);
        discordLocalServerStartedMessageStyle = validateJsonValue(discordLocalServerStartedMessageStyle, DISCORD_LOCAL_SERVER_STARTED_MESSAGE_STYLE_DEFAULT);

        discordServerClosedMessageStyle = existedDiscordChatStyle.getOrElse("discordServerClosedMessageStyle", DISCORD_SERVER_CLOSED_MESSAGE_STYLE_DEFAULT);
        discordChatStyle.set("discordServerClosedMessageStyle", discordServerClosedMessageStyle.replace("\r", ""));
        discordChatStyle.setComment("discordServerClosedMessageStyle", DISCORD_SERVER_CLOSED_MESSAGE_STYLE_COMMENT);
        discordServerClosedMessageStyle = validateJsonValue(discordServerClosedMessageStyle, DISCORD_SERVER_CLOSED_MESSAGE_STYLE_DEFAULT);

        discordPinnedStatusMessageServerUnavailableStyle = existedDiscordChatStyle.getOrElse("discordPinnedStatusMessageServerUnavailableStyle", DISCORD_PINNED_STATUS_MESSAGE_SERVER_UNAVAILABLE_STYLE_DEFAULT);
        discordChatStyle.set("discordPinnedStatusMessageServerUnavailableStyle", discordPinnedStatusMessageServerUnavailableStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPinnedStatusMessageServerUnavailableStyle", DISCORD_PINNED_STATUS_MESSAGE_SERVER_UNAVAILABLE_STYLE_COMMENT);
        discordPinnedStatusMessageServerUnavailableStyle = validateJsonValue(discordPinnedStatusMessageServerUnavailableStyle, DISCORD_PINNED_STATUS_MESSAGE_SERVER_UNAVAILABLE_STYLE_DEFAULT);

        discordPinnedStatusMessageServerAvailableStyle = existedDiscordChatStyle.getOrElse("discordPinnedStatusMessageServerAvailableStyle", DISCORD_PINNED_STATUS_MESSAGE_SERVER_AVAILABLE_STYLE_DEFAULT);
        discordChatStyle.set("discordPinnedStatusMessageServerAvailableStyle", discordPinnedStatusMessageServerAvailableStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPinnedStatusMessageServerAvailableStyle", DISCORD_PINNED_STATUS_MESSAGE_SERVER_AVAILABLE_STYLE_COMMENT);
        discordPinnedStatusMessageServerAvailableStyle = validateJsonValue(discordPinnedStatusMessageServerAvailableStyle, DISCORD_PINNED_STATUS_MESSAGE_SERVER_AVAILABLE_STYLE_DEFAULT);

        discordPinnedStatusMessagePlayerListDelimiter = existedDiscordChatStyle.getOrElse("discordPinnedStatusMessagePlayerListDelimiter", DISCORD_PINNED_STATUS_MESSAGE_PLAYER_LIST_DELIMITER_DEFAULT);
        discordChatStyle.set("discordPinnedStatusMessagePlayerListDelimiter", discordPinnedStatusMessagePlayerListDelimiter);
        discordChatStyle.setComment("discordPinnedStatusMessagePlayerListDelimiter", DISCORD_PINNED_STATUS_MESSAGE_PLAYER_LIST_DELIMITER_COMMENT);

        discordPinnedStatusMessagePlayerListNicknameStyle = existedDiscordChatStyle.getOrElse("discordPinnedStatusMessagePlayerListNicknameStyle", DISCORD_PINNED_STATUS_MESSAGE_PLAYER_LIST_NICKNAME_STYLE_DEFAULT);
        discordChatStyle.set("discordPinnedStatusMessagePlayerListNicknameStyle", discordPinnedStatusMessagePlayerListNicknameStyle);
        discordChatStyle.setComment("discordPinnedStatusMessagePlayerListNicknameStyle", DISCORD_PINNED_STATUS_MESSAGE_PLAYER_LIST_NICKNAME_STYLE_COMMENT);

        discordPinnedStatusMessageStyle = existedDiscordChatStyle.getOrElse("discordPinnedStatusMessageStyle", DISCORD_PINNED_STATUS_MESSAGE_STYLE_DEFAULT);
        discordChatStyle.set("discordPinnedStatusMessageStyle", discordPinnedStatusMessageStyle.replace("\r", ""));
        discordChatStyle.setComment("discordPinnedStatusMessageStyle", DISCORD_PINNED_STATUS_MESSAGE_STYLE_COMMENT);
        discordPinnedStatusMessageStyle = validateJsonValue(discordPinnedStatusMessageStyle, DISCORD_PINNED_STATUS_MESSAGE_STYLE_DEFAULT);

        discordGuildForwardedMessageUserNameStyle = existedDiscordChatStyle.getOrElse("discordGuildForwardedMessageUserNameStyle", DISCORD_GUILD_FORWARDED_MESSAGE_USERNAME_STYLE_DEFAULT);
        discordChatStyle.set("discordGuildForwardedMessageUserNameStyle", discordGuildForwardedMessageUserNameStyle);
        discordChatStyle.setComment("discordGuildForwardedMessageUserNameStyle", DISCORD_GUILD_FORWARDED_MESSAGE_USERNAME_STYLE_COMMENT);

        migrateScreenshotMessageStyle(existedDiscordChatStyle, discordChatStyle);

        return discordChatStyle;
    }

    private static String validateJsonValue(String jsonValue, String defaultValue) {
        if (jsonValue.isBlank())
            return defaultValue;

        try{
            JsonObject jsonObject = GSON.fromJson(jsonValue, JsonObject.class);

            if (!jsonObject.has("content") && !jsonObject.has("embed"))
                throw new JsonSyntaxException("Json should contains \"content\" or \"embed\" keys");

            return jsonValue.replace("\r", "");
        } catch (JsonSyntaxException e){
            LOGGER.warn("Error on parsing discord message style json: {}", e.getMessage());
            LOGGER.warn(jsonValue.replace("\r", ""));
            LOGGER.warn("Default style will be used");
            return defaultValue;
        }
    }

    /**
     * Migration method for configs generated with version 2.7.0 or less
     * @since 2.8.0
     */
    private static void migrateScreenshotMessageStyle(CommentedConfig existedDiscordChatStyle, CommentedConfig newDiscordChatStyle){
        if (existedDiscordChatStyle.contains("discordScreenshotMessageStyle")){
            String existedOldStyle = existedDiscordChatStyle.getOrElse("discordScreenshotMessageStyle", DISCORD_IMAGE_MESSAGE_STYLE_DEFAULT)
                    .replace("{screenshot_url}", Parameters.IMAGE_URL);
            newDiscordChatStyle.set("discordImageMessageStyle", existedOldStyle.replace("\r", ""));
            discordImageMessageStyle = validateJsonValue(existedOldStyle, DISCORD_IMAGE_MESSAGE_STYLE_DEFAULT);
            LOGGER.info("[discordChatStyle] Migrating \"discordScreenshotMessageStyle\" → \"discordImageMessageStyle\"");
        }

        if (existedDiscordChatStyle.contains("discordScreenshotMessageWebhookStyle")){
            String existedOldStyle = existedDiscordChatStyle.getOrElse("discordScreenshotMessageWebhookStyle", DISCORD_IMAGE_MESSAGE_WEBHOOK_STYLE_DEFAULT)
                    .replace("{screenshot_url}", Parameters.IMAGE_URL);
            newDiscordChatStyle.set("discordImageMessageWebhookStyle", existedOldStyle.replace("\r", ""));
            discordImageMessageWebhookStyle = validateJsonValue(existedOldStyle, DISCORD_IMAGE_MESSAGE_WEBHOOK_STYLE_DEFAULT);
            LOGGER.info("[discordChatStyle] Migrating \"discordScreenshotMessageWebhookStyle\" → \"discordImageMessageWebhookStyle\"");
        }
    }
}
