package com.denisnumb.discord_chat_mod;


import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = DiscordChatMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<String> DISCORD_BOT_TOKEN = BUILDER
            .comment("Bot access token")
            .define("discordBotToken", "");

    private static final ModConfigSpec.ConfigValue<String> DISCORD_CHANNEL_ID = BUILDER
            .comment("Discord channel ID for messaging with MineCraft")
            .define("discordChannelId", "");

    private static final ModConfigSpec.BooleanValue LOG_DISCORD_MESSAGES = BUILDER
            .comment("Do logging to the server console messages from discord")
            .define("logDiscordMessages", true);

    private static final ModConfigSpec.BooleanValue ENABLE_PINNED_STATUS_MESSAGE = BUILDER
            .comment("Create a pinned message with the current server status and player list")
            .define("enablePinnedStatusMessage", true);

    private static final ModConfigSpec.ConfigValue<String> MOD_LOCALE = BUILDER
            .comment("Mod locale")
            .define("modLocale", "en_us");

    static final ModConfigSpec SPEC = BUILDER.build();
    public static String discordBotToken;
    public static String discordChannelId;
    public static boolean logDiscordMessages;
    public static boolean enablePinnedStatusMessage;
    public static String modLocale;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        discordBotToken = DISCORD_BOT_TOKEN.get();
        discordChannelId = DISCORD_CHANNEL_ID.get();
        logDiscordMessages = LOG_DISCORD_MESSAGES.get();
        enablePinnedStatusMessage = ENABLE_PINNED_STATUS_MESSAGE.get();
        modLocale = MOD_LOCALE.get();
    }
}
