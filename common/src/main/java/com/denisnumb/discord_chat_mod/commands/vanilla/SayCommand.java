package com.denisnumb.discord_chat_mod.commands.vanilla;

import com.denisnumb.discord_chat_mod.MinecraftEvents;
import com.denisnumb.discord_chat_mod.chat_style.CustomChatTypeRegistry;
import com.denisnumb.discord_chat_mod.chat_style.MinecraftChatStyleProvider;
import com.denisnumb.discord_chat_mod.discord.chat_style.MessageType;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import java.util.Map;

import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.processChatMessage;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.ProcessChatMessageResult;
import static com.denisnumb.discord_chat_mod.utils.JavaUtils.mergeMaps;
import static com.denisnumb.discord_chat_mod.chat_style.CustomChatTypeRegistry.buildBound;
import static com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry.getAllContexts;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils.*;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.*;

public final class SayCommand {
    private SayCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(
                Commands.literal("say")
                        .requires(Commands.hasPermission(Commands.LEVEL_MODERATORS))
                        .then(Commands.argument("message", MessageArgument.message())
                                .executes(context -> {
                                    MessageArgument.resolveChatMessage(context, "message", (resolvedMessage) -> {
                                        CommandSourceStack source = context.getSource();
                                        ProcessChatMessageResult chatMessage
                                                = processChatMessage(resolvedMessage.decoratedContent().getString(), ChannelCategory.SAY_COMMAND);

                                        handleDiscord(() -> {
                                            Map<String, String> parameters = mergeMaps(Map.of(MESSAGE, chatMessage.forDiscord()), buildPlayerParameters(source));
                                            getDiscordMessageComponents(MessageType.SAY_COMMAND, parameters)
                                                    .ifPresent(components -> sendMessageFromServer(ChannelCategory.SAY_COMMAND, getAllContexts(), components));
                                        });

                                        Component senderComponent = source.getDisplayName();
                                        Component messageContent = chatMessage.forMinecraft();

                                        MinecraftEvents.handleChatMessage(
                                                CustomChatTypeRegistry.SAY_COMMAND,
                                                new MinecraftChatStyleProvider.ChatMessageComponents(senderComponent, messageContent, null, source.getEntity())
                                        ).ifPresentOrElse(
                                                styledContent -> {
                                                    ChatType.Bound styledBound = buildBound(CustomChatTypeRegistry.SAY_COMMAND, source.registryAccess(), senderComponent, messageContent);
                                                    PlayerChatMessage styledWithMarkdown = resolvedMessage.withUnsignedContent(styledContent);
                                                    source.getServer().getPlayerList().broadcastChatMessage(styledWithMarkdown, source, styledBound);
                                                },
                                                () -> {
                                                    PlayerChatMessage withMarkdown = resolvedMessage.withUnsignedContent(messageContent);
                                                    source.getServer().getPlayerList().broadcastChatMessage(withMarkdown, source, ChatType.bind(ChatType.SAY_COMMAND, source));
                                                }
                                        );
                                    });
                                    return 1;
                                })
                        )
        );
    }
}
