package com.denisnumb.discord_chat_mod.commands;

import com.denisnumb.discord_chat_mod.discord.model.DiscordMemberData;
import com.denisnumb.discord_chat_mod.markdown.tellraw.TellRawComponent;
import com.denisnumb.discord_chat_mod.discord.ChannelMembersProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.UNKNOWN_MENTION;
import static com.denisnumb.discord_chat_mod.discord.DiscordUtils.prepareTellRawCommand;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.getTranslate;

public class MentionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("mention")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(SUGGESTION_PROVIDER)
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    List<DiscordMemberData> memberData = ChannelMembersProvider.getMemberData(discordChannel);

                                    if (memberData.stream().noneMatch(data -> data.guildNickname.equals(name))) {
                                        throw new SimpleCommandExceptionType(Component.literal(String.format(
                                                getTranslate(UNKNOWN_MENTION, "There is no user with name %s in the channel #%s"),
                                                name,
                                                discordChannel.getName()
                                        ))).create();
                                    }

                                    if (context.getSource().getEntity() instanceof Player player){
                                        DiscordMemberData member = memberData.stream()
                                                .filter(data -> data.guildNickname.equals(name))
                                                .findFirst()
                                                .get();

                                        server.getCommands().performPrefixedCommand(
                                                server.createCommandSourceStack(),
                                                prepareTellRawCommand(new ArrayList<>() {{
                                                    add(new TellRawComponent(String.format("<%s> ", player.getName().getString())));
                                                    add(new TellRawComponent(String.format("@%s", name)).setColor(member.color));
                                                }})
                                        );
                                        discordChannel.sendMessage(String.format("`<%s>` %s", player.getName().getString(), member.mentionString)).queue();
                                    }
                                    return 1;
                                })
                        )
        );
    }

    private static boolean matchesPartial(DiscordMemberData member, String partial) {
        return member.guildNickname.toLowerCase().contains(partial)
                || member.discordNickName.toLowerCase().contains(partial)
                || member.discordName.toLowerCase().contains(partial);
    }

    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_PROVIDER = (context, builder) -> {
        if (isDiscordConnected()){
            String partial = builder.getRemaining().toLowerCase();

            ChannelMembersProvider.getMemberData(discordChannel)
                    .stream()
                    .filter(data -> matchesPartial(data, partial))
                    .map(DiscordMemberData::getGuildNickname)
                    .forEach(builder::suggest);
        }

        return builder.buildFuture();
    };
}
