package com.denisnumb.discord_chat_mod.config.configs;

import com.denisnumb.discord_chat_mod.utils.ColorUtils;
import com.electronwill.nightconfig.core.CommentedConfig;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Objects;

import static com.denisnumb.discord_chat_mod.utils.ColorUtils.parseColor;
import static com.denisnumb.discord_chat_mod.config.ConfigComments.*;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.*;

public final class MinecraftChatStyleConfig {
    private MinecraftChatStyleConfig() {}
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean enableMinecraftChatCustomization;
    public static String minecraftChatLinkColor;
    public static String minecraftDiscordMessagesStyle;
    public static String minecraftPlayerMessageStyle;
    public static String minecraftPlayerJoinedStyle;
    public static String minecraftPlayerLeftStyle;
    public static String minecraftPlayerDeathNameStyle;
    public static String minecraftPlayerDeathCauseStyle;
    public static String minecraftPlayerDeathSecondEntityNameStyle;
    public static String minecraftPlayerDeathWeaponStyle;
    public static String minecraftPlayerAdvancementTaskStyle;
    public static String minecraftPlayerAdvancementGoalStyle;
    public static String minecraftPlayerAdvancementChallengeStyle;
    public static String minecraftTeamMessageSentStyle;
    public static String minecraftTeamMessageReceivedStyle;
    public static String minecraftTellMessageSentStyle;
    public static String minecraftTellMessageReceivedStyle;
    public static String minecraftSayCommandStyle;
    public static String minecraftMeCommandStyle;

    public static CommentedConfig loadMinecraftChatStyleConfig(CommentedConfig commonConfig){
        CommentedConfig existedMinecraftChatStyle = commonConfig.getOrElse("minecraftChatStyle", commonConfig.createSubConfig());
        CommentedConfig minecraftChatStyle = commonConfig.createSubConfig();

        enableMinecraftChatCustomization = existedMinecraftChatStyle.getOrElse("enableMinecraftChatCustomization", ENABLE_MINECRAFT_CHAT_CUSTOMIZATION_DEFAULT);
        minecraftChatStyle.set("enableMinecraftChatCustomization", enableMinecraftChatCustomization);
        minecraftChatStyle.setComment("enableMinecraftChatCustomization", ENABLE_MINECRAFT_CHAT_CUSTOMIZATION_COMMENT);

        minecraftChatLinkColor = existedMinecraftChatStyle.getOrElse("minecraftChatLinkColor", MINECRAFT_CHAT_LINK_COLOR_DEFAULT);
        ColorUtils.Color.CHAT_LINK_COLOR = Objects.requireNonNullElse(parseColor(minecraftChatLinkColor), ColorUtils.Color.CHAT_LINK_COLOR);
        minecraftChatStyle.set("minecraftChatLinkColor", minecraftChatLinkColor);
        minecraftChatStyle.setComment("minecraftChatLinkColor", MINECRAFT_CHAT_LINK_COLOR_COMMENT);

        minecraftDiscordMessagesStyle = existedMinecraftChatStyle.getOrElse("minecraftDiscordMessagesStyle", MINECRAFT_DISCORD_MESSAGES_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftDiscordMessagesStyle", minecraftDiscordMessagesStyle);
        minecraftChatStyle.setComment("minecraftDiscordMessagesStyle", MINECRAFT_DISCORD_MESSAGES_STYLE_COMMENT);

        minecraftPlayerMessageStyle = existedMinecraftChatStyle.getOrElse("minecraftPlayerMessageStyle", MINECRAFT_PLAYER_MESSAGE_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftPlayerMessageStyle", minecraftPlayerMessageStyle);
        minecraftChatStyle.setComment("minecraftPlayerMessageStyle", MINECRAFT_PLAYER_MESSAGE_STYLE_COMMENT);

        minecraftPlayerJoinedStyle = existedMinecraftChatStyle.getOrElse("minecraftPlayerJoinedStyle", MINECRAFT_PLAYER_JOINED_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftPlayerJoinedStyle", minecraftPlayerJoinedStyle);
        minecraftChatStyle.setComment("minecraftPlayerJoinedStyle", MINECRAFT_PLAYER_JOINED_STYLE_COMMENT);

        minecraftPlayerLeftStyle = existedMinecraftChatStyle.getOrElse("minecraftPlayerLeftStyle", MINECRAFT_PLAYER_LEFT_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftPlayerLeftStyle", minecraftPlayerLeftStyle);
        minecraftChatStyle.setComment("minecraftPlayerLeftStyle", MINECRAFT_PLAYER_LEFT_STYLE_COMMENT);

        minecraftPlayerDeathCauseStyle = existedMinecraftChatStyle.getOrElse("minecraftPlayerDeathCauseStyle", MINECRAFT_PLAYER_DEATH_CAUSE_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftPlayerDeathCauseStyle", minecraftPlayerDeathCauseStyle);
        minecraftChatStyle.setComment("minecraftPlayerDeathCauseStyle", MINECRAFT_PLAYER_DEATH_STYLE_COMMENT);

        minecraftPlayerDeathNameStyle = existedMinecraftChatStyle.getOrElse("minecraftPlayerDeathNameStyle", MINECRAFT_PLAYER_DEATH_NAME_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftPlayerDeathNameStyle", minecraftPlayerDeathNameStyle);

        minecraftPlayerDeathSecondEntityNameStyle = existedMinecraftChatStyle.getOrElse("minecraftPlayerDeathSecondEntityNameStyle", MINECRAFT_PLAYER_DEATH_SECOND_ENTITY_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftPlayerDeathSecondEntityNameStyle", minecraftPlayerDeathSecondEntityNameStyle);

        minecraftPlayerDeathWeaponStyle = existedMinecraftChatStyle.getOrElse("minecraftPlayerDeathWeaponStyle", MINECRAFT_PLAYER_DEATH_WEAPON_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftPlayerDeathWeaponStyle", minecraftPlayerDeathWeaponStyle);

        minecraftPlayerAdvancementTaskStyle = existedMinecraftChatStyle.getOrElse("minecraftPlayerAdvancementTaskStyle", MINECRAFT_PLAYER_ADVANCEMENT_TASK_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftPlayerAdvancementTaskStyle", minecraftPlayerAdvancementTaskStyle);
        minecraftChatStyle.setComment("minecraftPlayerAdvancementTaskStyle", MINECRAFT_PLAYER_ADVANCEMENT_TASK_STYLE_COMMENT);

        minecraftPlayerAdvancementGoalStyle = existedMinecraftChatStyle.getOrElse("minecraftPlayerAdvancementGoalStyle", MINECRAFT_PLAYER_ADVANCEMENT_GOAL_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftPlayerAdvancementGoalStyle", minecraftPlayerAdvancementGoalStyle);
        minecraftChatStyle.setComment("minecraftPlayerAdvancementGoalStyle", MINECRAFT_PLAYER_ADVANCEMENT_GOAL_STYLE_COMMENT);

        minecraftPlayerAdvancementChallengeStyle = existedMinecraftChatStyle.getOrElse("minecraftPlayerAdvancementChallengeStyle", MINECRAFT_PLAYER_ADVANCEMENT_CHALLENGE_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftPlayerAdvancementChallengeStyle", minecraftPlayerAdvancementChallengeStyle);
        minecraftChatStyle.setComment("minecraftPlayerAdvancementChallengeStyle", MINECRAFT_PLAYER_ADVANCEMENT_CHALLENGE_STYLE_COMMENT);

        minecraftTeamMessageSentStyle = existedMinecraftChatStyle.getOrElse("minecraftTeamMessageSentStyle", MINECRAFT_TEAM_MESSAGE_SENT_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftTeamMessageSentStyle", minecraftTeamMessageSentStyle);
        minecraftChatStyle.setComment("minecraftTeamMessageSentStyle", MINECRAFT_TEAM_MESSAGE_SENT_STYLE_COMMENT);

        minecraftTeamMessageReceivedStyle = existedMinecraftChatStyle.getOrElse("minecraftTeamMessageReceivedStyle", MINECRAFT_TEAM_MESSAGE_RECEIVED_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftTeamMessageReceivedStyle", minecraftTeamMessageReceivedStyle);
        minecraftChatStyle.setComment("minecraftTeamMessageReceivedStyle", MINECRAFT_TEAM_MESSAGE_RECEIVED_STYLE_COMMENT);

        minecraftTellMessageSentStyle = existedMinecraftChatStyle.getOrElse("minecraftTellMessageSentStyle", MINECRAFT_TELL_MESSAGE_SENT_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftTellMessageSentStyle", minecraftTellMessageSentStyle);
        minecraftChatStyle.setComment("minecraftTellMessageSentStyle", MINECRAFT_TELL_MESSAGE_SENT_STYLE_COMMENT);

        minecraftTellMessageReceivedStyle = existedMinecraftChatStyle.getOrElse("minecraftTellMessageReceivedStyle", MINECRAFT_TELL_MESSAGE_RECEIVED_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftTellMessageReceivedStyle", minecraftTellMessageReceivedStyle);
        minecraftChatStyle.setComment("minecraftTellMessageReceivedStyle", MINECRAFT_TELL_MESSAGE_RECEIVED_STYLE_COMMENT);

        minecraftSayCommandStyle = existedMinecraftChatStyle.getOrElse("minecraftSayCommandStyle", MINECRAFT_SAY_COMMAND_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftSayCommandStyle", minecraftSayCommandStyle);
        minecraftChatStyle.setComment("minecraftSayCommandStyle", MINECRAFT_SAY_COMMAND_STYLE_COMMENT);

        minecraftMeCommandStyle = existedMinecraftChatStyle.getOrElse("minecraftMeCommandStyle", MINECRAFT_ME_COMMAND_STYLE_DEFAULT);
        minecraftChatStyle.set("minecraftMeCommandStyle", minecraftMeCommandStyle);
        minecraftChatStyle.setComment("minecraftMeCommandStyle", MINECRAFT_ME_COMMAND_STYLE_COMMENT);

        updateTellMessageExistedDefaultStyle(minecraftChatStyle);

        return minecraftChatStyle;
    }

    /**
     * Migration method for configs generated with version 2.8.0 or less
     * @since 2.9.0
     */
    private static void updateTellMessageExistedDefaultStyle(CommentedConfig minecraftChatStyle) {
        String oldSentStyle = "<grey>*{commands.message.display.outgoing} {receiver}: {message}*<grey/>";
        String oldReceivedStyle = "<grey>*{sender} {commands.message.display.incoming}: {message}*<grey/>";

        if (minecraftTellMessageSentStyle.equals(oldSentStyle)) {
            minecraftTellMessageSentStyle = MINECRAFT_TELL_MESSAGE_SENT_STYLE_DEFAULT;
            minecraftChatStyle.set("minecraftTellMessageSentStyle", minecraftTellMessageSentStyle);
            LOGGER.info("[minecraftChatStyle] Updating the default style for messages sent via the /tell command.");
        }

        if (minecraftTellMessageReceivedStyle.equals(oldReceivedStyle)) {
            minecraftTellMessageReceivedStyle = MINECRAFT_TELL_MESSAGE_RECEIVED_STYLE_DEFAULT;
            minecraftChatStyle.set("minecraftTellMessageReceivedStyle", minecraftTellMessageReceivedStyle);
            LOGGER.info("[minecraftChatStyle] Updating the default style for messages received via the /tell command.");
        }
    }
}
