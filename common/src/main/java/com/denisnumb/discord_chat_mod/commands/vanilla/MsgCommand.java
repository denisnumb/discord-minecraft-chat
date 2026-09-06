package com.denisnumb.discord_chat_mod.commands.vanilla;

import com.denisnumb.discord_chat_mod.MinecraftEvents;
import com.denisnumb.discord_chat_mod.utils.MinecraftUtils;
import com.denisnumb.discord_chat_mod.chat_style.CustomChatTypeRegistry;
import com.denisnumb.discord_chat_mod.chat_style.MinecraftChatStyleProvider;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

import java.util.Collection;
import java.util.Optional;

import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.processChatMessage;
import static com.denisnumb.discord_chat_mod.chat_style.CustomChatTypeRegistry.buildBound;

public final class MsgCommand {
    private MsgCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        LiteralCommandNode<CommandSourceStack> literalCommandNode = commandDispatcher.register(
                Commands.literal("msg")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("message", MessageArgument.message())
                                        .executes((commandContext) -> {
                                                    Collection<ServerPlayer> collection = EntityArgument.getPlayers(commandContext, "targets");
                                                    if (!collection.isEmpty()) {
                                                        MessageArgument.resolveChatMessage(
                                                                commandContext,
                                                                "message",
                                                                (playerChatMessage) -> sendMessage(
                                                                        (CommandSourceStack) commandContext.getSource(),
                                                                        collection, playerChatMessage
                                                                )
                                                        );
                                                    }

                                                    return collection.size();
                                                }
                                        )
                                )
                        )
        );
        commandDispatcher.register(Commands.literal("tell").redirect(literalCommandNode));
        commandDispatcher.register(Commands.literal("w").redirect(literalCommandNode));
    }

    private static void sendMessage(CommandSourceStack commandSourceStack, Collection<ServerPlayer> collection, PlayerChatMessage playerChatMessage) {
        MinecraftUtils.ProcessChatMessageResult chatMessage
                = processChatMessage(playerChatMessage.decoratedContent().getString(), ChannelCategory.PLAYER_CHAT);

        PlayerChatMessage playerChatMessageStyled = playerChatMessage.withUnsignedContent(chatMessage.forMinecraft());

        MinecraftEvents.handleChatMessage(
                CustomChatTypeRegistry.MSG_COMMAND_INCOMING,
                new MinecraftChatStyleProvider.ChatMessageComponents(commandSourceStack.getDisplayName(), playerChatMessageStyled.decoratedContent(), null, commandSourceStack.getEntity())
        ).ifPresentOrElse(
                styledContent -> sendMessageStyled(commandSourceStack, collection, playerChatMessageStyled, styledContent),
                () -> sendMessageDefault(commandSourceStack, collection, playerChatMessageStyled)
        );
    }

    private static void sendMessageStyled(
            CommandSourceStack commandSourceStack,
            Collection<ServerPlayer> collection,
            PlayerChatMessage playerChatMessage,
            Component styledIncomingContent
    ) {
        ChatType.Bound styledIncoming = buildBound(CustomChatTypeRegistry.MSG_COMMAND_INCOMING, commandSourceStack.registryAccess(), commandSourceStack.getDisplayName(), playerChatMessage.decoratedContent());
        OutgoingChatMessage incomingChatMessage = OutgoingChatMessage.create(playerChatMessage.withUnsignedContent(styledIncomingContent));
        boolean bl = false;

        for (ServerPlayer serverPlayer : collection) {
            Optional<Component> styledOutgoingContentOptional = MinecraftEvents.handleChatMessage(
                    CustomChatTypeRegistry.MSG_COMMAND_OUTGOING,
                    new MinecraftChatStyleProvider.ChatMessageComponents(serverPlayer.getDisplayName(), playerChatMessage.decoratedContent(), null, commandSourceStack.getEntity())
            );

            if (styledOutgoingContentOptional.isPresent()){
                ChatType.Bound styledOutgoing = buildBound(CustomChatTypeRegistry.MSG_COMMAND_OUTGOING, commandSourceStack.registryAccess(), serverPlayer.getDisplayName(), playerChatMessage.decoratedContent());
                OutgoingChatMessage outgoingChatMessage = OutgoingChatMessage.create(playerChatMessage.withUnsignedContent(styledOutgoingContentOptional.get()));

                commandSourceStack.sendChatMessage(outgoingChatMessage, false, styledOutgoing);
                boolean bl2 = commandSourceStack.shouldFilterMessageTo(serverPlayer);
                serverPlayer.sendChatMessage(incomingChatMessage, bl2, styledIncoming);
                bl |= bl2 && playerChatMessage.isFullyFiltered();
            }
        }

        if (bl)
            commandSourceStack.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
    }

    private static void sendMessageDefault(CommandSourceStack commandSourceStack, Collection<ServerPlayer> collection, PlayerChatMessage playerChatMessage){
        ChatType.Bound bound = ChatType.bind(ChatType.MSG_COMMAND_INCOMING, commandSourceStack);
        OutgoingChatMessage outgoingChatMessage = OutgoingChatMessage.create(playerChatMessage);
        boolean bl = false;

        for (ServerPlayer serverPlayer : collection) {
            ChatType.Bound bound2 = ChatType.bind(ChatType.MSG_COMMAND_OUTGOING, commandSourceStack).withTargetName(serverPlayer.getDisplayName());
            commandSourceStack.sendChatMessage(outgoingChatMessage, false, bound2);
            boolean bl2 = commandSourceStack.shouldFilterMessageTo(serverPlayer);
            serverPlayer.sendChatMessage(outgoingChatMessage, bl2, bound);
            bl |= bl2 && playerChatMessage.isFullyFiltered();
        }

        if (bl) {
            commandSourceStack.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
        }
    }
}
