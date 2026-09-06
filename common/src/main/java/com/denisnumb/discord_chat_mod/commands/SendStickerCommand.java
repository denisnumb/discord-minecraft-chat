package com.denisnumb.discord_chat_mod.commands;

import com.denisnumb.discord_chat_mod.discord.data_providers.StickersProvider;
import com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider;
import com.denisnumb.discord_chat_mod.discord.chat_style.MessageType;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.locale.DiscordLocaleProvider;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.sendMessageToAllPlayersFromPlayer;
import static com.denisnumb.discord_chat_mod.utils.JavaUtils.mergeMaps;
import static com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry.*;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils.handleDiscord;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils.sendMessageFromPlayer;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.buildPlayerParameters;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.getDiscordMessageComponents;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.MESSAGE;

public final class SendStickerCommand {
    private SendStickerCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("send_sticker")
                        .then(Commands.argument("sticker", StringArgumentType.greedyString())
                                .suggests(STICKERS_PROVIDER)
                                .executes(context -> {
                                    String stickerName = StringArgumentType.getString(context, "sticker");

                                    StickersProvider.StickerData stickerData = StickersProvider.getNameToStickerDataMap().get(stickerName);

                                    if (stickerData == null)
                                        throw new SimpleCommandExceptionType(MinecraftLocaleProvider.Command.SendSticker.Error.unknownSticker(stickerName)).create();

                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        String stickerMessageContent = DiscordLocaleProvider.sticker(stickerData.originalName());

                                        Component messageWithStickerComponent = MinecraftLocaleProvider.sticker(stickerData.originalName())
                                                .withStyle(style -> style
                                                        .withItalic(true)
                                                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(stickerData.imageUrl())))
                                        );

                                        sendMessageToAllPlayersFromPlayer(player, messageWithStickerComponent);

                                        handleDiscord(() -> {
                                            Optional<DiscordChatStyleProvider.DiscordMessageComponents> chatComponentsOpt = getDiscordMessageComponents(
                                                    MessageType.CHAT,
                                                    mergeMaps(Map.of(MESSAGE, stickerMessageContent), buildPlayerParameters(player))
                                            );
                                            DiscordChatStyleProvider.DiscordMessageComponents webhookComponents
                                                    = new DiscordChatStyleProvider.DiscordMessageComponents(Optional.of(stickerData.imageUrl()), Optional.empty());

                                            chatComponentsOpt.ifPresent(discordMessageComponents ->
                                                    sendMessageFromPlayer(ChannelCategory.PLAYER_CHAT, getAllContexts(), player, webhookComponents, discordMessageComponents, stickerData)
                                            );
                                        });
                                    }
                                    return 1;
                                })
                        )
        );
    }

    private static final SuggestionProvider<CommandSourceStack> STICKERS_PROVIDER = (context, builder) -> {
        String partial = builder.getRemaining().toLowerCase();

        StickersProvider.getNameToStickerDataMap().keySet().stream()
                .filter(stickerName -> stickerName.toLowerCase().contains(partial))
                .forEach(builder::suggest);

        return builder.buildFuture();
    };
}
