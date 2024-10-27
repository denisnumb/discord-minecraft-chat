package com.denisnumb.discord_chat_mod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = DiscordChatMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> DISCORD_BOT_TOKEN = BUILDER
            .comment("Bot access token")
            .define("discordBotToken", "");

    private static final ForgeConfigSpec.ConfigValue<String> DISCORD_CHANNEL_ID = BUILDER
            .comment("Discord channel ID for messaging with MineCraft")
            .define("discordChannelId", "");

    private static final ForgeConfigSpec.BooleanValue LOG_DISCORD_MESSAGES = BUILDER
            .comment("Do logging to the server console messages from discord")
            .define("logDiscordMessages", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();
    public static String discordBotToken;
    public static String discordChannelId;
    public static boolean logDiscordMessages;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        discordBotToken = DISCORD_BOT_TOKEN.get();
        discordChannelId = DISCORD_CHANNEL_ID.get();
        logDiscordMessages = LOG_DISCORD_MESSAGES.get();
    }
}
