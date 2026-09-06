package com.denisnumb.discord_chat_mod.config;

import static com.denisnumb.discord_chat_mod.chat_style.Parameters.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.Translatable.*;
import static com.denisnumb.discord_chat_mod.config.ConfigDefaults.*;

public final class ConfigComments {
    private ConfigComments() {}

    public static final String DISCORD_BOT_TOKEN_COMMENT = """
                     Read more about configuration here: https://github.com/denisnumb/discord-chat-mod/wiki\

                     \

                     Bot access token\
                    
                     [!] Make sure all Privileged Gateway Intents are enabled on https://discord.com/developers/applications/<your_app_id>/bot\
                    
                     [!] Make sure the bot has the following permissions on the server:\
                    
                     [!] - VIEW_CHANNEL\
                    
                     [!] - MESSAGE_SEND\
                    
                     [!] - MESSAGE_SEND_IN_THREADS\
                    
                     [!] - MESSAGE_EMBED_LINKS\
                    
                     [!] - MESSAGE_ATTACH_FILES\
                    
                     [!] - PIN_MESSAGES\
                    
                     [!] - MESSAGE_HISTORY\
                    """;

    public static final String MOD_LOCALE_COMMENT = " Mod locale";

    public static final String UTC_OFFSET_HOURS_COMMENT
            = """
             The offset of the current time from UTC in hours.\

             Used to generate timestamps for message style configurations.\
            """;

    public static final String ENABLE_BOT_PRESENCE_STATUS_COMMENT
            = """
             If true, the bot's Discord presence (custom status) will display the current online player count, e.g. "Online: 3/20".\

             Set to false to disable the presence status entirely (the bot will appear without a custom status).\
            """;

    public static final String MENTION_BOTS_COMMENT
            = """
             If true, Discord bots are included in the @-autocomplete shown in Minecraft chat and can be mentioned from the server.\

             Set to false to hide bots from the autocomplete and stop resolving "@botname" in player messages into a Discord mention.\
            """;

    // =================================================================================================================
    //                                              LOGS CONFIG COMMENTS
    // =================================================================================================================

    public static final String LOG_DISCORD_MESSAGES_COMMENT = " Do logging to the server console messages from discord";

    public static final String LOG_DISCORD_ERRORS_TO_SERVER_CHAT_COMMENT = " Notify about internal Discord interaction errors in the server's in-game chat";

    public static final String DISCORD_ERRORS_CHAT_PLAYER_SELECTOR_COMMENT
            = " If logDiscordErrorsToServerChat=true, then the errors in the chat will be seen by players with the specified selector" +
            "\n By default, \"@a\" — all players. You can specify a specific nickname or attribute, for example, \"@a[tag=admin]\"";

    public static final String SERVER_LOGS_TO_DISCORD_ENABLED_COMMENT
            = """
             Enable sending server logs to Discord\
            
             [!] When enabling, you must also specify a serverLogsChannelId in the guilds section\
            """;

    public static final String SERVER_LOGS_TO_DISCORD_LOGGING_LEVEL_COMMENT
            = " The minimum level of logs that will be sent to Discord.\n Possible values: ERROR, WARN, INFO";

    public static final String SERVER_LOGS_PATTERN_COMMENT
            = """
             Log4j pattern for formatting server logs sent to Discord.\

             Common placeholders: %d{HH:mm:ss} (time), %t (thread), %level (level), %logger{1} (class), %msg (message), %n (newline)\

             Example (clean): "[%d{HH:mm:ss}] %msg%n"\

             Example (verbose): "[%d{HH:mm:ss}] [%t/%level] (%logger{1}) %msg%n"\
            """;

    public static final String COMMAND_LOG_ENABLED_COMMENT
            = """
             Enable logging of commands executed by players to a Discord channel (Guild defaultChannel by default).\

             Command log channel can be overridden for each guild in the guilds.channelOverrides section.\

             Only commands from players whose permission level is at least commandLogMinPermissionLevel are logged.\
            """;

    public static final String COMMAND_LOG_MIN_PERMISSION_LEVEL_COMMENT
            = " Minimum permission level required for a player's commands to be logged (0-4)." +
            "\n 0 = all players, 2 = operators (default), 3 = admins, 4 = owners.";

    public static final String COMMAND_LOG_IGNORED_COMMANDS_COMMENT
            = """
             Comma-separated list of root command names to skip (without the leading /).\
            
             Matching is case-insensitive. Example: ["msg", "tell", "w", "help", "list"]\
            
             Leave blank to log every command that passes the permission level filter.\
            """;

    // =================================================================================================================
    //                                          DISCORD GUILD CONFIG COMMENTS
    // =================================================================================================================

    public static final String GUILD_ID_COMMENT =
            """
             The [[guilds]] section is used to define the main and additional channels within a specific discord guild.\

             You can copy this section entirely and define another discord guild in the configuration.\

             In the guildId field you must specify the identifier of the discord guild to which all the channels listed below belong.\
            """;

    public static final String DEFAULT_CHANNEL_ID_COMMENT
            = " Discord channel ID for messaging with MineCraft\n [!] Make sure the bot has access to the channel and all the permissions listed above.";

    public static final String SERVER_LOGS_CHANNEL_ID_COMMENT
            = " Discord channel ID for logs from Minecraft Server console\n Leave blank if you don't want to send server logs to this guild.";

    public static final String ENABLE_PINNED_STATUS_MESSAGE_COMMENT = " Create a pinned message with the current server status and player list";

    public static final String ENABLE_SLASH_COMMANDS_COMMENT
            = " If true, slash commands will be enabled for this guild (/list, /uptime, /tps, /cmd)";

    // =================================================================================================================
    //                                        SLASH COMMAND PERMISSIONS COMMENTS
    // =================================================================================================================

    public static final String SLASH_COMMAND_PERMISSIONS_DEFAULT_COMMENT
            = """
             Configuration section for permissions to execute server commands using the /cmd slash command\

             You can read more about this configuration section and see examples here: https://github.com/denisnumb/discord-chat-mod/wiki/Discord-server-configuration#slash-commands\
            """;

    public static final String SLASH_COMMAND_PERMISSIONS_MODE_COMMENT
            = """
             Determines how permissions are evaluated when user has multiple roles defined in the configuration.\
    
             Available modes:\
    
               merge    — The permissions of all roles are evaluated in priority order.\

                          If the highest-priority role does not explicitly allow or deny a command, the next role in the hierarchy is checked, and so on.\

                          If no role defines a permission for a command, the command is denied.\

            \

               top-role — The permissions of the highest-priority Discord role are used. Other roles are ignored.\
            """;

    public static final String SLASH_COMMAND_PERMISSIONS_DEFAULT_ALLOW_COMMENT
            = """
             List of /cmd server commands allowed for users who have no configured roles.\
    
             Use ["*"] to allow all commands, or specify command names like ["ban", "kick"].\
    
             Everything not listed here is denied automatically.\
            """;

    public static final String SLASH_COMMAND_PERMISSIONS_DEFAULT_DENY_COMMENT
            = """
             List of /cmd server commands explicitly denied for users who have no configured roles.\
    
             Deny takes priority over allow. A command present in both lists will always be denied.\
    
             In most cases this can be left empty, since unlisted commands are already denied.\
    
             Deny is mainly useful to restrict command list when allow = ["*"].\
            """;

    public static final String SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_COMMENT
            = """
             Role-specific permission overrides. Replace "ExampleRole" with a role name or role ID.\
    
             You can define multiple sections like this for different roles.\
    
             If allow or deny is not specified, the [] (empty) value is used.\
    
             To define permissions for all users regardless of their roles, add a section with the guild ID (@everyone role).\
    
             This is useful in "merge" mode to set a common baseline that applies when no other role matches a command.\
            """;

    public static final String SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_ALLOW_COMMENT
            = " List of /cmd server commands allowed for this role.";

    public static final String SLASH_COMMAND_PERMISSIONS_EXAMPLE_ROLE_DENY_COMMENT
            = " List of /cmd server commands denied for this role.";

    // =================================================================================================================
    //                                              WEBHOOK MODE COMMENTS
    // =================================================================================================================

    public static final String ENABLE_WEBHOOK_MODE_COMMENT
            = """
             If true, server and player messages will be sent in webhook mode.\
            
             [!] Webhook mode works only in regular channels (Threads does not work)\
            
             [!] The bot must have Manage Webhooks permission\
            """;

    public static final String WEBHOOK_SERVER_NAME_COMMENT = " Webhook name for displaying server messages";

    public static final String WEBHOOK_SERVER_AVATAR_URL_COMMENT
            = " Avatar url for the webhook for displaying server messages" +
            "\n Leave blank to display the connected bot's avatar";

    public static final String ENABLE_SET_AVATAR_URL_COMMAND_COMMENT
            = " If true, then all players will have access to the /set_avatar_url <url> command to set a custom avatar, which will be used instead of the webhookPlayerAvatarUrl parameter." +
            "\n The /remove_avatar_url command will also be available to remove an avatar.";

    public static final String WEBHOOK_PLAYER_AVATAR_URL_COMMENT
            = String.format("""
             Url to get the player's display avatar\

             You can specify a link with dynamic parameters <uuid>, <name> and <texture>, which will be automatically substituted when requesting an image\

             <uuid> - player's UUID\

             <name> - player's nickname\

             <texture> - texture hash from the player's GameProfile (works with skin mods like Fabric Tailor or SkinRestorer that write into the server-side profile). Example: https://mc-heads.net/avatar/<texture>\

             Default: %s\
            """, WEBHOOK_PLAYER_AVATAR_URL_DEFAULT);

    public static final String WEBHOOK_PLAYER_DEFAULT_AVATAR_URL_COMMENT
            = String.format(" Url to get the player's display avatar if dynamic link is invalid" +
            "\n Default: %s", WEBHOOK_PLAYER_DEFAULT_AVATAR_URL_DEFAULT);

    // =================================================================================================================
    //                                              DISCORD PROXY COMMENTS
    // =================================================================================================================

    public static final String PROXY_HOSTNAME_COMMENT
            = " Configuring the HTTP proxy when connecting with Discord. Do not modify if you don't understand what this is.";

    public static final String PROXY_USER_COMMENT
            = " Leave blank if no certificate required.";

    // =================================================================================================================
    //                                             CHANNEL OVERRIDES COMMENTS
    // =================================================================================================================

    public static final String DUPLICATE_MESSAGES_COMMENT
            = " If true, messages will still be sent to the main channel, but will also be duplicated to the specified channels.";

    public static final String PINNED_STATUS_MESSAGE_CHANNEL_ID_COMMENT
            = " Overrides the default channel for pinned server status message. If empty, uses defaultChannelId.";

    public static final String DEATHS_CHANNEL_ID_COMMENT
            = " Overrides the default channel for player death messages. If empty, uses defaultChannelId." +
            "\n Specify \"-1\" to disable sending death messages to Discord";

    public static final String ADVANCEMENTS_CHANNEL_ID_COMMENT
            = " Overrides the default channel for player advancement messages. If empty, uses defaultChannelId." +
            "\n Specify \"-1\" to disable sending player advancement messages to Discord";

    public static final String SERVER_START_STOP_CHANNEL_ID_COMMENT
            = " Overrides the default channel for server started/closed messages. If empty, uses defaultChannelId." +
            "\n Specify \"-1\" to disable sending server started/closed messages to Discord";

    public static final String PLAYER_JOIN_LEAVE_CHANNEL_ID_COMMENT
            = " Overrides the default channel for player join/leave messages. If empty, uses defaultChannelId." +
            "\n Specify \"-1\" to disable sending player join/leave messages to Discord";

    public static final String PLAYER_CHAT_MESSAGES_CHANNEL_ID_COMMENT
            = " Overrides the default channel for messages from players in Minecraft chat. If empty, uses defaultChannelId." +
            "\n Specify \"-1\" to disable sending messages from Minecraft players chat to Discord";

    public static final String IMAGES_CHANNEL_ID_COMMENT
            = " Overrides the default channel for images sent from Minecraft. If empty, uses defaultChannelId." +
            "\n Specify \"-1\" to disable sending images from Minecraft to Discord";

    public static final String TELLRAW_CHANNEL_ID_COMMENT
            = " Overrides the default channel for messages sent using /tellraw @a command. If empty, uses defaultChannelId." +
            "\n Specify \"-1\" to disable sending messages sent using /tellraw @a command to Discord";

    public static final String SAY_CHANNEL_ID_COMMENT
            = " Overrides the default channel for messages sent using /say command. If empty, uses defaultChannelId." +
            "\n Specify \"-1\" to disable sending messages send using /say command to Discord";

    public static final String ME_CHANNEL_ID_COMMENT
            = " Overrides the default channel for messages sent using /me command. If empty, uses defaultChannelId." +
            "\n Specify \"-1\" to disable sending messages send using /me command to Discord";

    public static final String COMMAND_LOG_CHANNEL_ID_COMMENT
            = " Overrides the default channel for logged player commands. If empty, uses defaultChannelId." +
            "\n Specify \"-1\" to disable sending command log messages to Discord";

    // =================================================================================================================
    //                                      MINECRAFT CHAT CUSTOMIZATION COMMENTS
    // =================================================================================================================

    public static final String ENABLE_MINECRAFT_CHAT_CUSTOMIZATION_COMMENT = String.format(
            """
             If true, messages in the game chat will be formatted in the formats specified below.\
            
             For each format, you can use dynamic parameters specified in the comment above the parameter.\

             Global parameters available for all styles listed below:\

             %s, %s, %s — Hours, minutes, and seconds at the time the message was sent. Example usage: "[%s:%s] <%s> %s"\

             %s, %s, %s, %s — the player's coordinates and dimension name\

             You can read more about this configuration section and see examples here: https://github.com/denisnumb/discord-chat-mod/wiki/Minecraft-Chat-Customization\

             [!] If enabled, make sure you don't have any other chat styling mods installed.\
            """,
            HH, MM, SS,
            HH, MM, PLAYER, MESSAGE,
            X, Y, Z, DIMENSION
    );

    public static final String MINECRAFT_CHAT_LINK_COLOR_COMMENT
            = " Color of links in game chat";

    public static final String MINECRAFT_DISCORD_MESSAGES_STYLE_COMMENT = String.format(
            """
            Style for displaying Discord messages in game chat\
           
            Parameters: %s, %s, %s\
           
            Default: "%s"\
           """,
            GUILD,
            MEMBER,
            MESSAGE,
            MINECRAFT_DISCORD_MESSAGES_STYLE_DEFAULT
    );

    public static final String MINECRAFT_PLAYER_MESSAGE_STYLE_COMMENT = String.format(
            """
            Chat message from player\
           
            Parameters: %s, %s\
           
            Default: "%s"\
           """,
            PLAYER,
            MESSAGE,
            MINECRAFT_PLAYER_MESSAGE_STYLE_DEFAULT
    );

    public static final String MINECRAFT_PLAYER_JOINED_STYLE_COMMENT = String.format(
            """
            Player joined the game\
           
            Parameters: %s, %s\
           
            Default: "%s"\
           """,
            PLAYER,
            PLAYER_JOINED,
            MINECRAFT_PLAYER_JOINED_STYLE_DEFAULT
    );

    public static final String MINECRAFT_PLAYER_LEFT_STYLE_COMMENT = String.format(
            """
            Player left the game\
           
            Parameters: %s, %s\
           
            Default: "%s"\
           """,
            PLAYER,
            PLAYER_LEFT,
            MINECRAFT_PLAYER_LEFT_STYLE_DEFAULT
    );

    public static final String MINECRAFT_PLAYER_DEATH_STYLE_COMMENT = String.format(
            """
             Player death message\

             Death messages are composite and contain: Player Name, Cause of Death, Second Entity (optional), Murder Weapon (optional).\

             You can customize them separately using the configuration fields below.\

             Parameters: %s, %s, %s, %s\
            """,
            DEATH_CAUSE,
            PLAYER,
            SECOND_ENTITY,
            ITEM
    );

    public static final String MINECRAFT_PLAYER_ADVANCEMENT_TASK_STYLE_COMMENT = String.format(
            """
            Player has made the advancement\
           
            Parameters: %s, %s, %s\
           
            Default: "%s"\
           """,
            PLAYER,
            ADVANCEMENT_TASK,
            ADVANCEMENT,
            MINECRAFT_PLAYER_ADVANCEMENT_TASK_STYLE_DEFAULT
    );

    public static final String MINECRAFT_PLAYER_ADVANCEMENT_GOAL_STYLE_COMMENT = String.format(
            """
            Player has reached the goal\
           
            Parameters: %s, %s, %s\
           
            Default: "%s"\
           """,
            PLAYER,
            ADVANCEMENT_GOAL,
            ADVANCEMENT,
            MINECRAFT_PLAYER_ADVANCEMENT_GOAL_STYLE_DEFAULT
    );

    public static final String MINECRAFT_PLAYER_ADVANCEMENT_CHALLENGE_STYLE_COMMENT = String.format(
            """
            Player has completed the challenge\
           
            Parameters: %s, %s, %s\
           
            Default: "%s"\
           """,
            PLAYER,
            ADVANCEMENT_CHALLENGE,
            ADVANCEMENT,
            MINECRAFT_PLAYER_ADVANCEMENT_CHALLENGE_STYLE_DEFAULT
    );

    public static final String MINECRAFT_TEAM_MESSAGE_SENT_STYLE_COMMENT = String.format(
            """
            Team message sent by the player\
           
            Parameters: %s, %s, %s\
           
            Default: "%s"\
           """,
            TEAM,
            PLAYER,
            MESSAGE,
            MINECRAFT_TEAM_MESSAGE_SENT_STYLE_DEFAULT
    );

    public static final String MINECRAFT_TEAM_MESSAGE_RECEIVED_STYLE_COMMENT = String.format(
            """
            Team message received from any player\
           
            Parameters: %s, %s, %s\
           
            Default: "%s"\
           """,
            TEAM,
            PLAYER,
            MESSAGE,
            MINECRAFT_TEAM_MESSAGE_RECEIVED_STYLE_DEFAULT
    );

    public static final String MINECRAFT_TELL_MESSAGE_SENT_STYLE_COMMENT = String.format(
            """
            Private message sent by the player using /tell or /msg command\
           
            Parameters: %s, %s, %s\
           
            Default: "%s"\
           """,
            COMMANDS_MESSAGE_DISPLAY_OUTGOING,
            RECEIVER,
            MESSAGE,
            MINECRAFT_TELL_MESSAGE_SENT_STYLE_DEFAULT
    );

    public static final String MINECRAFT_TELL_MESSAGE_RECEIVED_STYLE_COMMENT = String.format(
            """
            Private message received from any player who used the /tell or /msg command\
           
            Parameters: %s, %s, %s\
           
            Default: "%s"\
           """,
            SENDER,
            COMMANDS_MESSAGE_DISPLAY_INCOMING,
            MESSAGE,
            MINECRAFT_TELL_MESSAGE_RECEIVED_STYLE_DEFAULT
    );

    public static final String MINECRAFT_SAY_COMMAND_STYLE_COMMENT = String.format(
            """
            Chat message from player sent using /say command\
           
            Parameters: %s, %s\
           
            Default: "%s"\
           """,
            PLAYER,
            MESSAGE,
            MINECRAFT_SAY_COMMAND_STYLE_DEFAULT
    );

    public static final String MINECRAFT_ME_COMMAND_STYLE_COMMENT = String.format(
            """
            Chat message from player sent using /me command\
           
            Parameters: %s, %s\
           
            Default: "%s"\
           """,
            PLAYER,
            MESSAGE,
            MINECRAFT_ME_COMMAND_STYLE_DEFAULT
    );

    // =================================================================================================================
    //                                      DISCORD CHAT CUSTOMIZATION COMMENTS
    // =================================================================================================================

    public static final String DISCORD_PLAYER_MESSAGE_STYLE_COMMENT = String.format(
            """
             The configuration settings below determine how messages sent from Minecraft to Discord will be formatted.\

             The message format is specified in JSON format. You can remove the parameter from the configuration to get the default value\

             For each format, you can use dynamic parameters specified in the comment above the parameter.\

             Global parameters available for all styles listed below:\

             %s — link to the player's avatar (available wherever the %s parameter is present)\

             %s, %s, %s, %s — the player's coordinates and dimension name (available wherever the %s parameter is present)\

             %s — current number of seconds in the system. It's convenient to substitute when using Discord timestamps, for example: <t:%s:R>\

             %s — A string with the current date and time in the format "2025-11-02T10:00:00.000Z". May be useful for embed timestamps.\

             You can read more about this configuration section and see examples here: https://github.com/denisnumb/discord-chat-mod/wiki/Discord-Chat-Customization\

             \

             Chat message from player\
            
             Parameters: %s, %s\
            """,
            PLAYER_AVATAR_URL, PLAYER,
            X, Y, Z, DIMENSION, PLAYER,
            TIMESTAMP, TIMESTAMP,
            DATETIME,
            PLAYER, MESSAGE
    );

    public static final String DISCORD_PLAYER_MESSAGE_WEBHOOK_STYLE_COMMENT = String.format(
            """
             Chat message from player (if webhook mode is enabled) \
            
             Parameters: %s, %s\
            """,
            PLAYER,
            MESSAGE
    );

    public static final String DISCORD_PLAYER_JOINED_STYLE_COMMENT = String.format(
            """
             Player joined the game\
            
             Parameters: %s, %s\
            """,
            PLAYER,
            PLAYER_JOINED
    );

    public static final String DISCORD_PLAYER_LEFT_STYLE_COMMENT = String.format(
            """
             Player left the game\
            
             Parameters: %s, %s\
            """,
            PLAYER,
            PLAYER_LEFT
    );

    public static final String DISCORD_PLAYER_DEATH_CAUSE_STYLE_COMMENT = String.format(
            """
             You can apply Markdown to individual components of the death message using the parameters below.\

             They will ultimately be compiled into the %s substitution parameter, which you can use to form the final message in Discord.\
            """,
            DEATH_MESSAGE
    );

    public static final String DISCORD_PLAYER_DEATH_MESSAGE_STYLE_COMMENT = String.format(
            """
             Player death message (configure parameters above)\

             Parameters: %s, %s\
            """,
            PLAYER,
            DEATH_MESSAGE
    );

    public static final String DISCORD_PLAYER_ADVANCEMENT_TASK_STYLE_COMMENT = String.format(
            """
             Player has made the advancement\
            
             Parameters: %s, %s, %s, %s, %s\
            """,
            PLAYER,
            ADVANCEMENT_TASK,
            ADVANCEMENT,
            DESCRIPTION,
            ICON_URL
    );

    public static final String DISCORD_PLAYER_ADVANCEMENT_GOAL_STYLE_COMMENT = String.format(
            """
             Player has reached the goal\
            
             Parameters: %s, %s, %s, %s, %s\
            """,
            PLAYER,
            ADVANCEMENT_GOAL,
            ADVANCEMENT,
            DESCRIPTION,
            ICON_URL
    );

    public static final String DISCORD_PLAYER_ADVANCEMENT_CHALLENGE_STYLE_COMMENT = String.format(
            """
             Player has completed the challenge\
            
             Parameters: %s, %s, %s, %s, %s\
            """,
            PLAYER,
            ADVANCEMENT_CHALLENGE,
            ADVANCEMENT,
            DESCRIPTION,
            ICON_URL
    );

    public static final String DISCORD_SAY_COMMAND_STYLE_COMMENT = String.format(
            """
             Chat message from player sent using /say command\
            
             Parameters: %s, %s\
            """,
            PLAYER,
            MESSAGE
    );

    public static final String DISCORD_ME_COMMAND_STYLE_COMMENT = String.format(
            """
             Chat message from player sent using /me command\
            
             Parameters: %s, %s\
            """,
            PLAYER,
            MESSAGE
    );

    public static final String DISCORD_ME_COMMAND_WEBHOOK_STYLE_COMMENT = String.format(
            """
             Chat message from player sent using /me command (if webhook mode is enabled)\
            
             Parameters: %s, %s\
            """,
            PLAYER,
            MESSAGE
    );

    public static final String DISCORD_TELLRAW_COMMAND_STYLE_COMMENT = String.format(
            """
             Chat message from send using /tellraw @a command\

             Parameters: %s\
            """,
            MESSAGE
    );

    public static final String DISCORD_COMMAND_LOG_STYLE_COMMENT = String.format(
            """
             Command executed by a player above the configured permission level\

             Parameters: %s, %s\
            """,
            PLAYER,
            COMMAND
    );

    public static final String DISCORD_IMAGE_MESSAGE_STYLE_COMMENT = String.format(
            """
             Image from player\
            
             Parameters: %s, %s\
            """,
            PLAYER,
            IMAGE_URL
    );

    public static final String DISCORD_IMAGE_MESSAGE_WEBHOOK_STYLE_COMMENT = String.format(
            """
             Image from player (if webhook mode is enabled)\
            
             Parameters: %s, %s\
            """,
            PLAYER,
            IMAGE_URL
    );

    public static final String DISCORD_SERVER_STARTED_MESSAGE_STYLE_COMMENT = String.format(
            """
             Server started message\
            
             Parameters: %s\
            """,
            SERVER_STARTED
    );

    public static final String DISCORD_LOCAL_SERVER_STARTED_MESSAGE_STYLE_COMMENT = String.format(
            """
             Local server started message\
            
             Parameters: %s, %s\
            """,
            LOCAL_SERVER_STARTED,
            SERVER_PORT
    );

    public static final String DISCORD_SERVER_CLOSED_MESSAGE_STYLE_COMMENT = String.format(
            """
             Server closed message\
            
             Parameters: %s\
            """,
            SERVER_CLOSED
    );

    public static final String DISCORD_PINNED_STATUS_MESSAGE_SERVER_UNAVAILABLE_STYLE_COMMENT = String.format(
            """
             Pinned server status message when server is unavailable\
            
             Parameters: %s\
            """,
            SERVER_UNAVAILABLE
    );

    public static final String DISCORD_PINNED_STATUS_MESSAGE_SERVER_AVAILABLE_STYLE_COMMENT = String.format(
            """
             Pinned server status message when server is available but there are no players\
            
             Parameters: %s\
            """,
            SERVER_AVAILABLE
    );

    public static final String DISCORD_PINNED_STATUS_MESSAGE_PLAYER_LIST_DELIMITER_COMMENT = String.format(
            " The characters separating player nicknames in the %s parameter for the pinned status message style",
            PLAYER_LIST
    );

    public static final String DISCORD_PINNED_STATUS_MESSAGE_PLAYER_LIST_NICKNAME_STYLE_COMMENT = String.format(
            """
             Style of a single nickname in the %s parameter of a pinned status message\
            
             Parameters: %s, %s — can be used to number players in order\
            """,
            PLAYER_LIST,
            PLAYER,
            COUNTER
    );

    public static final String DISCORD_PINNED_STATUS_MESSAGE_STYLE_COMMENT = String.format(
            """
             Pinned server status message when there are players on the server\
            
             Parameters: %s, %s, %s, %s\
            """,
            ONLINE_PLAYERS,
            PLAYER_LIST,
            PLAYER_COUNT,
            MAX_PLAYERS
    );

    public static final String DISCORD_GUILD_FORWARDED_MESSAGE_USERNAME_STYLE_COMMENT = String.format(
            """
             The username displayed in Discord when forwarding a message from another guild\
            
             Parameters: %s, %s, %s\
            """,
            MEMBER,
            USER,
            GUILD
    );

    // =================================================================================================================
    //                                             CLIENT CONFIG COMMENTS
    // =================================================================================================================

    public static final String EMOJIFUL_COMPATIBILITY_COMMENT = """
                     Can be set to "true" for correct compatibility with the Emojiful mod. Enabling this option:\
                    
                     1) Disables Discord Chat Mod's emoji suggestions to avoid conflicts with Emojiful\
                    
                     2) Disables Discord Chat Mod's rendering of custom Discord Emoji\
                    
                     3) Convert emoji sent from Discord to their names. For example, "\uD83D\uDE03" will be converted to ":smiley:"\
                    """;

    public static final String MAX_CHAT_HISTORY_COMMENT = """
                     Maximum number of messages in the chat.\
                    
                     When the number of messages exceeds this value, old messages are automatically deleted.\
                    
                     By default, this value is 100 in the vanilla game.\
                    """;

    public static final String MAX_IMAGE_CACHE_SIZE_COMMENT = """
                     Maximum number of images in the cache.\

                     The image cache stores images sent by players, custom discord emojis and stickers.\

                     If the set limit is exceeded, the least frequently used images will be removed from the cache.\
                    """;

    public static final String IMAGE_LOAD_TIMEOUT_COMMENT = """
                     Maximum time in seconds for establishing a connection and reading data when loading an image via a link.\
                    """;

    public static final String ENABLE_ATTACH_IMAGE_BUTTON_COMMENT
            = " When set to \"true\" it adds a button to the right corner of the chat for selecting and sending an image from your computer.";

    public static final String ENABLE_CLIPBOARD_IMAGE_PASTE_COMMENT
            = " When set to \"true\" it allows pasting images from the clipboard into the chat by pressing Ctrl+V.";
}
