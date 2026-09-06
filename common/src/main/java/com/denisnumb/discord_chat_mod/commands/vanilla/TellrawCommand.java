package com.denisnumb.discord_chat_mod.commands.vanilla;

import com.denisnumb.discord_chat_mod.utils.EmojiUtils;
import com.denisnumb.discord_chat_mod.discord.chat_style.MessageType;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.getPlayerListBySelector;
import static com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry.getAllContexts;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils.*;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.getDiscordMessageComponents;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.MESSAGE;

public class TellrawCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context){
        dispatcher.register(
                Commands.literal("tellraw")
                        .requires(Commands.hasPermission(Commands.LEVEL_MODERATORS))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("message", ComponentArgument.textComponent(context))
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
                                            Component message = ComponentArgument.getResolvedComponent(ctx, "message");

                                            if (ctx.getInput().split(" ", 3)[1].equals("@a")){
                                                handleDiscord(() -> {
                                                    StringBuilder messageTextBuilder = new StringBuilder();
                                                    messageTextBuilder.append(parseComponentContents(message.getContents(), message.getStyle()));

                                                    for (Component comp : message.getSiblings())
                                                        messageTextBuilder.append(parseComponentContents(comp.getContents(), comp.getStyle()));

                                                    getDiscordMessageComponents(
                                                            MessageType.TELLRAW_COMMAND,
                                                            Map.of(MESSAGE, EmojiUtils.replaceEmojiCodesToDiscordMentions(messageTextBuilder.toString())))
                                                            .ifPresent(components
                                                                    -> sendMessageFromServer(ChannelCategory.TELLRAW_COMMAND, getAllContexts(), components)
                                                    );
                                                });
                                            }

                                            for (ServerPlayer player : players)
                                                player.sendSystemMessage(ComponentUtils.updateForEntity(ctx.getSource(), message, player, 0), false);
                                            return players.size();
                                        })
                                )
                        )
        );
    }

    private static String parseComponentContents(ComponentContents componentContents, Style style){
        try {
            Component component = componentContents.resolve(null, null, 0);

            if (componentContents instanceof SelectorContents selectorContents){
                List<ServerPlayer> playerList = getPlayerListBySelector(selectorContents.selector().pattern());
                String result = component.getString();

                if (!playerList.isEmpty())
                    result = String.join(", ", playerList.stream().map(p -> p.getDisplayName().getString()).toList());

                return applyStyles(style, result);
            }

            return applyStyles(style, component.getString());
        } catch (CommandSyntaxException e) {
            return "";
        }
    }

    private static String applyStyles(Style style, String translatedText){
        if (style.isBold()) translatedText = "**" + translatedText + "**";
        if (style.isItalic()) translatedText = "*" + translatedText + "*";
        if (style.isStrikethrough()) translatedText = "~~" + translatedText + "~~";
        if (style.isUnderlined()) translatedText = "__" + translatedText + "__";
        if (style.isObfuscated()) translatedText = "||" + translatedText + "||";

        return translatedText;
    }
}
