package com.denisnumb.discord_chat_mod.commands.set_avatar;

import com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.denisnumb.discord_chat_mod.markdown.MarkdownParser;
import com.denisnumb.discord_chat_mod.markdown.MarkdownToComponentConverter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;


public class SetAvatarCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("set_avatar_url")
                .then(Commands.argument("url", StringArgumentType.greedyString())
                        .executes(context -> {
                            String url = StringArgumentType.getString(context, "url");

                            if (!ImageUtils.isImageUrl(ImageUtils.getMimeType(url)))
                                throw new SimpleCommandExceptionType(MinecraftLocaleProvider.Command.SetAvatarUrl.Error.invalidUrl()).create();

                            ServerPlayer player = context.getSource().getPlayer();
                            if (player == null)
                                return 0;

                            AvatarUrlStorage.setUrl(player.getUUID(), url, context.getSource().getServer());

                            player.sendSystemMessage(MinecraftLocaleProvider.Command.SetAvatarUrl.success(
                                    new MarkdownToComponentConverter(
                                            MarkdownParser.parseMarkdown(url)
                                    ).convertMarkdownTokensToComponent()
                            ));
                            return 1;
                        })
                )
        );

        dispatcher.register(Commands.literal("remove_avatar_url")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null)
                        return 0;

                    AvatarUrlStorage.removeUrl(player.getUUID(), context.getSource().getServer());
                    player.sendSystemMessage(MinecraftLocaleProvider.Command.RemoveAvatarUrl.success()
                            .setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
                    return 1;
                })
        );
    }
}
