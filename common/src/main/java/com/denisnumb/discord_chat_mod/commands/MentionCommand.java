package com.denisnumb.discord_chat_mod.commands;

import com.denisnumb.discord_chat_mod.utils.MinecraftUtils;
import com.denisnumb.discord_chat_mod.discord.data_providers.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils;
import com.denisnumb.discord_chat_mod.discord.chat_style.MessageType;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.discord.model.DiscordUserData;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.isDiscordConnected;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.sendMessageToAllPlayersFromPlayer;
import static com.denisnumb.discord_chat_mod.utils.JavaUtils.mergeMaps;
import static com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry.*;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.MESSAGE;

public final class MentionCommand {
    private MentionCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("mention")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(MENTIONS_PROVIDER)
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    List<DiscordUserData> memberData = ChannelMembersProvider.getMemberData(ChannelCategory.PLAYER_CHAT);

                                    if (!isDiscordConnected()) {
                                        throw new SimpleCommandExceptionType(MinecraftLocaleProvider.Server.discordNotConnected()).create();
                                    }

                                    Optional<DiscordUserData> optionalMemberData = memberData.stream()
                                            .filter(data -> data.displayName.equals(name))
                                            .findFirst();

                                    if (optionalMemberData.isEmpty()) {
                                        throw new SimpleCommandExceptionType(
                                                MinecraftLocaleProvider.Command.Mention.Error.unknownMention(name)
                                        ).create();
                                    }

                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        DiscordUserData member = optionalMemberData.get();

                                        Component mentionComponent = MinecraftUtils.buildGradientComponent(member.prettyMention, member.colors)
                                                .withStyle(style -> style
                                                        .withInsertion(member.prettyMention)
                                                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(member.discordName)))
                                                );

                                        sendMessageToAllPlayersFromPlayer(player, mentionComponent);

                                        Map<String, String> parameters = mergeMaps(Map.of(MESSAGE, member.mentionString), buildPlayerParameters(player));
                                        Optional<DiscordMessageComponents> chatComponentsOpt = getDiscordMessageComponents(MessageType.CHAT, parameters);
                                        Optional<DiscordMessageComponents> webhookComponentsOpt = getDiscordMessageComponents(MessageType.CHAT_WEBHOOK, parameters);

                                        if (chatComponentsOpt.isPresent() && webhookComponentsOpt.isPresent())
                                            DiscordMessageUtils.sendMessageFromPlayer(ChannelCategory.PLAYER_CHAT, getAllContexts(), player, webhookComponentsOpt.get(), chatComponentsOpt.get());
                                    }
                                    return 1;
                                })
                        )
        );
    }

    private static boolean matchesPartial(DiscordUserData member, String partial) {
        return member.displayName.toLowerCase().contains(partial)
                || member.discordName.toLowerCase().contains(partial);
    }

    private static final SuggestionProvider<CommandSourceStack> MENTIONS_PROVIDER = (context, builder) -> {
        if (isDiscordConnected()) {
            String partial = builder.getRemaining().toLowerCase();
            StringRange range = StringRange.between(builder.getStart(), builder.getInput().length());

            List<Suggestion> suggestions = ChannelMembersProvider.getMemberData(ChannelCategory.PLAYER_CHAT)
                    .stream()
                    .filter(data -> matchesPartial(data, partial))
                    .map(data -> new Suggestion(range, data.getPrettyMention().substring(1)))
                    .toList();

            return CompletableFuture.completedFuture(new Suggestions(range, suggestions));
        }

        return builder.buildFuture();
    };
}