package com.denisnumb.discord_chat_mod.config;

import java.util.List;

import static com.denisnumb.discord_chat_mod.chat_style.Parameters.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.Translatable.*;

public final class ConfigDefaults {
    private ConfigDefaults() {}

    public static final String DISCORD_BOT_TOKEN_DEFAULT = "";
    public static final String MOD_LOCALE_DEFAULT = "en_us";
    public static final int UTC_OFFSET_HOURS_DEFAULT = 0;
    public static final boolean ENABLE_BOT_PRESENCE_STATUS_DEFAULT = true;
    public static final boolean MENTION_BOTS_DEFAULT = true;

    // =================================================================================================================
    //                                              LOGS CONFIG DEFAULTS
    // =================================================================================================================

    public static final boolean LOG_DISCORD_MESSAGES_DEFAULT = true;
    public static final boolean LOG_DISCORD_ERRORS_TO_SERVER_CHAT_DEFAULT = true;
    public static final String DISCORD_ERRORS_CHAT_PLAYER_SELECTOR_DEFAULT = "@a";
    public static final boolean SERVER_LOGS_TO_DISCORD_ENABLED_DEFAULT = false;
    public static final String SERVER_LOGS_TO_DISCORD_LOGGING_LEVEL_DEFAULT = "INFO";
    public static final String SERVER_LOGS_PATTERN_DEFAULT = "[%d{HH:mm:ss}] [%t/%level] (%logger{1}) %msg%n";
    public static final boolean COMMAND_LOG_ENABLED_DEFAULT = false;
    public static final int COMMAND_LOG_MIN_PERMISSION_LEVEL_DEFAULT = 2;
    public static final List<String> COMMAND_LOG_IGNORED_COMMANDS_DEFAULT = List.of();

    // =================================================================================================================
    //                                              WEBHOOK MODE DEFAULTS
    // =================================================================================================================

    public static final boolean ENABLE_WEBHOOK_MODE_DEFAULT = false;
    public static final String WEBHOOK_SERVER_NAME_DEFAULT = "Minecraft Server";
    public static final String WEBHOOK_SERVER_AVATAR_URL_DEFAULT = "";
    public static final boolean ENABLE_SET_AVATAR_URL_COMMAND_DEFAULT = true;
    public static final String WEBHOOK_PLAYER_AVATAR_URL_DEFAULT = "https://mc-heads.net/avatar/<name>.png";
    public static final String WEBHOOK_PLAYER_DEFAULT_AVATAR_URL_DEFAULT = "https://mc-heads.net/avatar/steve_head_png";

    // =================================================================================================================
    //                                              DISCORD PROXY DEFAULTS
    // =================================================================================================================

    public static final String PROXY_HOSTNAME_DEFAULT = "";
    public static final int PROXY_PORT_DEFAULT = 1234;
    public static final String  PROXY_USER_DEFAULT = "";
    public static final String  PROXY_PASSWORD_DEFAULT = "";

    // =================================================================================================================
    //                                          DISCORD GUILD CONFIG DEFAULTS
    // =================================================================================================================

    public static final String GUILD_ID_DEFAULT = "";
    public static final String DEFAULT_CHANNEL_ID_DEFAULT = "";
    public static final String SERVER_LOGS_CHANNEL_ID_DEFAULT = "";
    public static final boolean DUPLICATE_MESSAGES_DEFAULT = false;
    public static final String OVERRIDE_CHANNEL_ID_DEFAULT = "";
    public static final boolean ENABLE_PINNED_STATUS_MESSAGE_DEFAULT = true;
    public static final boolean ENABLE_SLASH_COMMANDS_DEFAULT = true;

    // =================================================================================================================
    //                                        SLASH COMMAND PERMISSIONS DEFAULTS
    // =================================================================================================================

    public static final String SLASH_COMMAND_PERMISSIONS_MODE_DEFAULT = "merge";
    public static final List<String> SLASH_COMMAND_PERMISSIONS_DEFAULT_ALLOW_DEFAULT = List.of();
    public static final List<String> SLASH_COMMAND_PERMISSIONS_DEFAULT_DENY_DEFAULT = List.of();
    public static final List<String> SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_ALLOW_DEFAULT = List.of();
    public static final List<String> SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_DENY_DEFAULT = List.of();

    // =================================================================================================================
    //                                      MINECRAFT CHAT CUSTOMIZATION DEFAULTS
    // =================================================================================================================

    public static final boolean ENABLE_MINECRAFT_CHAT_CUSTOMIZATION_DEFAULT = false;
    public static final String MINECRAFT_CHAT_LINK_COLOR_DEFAULT = "#00b7ff";

    public static final String MINECRAFT_DISCORD_MESSAGES_STYLE_DEFAULT = String.format(
            "**<#5f78d9>[discord]<#5f78d9/>** <%s> %s",
            MEMBER,
            MESSAGE
    );

    public static final String MINECRAFT_PLAYER_MESSAGE_STYLE_DEFAULT = String.format(
            "<%s> %s",
            PLAYER,
            MESSAGE
    );

    public static final String MINECRAFT_PLAYER_JOINED_STYLE_DEFAULT = String.format(
            "<yellow>%s %s<yellow/>",
            PLAYER,
            PLAYER_JOINED
    );

    public static final String MINECRAFT_PLAYER_LEFT_STYLE_DEFAULT = String.format(
            "<yellow>%s %s<yellow/>",
            PLAYER,
            PLAYER_LEFT
    );

    public static final String MINECRAFT_PLAYER_DEATH_CAUSE_STYLE_DEFAULT = DEATH_CAUSE;
    public static final String MINECRAFT_PLAYER_DEATH_NAME_STYLE_DEFAULT = PLAYER;
    public static final String MINECRAFT_PLAYER_DEATH_SECOND_ENTITY_STYLE_DEFAULT = SECOND_ENTITY;
    public static final String MINECRAFT_PLAYER_DEATH_WEAPON_STYLE_DEFAULT = ITEM;

    public static final String MINECRAFT_PLAYER_ADVANCEMENT_TASK_STYLE_DEFAULT = String.format(
            "%s %s <green>%s<green/>",
            PLAYER,
            ADVANCEMENT_TASK,
            ADVANCEMENT
    );

    public static final String MINECRAFT_PLAYER_ADVANCEMENT_GOAL_STYLE_DEFAULT = String.format(
            "%s %s <green>%s<green/>",
            PLAYER,
            ADVANCEMENT_GOAL,
            ADVANCEMENT
    );

    public static final String MINECRAFT_PLAYER_ADVANCEMENT_CHALLENGE_STYLE_DEFAULT = String.format(
            "%s %s <darkpurple>%s<darkpurple/>",
            PLAYER,
            ADVANCEMENT_CHALLENGE,
            ADVANCEMENT
    );

    public static final String MINECRAFT_TEAM_MESSAGE_SENT_STYLE_DEFAULT = String.format(
            "-> %s <%s> %s",
            TEAM,
            PLAYER,
            MESSAGE
    );

    public static final String MINECRAFT_TEAM_MESSAGE_RECEIVED_STYLE_DEFAULT = String.format(
            "%s <%s> %s",
            TEAM,
            PLAYER,
            MESSAGE
    );

    public static final String MINECRAFT_TELL_MESSAGE_SENT_STYLE_DEFAULT = String.format(
            "<grey>*%s %s %s*<grey/>",
            COMMANDS_MESSAGE_DISPLAY_OUTGOING,
            RECEIVER,
            MESSAGE
    );

    public static final String MINECRAFT_TELL_MESSAGE_RECEIVED_STYLE_DEFAULT = String.format(
            "<grey>*%s %s %s*<grey/>",
            SENDER,
            COMMANDS_MESSAGE_DISPLAY_INCOMING,
            MESSAGE
    );

    public static final String MINECRAFT_SAY_COMMAND_STYLE_DEFAULT = String.format(
            "[%s] %s",
            PLAYER,
            MESSAGE
    );

    public static final String MINECRAFT_ME_COMMAND_STYLE_DEFAULT = String.format(
            "* %s %s",
            PLAYER,
            MESSAGE
    );

    // =================================================================================================================
    //                                      DISCORD CHAT CUSTOMIZATION DEFAULTS
    // =================================================================================================================

    public static final String DISCORD_PLAYER_MESSAGE_STYLE_DEFAULT = String.format(
            """
            {
                "content": "`<%s>` %s"
            }
            """,
            PLAYER,
            MESSAGE
    );

    public static final String DISCORD_PLAYER_MESSAGE_WEBHOOK_STYLE_DEFAULT = String.format(
            """
            {
                "content": "%s"
            }
            """,
            MESSAGE
    );

    public static final String DISCORD_PLAYER_JOINED_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "author": {
                        "name": "%s %s",
                        "icon_url": "%s"
                    },
                    "color": "#2ECC71"
                }
            }
            """,
            PLAYER,
            PLAYER_JOINED,
            PLAYER_AVATAR_URL
    );

    public static final String DISCORD_PLAYER_LEFT_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "author": {
                        "name": "%s %s",
                        "icon_url": "%s"
                    },
                    "color": "#E74C3C"
                }
            }
            """,
            PLAYER,
            PLAYER_LEFT,
            PLAYER_AVATAR_URL
    );

    public static final String DISCORD_PLAYER_DEATH_CAUSE_STYLE_DEFAULT = DEATH_CAUSE;
    public static final String DISCORD_PLAYER_DEATH_NAME_STYLE_DEFAULT = PLAYER;
    public static final String DISCORD_PLAYER_DEATH_SECOND_ENTITY_STYLE_DEFAULT = SECOND_ENTITY;
    public static final String DISCORD_PLAYER_DEATH_WEAPON_STYLE_DEFAULT = ITEM;
    public static final String DISCORD_PLAYER_DEATH_MESSAGE_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "author": {
                        "name": "%s",
                        "icon_url": "%s"
                    }
                }
            }
            """,
            DEATH_MESSAGE,
            PLAYER_AVATAR_URL
    );

    public static final String DISCORD_PLAYER_ADVANCEMENT_TASK_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "title": "**%s**",
                    "description": "%s",
                    "color": "#F1C40F",
                    "author": {
                        "name": "%s %s",
                        "icon_url": "%s"
                    },
                    "thumbnail": {
                        "url": "%s"
                    }
                }
            }
            """,
            ADVANCEMENT,
            DESCRIPTION,
            PLAYER,
            ADVANCEMENT_TASK,
            PLAYER_AVATAR_URL,
            ICON_URL
    );

    public static final String DISCORD_PLAYER_ADVANCEMENT_GOAL_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "title": "**%s**",
                    "description": "%s",
                    "color": "#F1C40F",
                    "author": {
                        "name": "%s %s",
                        "icon_url": "%s"
                    },
                    "thumbnail": {
                        "url": "%s"
                    }
                }
            }
            """,
            ADVANCEMENT,
            DESCRIPTION,
            PLAYER,
            ADVANCEMENT_GOAL,
            PLAYER_AVATAR_URL,
            ICON_URL
    );

    public static final String DISCORD_PLAYER_ADVANCEMENT_CHALLENGE_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "title": "**%s**",
                    "description": "%s",
                    "color": "#A700A7",
                    "author": {
                        "name": "%s %s",
                        "icon_url": "%s"
                    },
                    "thumbnail": {
                        "url": "%s"
                    }
                }
            }
            """,
            ADVANCEMENT,
            DESCRIPTION,
            PLAYER,
            ADVANCEMENT_CHALLENGE,
            PLAYER_AVATAR_URL,
            ICON_URL
    );

    public static final String DISCORD_SAY_COMMAND_STYLE_DEFAULT = String.format(
            """
            {
                "content": "`[%s]` %s"
            }
            """,
            PLAYER,
            MESSAGE
    );

    public static final String DISCORD_ME_COMMAND_STYLE_DEFAULT = String.format(
            """
            {
                "content": "* %s %s"
            }
            """,
            PLAYER,
            MESSAGE
    );

    public static final String DISCORD_ME_COMMAND_WEBHOOK_STYLE_DEFAULT = String.format(
            """
            {
                "content": "> %s"
            }
            """,
            MESSAGE
    );

    public static final String DISCORD_TELLRAW_COMMAND_STYLE_DEFAULT = String.format(
            """
            {
                "content": "%s"
            }
            """,
            MESSAGE
    );

    public static final String DISCORD_COMMAND_LOG_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "description": "`%s`",
                    "color": "#F1C40F",
                    "author": {
                        "name": "%s",
                        "icon_url": "%s"
                    },
                    "timestamp": "%s"
                }
            }
            """,
            COMMAND,
            PLAYER,
            PLAYER_AVATAR_URL,
            DATETIME
    );

    public static final String DISCORD_IMAGE_MESSAGE_STYLE_DEFAULT = String.format(
            """
            {
                "content": "`<%s>`"
            }
            """,
            PLAYER
    );

    public static final String DISCORD_IMAGE_MESSAGE_WEBHOOK_STYLE_DEFAULT =
            """
            {
                "content": ""
            }
            """;

    public static final String DISCORD_SERVER_STARTED_MESSAGE_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "description": "%s <t:%s:R>",
                    "color": "#2ECC71"
                }
            }
            """,
            SERVER_STARTED,
            TIMESTAMP
    );

    public static final String DISCORD_LOCAL_SERVER_STARTED_MESSAGE_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "description": "%s <t:%s:R>",
                    "color": "#2ECC71"
                }
            }
            """,
            LOCAL_SERVER_STARTED,
            TIMESTAMP
    );

    public static final String DISCORD_SERVER_CLOSED_MESSAGE_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "description": "%s <t:%s:R>",
                    "color": "#E74C3C"
                }
            }
            """,
            SERVER_CLOSED,
            TIMESTAMP
    );

    public static final String DISCORD_PINNED_STATUS_MESSAGE_SERVER_UNAVAILABLE_STYLE_DEFAULT = String.format(
            """
           {
               "embed": {
                   "description": "%s",
                   "color": "#E74C3C"
               }
           }
           """,
            SERVER_UNAVAILABLE
    );

    public static final String DISCORD_PINNED_STATUS_MESSAGE_SERVER_AVAILABLE_STYLE_DEFAULT = String.format(
            """
           {
               "embed": {
                   "description": "%s",
                   "color": "#1F8B4C"
               }
           }
           """,
            SERVER_AVAILABLE
    );

    public static final String DISCORD_PINNED_STATUS_MESSAGE_PLAYER_LIST_DELIMITER_DEFAULT = "\n";

    public static final String DISCORD_PINNED_STATUS_MESSAGE_PLAYER_LIST_NICKNAME_STYLE_DEFAULT = PLAYER;

    public static final String DISCORD_PINNED_STATUS_MESSAGE_STYLE_DEFAULT = String.format(
            """
            {
                "embed": {
                    "title": "%s",
                    "description": "%s",
                    "color": "#2ECC71"
                }
            }
            """,
            ONLINE_PLAYERS,
            PLAYER_LIST
    );

    public static final String DISCORD_GUILD_FORWARDED_MESSAGE_USERNAME_STYLE_DEFAULT = String.format(
            "%s (%s)",
            USER,
            GUILD
    );

    // =================================================================================================================
    //                                             CLIENT CONFIG DEFAULTS
    // =================================================================================================================

    public static final boolean EMOJIFUL_COMPATIBILITY_DEFAULT = false;
    public static final int MAX_CHAT_HISTORY_DEFAULT = 500;
    public static final int MAX_IMAGE_CACHE_SIZE_DEFAULT = 500;
    public static final int IMAGE_LOAD_TIMEOUT_DEFAULT = 15;
    public static final boolean ENABLE_ATTACH_IMAGE_BUTTON_DEFAULT = true;
    public static final boolean ENABLE_CLIPBOARD_IMAGE_PASTE_DEFAULT = true;
}
