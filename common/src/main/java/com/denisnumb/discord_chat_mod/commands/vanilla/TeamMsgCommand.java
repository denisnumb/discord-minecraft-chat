package com.denisnumb.discord_chat_mod.commands.vanilla;

import com.denisnumb.discord_chat_mod.MinecraftEvents;
import com.denisnumb.discord_chat_mod.utils.MinecraftUtils;
import com.denisnumb.discord_chat_mod.chat_style.CustomChatTypeRegistry;
import com.denisnumb.discord_chat_mod.chat_style.MinecraftChatStyleProvider;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.scores.PlayerTeam;

import java.util.List;
import java.util.Optional;

import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.processChatMessage;
import static com.denisnumb.discord_chat_mod.chat_style.CustomChatTypeRegistry.buildBound;

public class TeamMsgCommand {
    private static final Style SUGGEST_STYLE;
    private static final SimpleCommandExceptionType ERROR_NOT_ON_TEAM;

    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        LiteralCommandNode<CommandSourceStack> literalCommandNode = commandDispatcher.register(Commands.literal("teammsg").then(Commands.argument("message", MessageArgument.message()).executes((commandContext) -> {
            CommandSourceStack commandSourceStack = commandContext.getSource();
            Entity entity = commandSourceStack.getEntityOrException();
            PlayerTeam playerTeam = entity.getTeam();
            if (playerTeam == null) {
                throw ERROR_NOT_ON_TEAM.create();
            } else {
                List<ServerPlayer> list = commandSourceStack.getServer().getPlayerList().getPlayers().stream().filter((serverPlayer) -> serverPlayer == entity || serverPlayer.getTeam() == playerTeam).toList();
                if (!list.isEmpty()) {
                    MessageArgument.resolveChatMessage(commandContext, "message", (playerChatMessage) -> sendMessage(commandSourceStack, entity, playerTeam, list, playerChatMessage));
                }

                return list.size();
            }
        })));
        commandDispatcher.register(Commands.literal("tm").redirect(literalCommandNode));
    }

    private static void sendMessage(CommandSourceStack commandSourceStack, Entity entity, PlayerTeam playerTeam, List<ServerPlayer> list, PlayerChatMessage playerChatMessage) {
        MinecraftUtils.ProcessChatMessageResult chatMessage
                = processChatMessage(playerChatMessage.decoratedContent().getString(), ChannelCategory.PLAYER_CHAT);

        PlayerChatMessage playerChatMessageStyled = playerChatMessage.withUnsignedContent(chatMessage.forMinecraft());
        MinecraftEvents.handleChatMessage(
                CustomChatTypeRegistry.TEAM_MSG_COMMAND_INCOMING,
                new MinecraftChatStyleProvider.ChatMessageComponents(
                        commandSourceStack.getDisplayName(),
                        playerChatMessageStyled.decoratedContent(),
                        playerTeam.getFormattedDisplayName().withStyle(SUGGEST_STYLE),
                        entity
                )
        ).ifPresentOrElse(
                styledContent -> sendMessageStyled(commandSourceStack, entity, playerTeam, list, playerChatMessageStyled, styledContent),
                () -> sendMessageDefault(commandSourceStack, entity, playerTeam, list, playerChatMessageStyled)
        );
    }

    private static void sendMessageStyled(
            CommandSourceStack commandSourceStack,
            Entity entity,
            PlayerTeam playerTeam,
            List<ServerPlayer> list,
            PlayerChatMessage playerChatMessage,
            Component styledIncomingContent
    ) {
        Optional<Component> styledOutgoungContentOptional = MinecraftEvents.handleChatMessage(
                CustomChatTypeRegistry.TEAM_MSG_COMMAND_OUTGOING,
                new MinecraftChatStyleProvider.ChatMessageComponents(
                        commandSourceStack.getDisplayName(),
                        playerChatMessage.decoratedContent(),
                        playerTeam.getFormattedDisplayName().withStyle(SUGGEST_STYLE),
                        entity
                )
        );

        if (styledOutgoungContentOptional.isPresent()){
            ChatType.Bound styledIncoming = buildBound(
                    CustomChatTypeRegistry.TEAM_MSG_COMMAND_INCOMING,
                    commandSourceStack.registryAccess(),
                    commandSourceStack.getDisplayName(),
                    playerChatMessage.decoratedContent()
            );

            ChatType.Bound styledOutgoing = buildBound(
                    CustomChatTypeRegistry.TEAM_MSG_COMMAND_OUTGOING,
                    commandSourceStack.registryAccess(),
                    commandSourceStack.getDisplayName(),
                    playerChatMessage.decoratedContent()
            );

            OutgoingChatMessage incomingChatMessage = OutgoingChatMessage.create(playerChatMessage.withUnsignedContent(styledIncomingContent));
            OutgoingChatMessage outgoingChatMessage = OutgoingChatMessage.create(playerChatMessage.withUnsignedContent(styledOutgoungContentOptional.get()));
            boolean bl = false;

            for(ServerPlayer serverPlayer : list) {
                ChatType.Bound bound = serverPlayer == entity ? styledOutgoing : styledIncoming;
                OutgoingChatMessage message = serverPlayer == entity ? outgoingChatMessage : incomingChatMessage;
                boolean bl2 = commandSourceStack.shouldFilterMessageTo(serverPlayer);
                serverPlayer.sendChatMessage(message, bl2, bound);
                bl |= bl2 && playerChatMessage.isFullyFiltered();
            }

            if (bl) {
                commandSourceStack.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
            }
        }
    }

    private static void sendMessageDefault(CommandSourceStack commandSourceStack, Entity entity, PlayerTeam playerTeam, List<ServerPlayer> list, PlayerChatMessage playerChatMessage) {
        Component component = playerTeam.getFormattedDisplayName().withStyle(SUGGEST_STYLE);
        ChatType.Bound bound = ChatType.bind(ChatType.TEAM_MSG_COMMAND_INCOMING, commandSourceStack).withTargetName(component);
        ChatType.Bound bound2 = ChatType.bind(ChatType.TEAM_MSG_COMMAND_OUTGOING, commandSourceStack).withTargetName(component);
        OutgoingChatMessage outgoingChatMessage = OutgoingChatMessage.create(playerChatMessage);
        boolean bl = false;

        for(ServerPlayer serverPlayer : list) {
            ChatType.Bound bound3 = serverPlayer == entity ? bound2 : bound;
            boolean bl2 = commandSourceStack.shouldFilterMessageTo(serverPlayer);
            serverPlayer.sendChatMessage(outgoingChatMessage, bl2, bound3);
            bl |= bl2 && playerChatMessage.isFullyFiltered();
        }

        if (bl) {
            commandSourceStack.sendSystemMessage(PlayerList.CHAT_FILTERED_FULL);
        }
    }

    static {
        SUGGEST_STYLE = Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.type.team.hover"))).withClickEvent(new ClickEvent.SuggestCommand("/teammsg "));
        ERROR_NOT_ON_TEAM = new SimpleCommandExceptionType(Component.translatable("commands.teammsg.failed.noteam"));
    }
}
