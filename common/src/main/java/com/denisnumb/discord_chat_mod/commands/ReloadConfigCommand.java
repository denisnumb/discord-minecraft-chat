package com.denisnumb.discord_chat_mod.commands;

import com.denisnumb.discord_chat_mod.ServerLogsRetranslator;
import com.denisnumb.discord_chat_mod.config.ConfigManager;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.CustomEmojiProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.StickersProvider;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.locale.LocaleStorage.loadLocalization;

public final class ReloadConfigCommand {
    private ReloadConfigCommand() {}

    public static boolean isReloadingNow = false;

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("reload_discord_chat_mod_config")
                .requires(Commands.hasPermission(Commands.LEVEL_MODERATORS))
                .executes(ctx -> {
                    if (isReloadingNow)
                        return 0;

                    Thread t = new Thread(() -> {
                        boolean discordConnected = isDiscordConnected();
                        isReloadingNow = true;

                        if (discordConnected)
                            stopJDA();

                        ConfigManager.load(false);
                        ChannelMembersProvider.dropTimeouts();
                        StickersProvider.dropTimeouts();
                        CustomEmojiProvider.dropTimeouts();
                        loadLocalization();

                        if (ctx.getSource().getPlayer() instanceof ServerPlayer player)
                            player.sendSystemMessage(MinecraftLocaleProvider.Command.ReloadConfig.success());

                        isReloadingNow = false;

                        if (discordConnected){
                            initJDA();

                            if (ConfigProvider.getConfig().isServerLogsToDiscordEnabled())
                                ServerLogsRetranslator.start();
                        }
                    });
                    t.setDaemon(true);
                    t.start();

                    return 1;
        }));
    }
}
