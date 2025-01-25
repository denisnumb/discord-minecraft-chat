package com.denisnumb.discord_chat_mod.commands;

import com.denisnumb.discord_chat_mod.markdown.DiscordMentionData;
import com.denisnumb.discord_chat_mod.markdown.TellRawTextComponent;
import com.denisnumb.discord_chat_mod.utils.ChannelMembersProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.dv8tion.jda.api.entities.Member;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.UNKNOWN_MENTION;
import static com.denisnumb.discord_chat_mod.utils.DiscordUtils.prepareTellRawCommand;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.getTranslate;

public class MentionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("mention")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .suggests(SUGGESTION_PROVIDER)
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");

                                    if (!ChannelMembersProvider.getNames(discordChannel).contains(name)) {
                                        throw new SimpleCommandExceptionType(Component.literal(String.format(
                                                getTranslate(UNKNOWN_MENTION, "There is no user with name %s in the channel #%s"),
                                                name,
                                                discordChannel.getName()
                                        ))).create();
                                    }

                                    if (context.getSource().getEntity() instanceof Player player){
                                        Member mention = ChannelMembersProvider.getMemberByName(discordChannel, name);
                                        server.getCommands().performPrefixedCommand(
                                                server.createCommandSourceStack(),
                                                prepareTellRawCommand(new ArrayList<>() {{
                                                    add(new TellRawTextComponent(String.format("<%s> ", player.getName().getString())));
                                                    add(new TellRawTextComponent(String.format("@%s", name)).setColor(DiscordMentionData.getHexColor(mention.getColor())));
                                                }})
                                        );
                                        discordChannel.sendMessage(String.format("`<%s>` %s", player.getName().getString(), mention.getAsMention())).queue();
                                    }
                                    return 1;
                                })
                        )
        );
    }

    private static boolean matchesPartial(Member member, String partial) {
        return member.getEffectiveName().toLowerCase().contains(partial)
                || member.getUser().getName().toLowerCase().contains(partial);
    }

    private static final SuggestionProvider<CommandSourceStack> SUGGESTION_PROVIDER = (context, builder) -> {
        if (isDiscordConnected()){
            String partial = builder.getRemaining().toLowerCase();

            ChannelMembersProvider.getList(discordChannel)
                    .stream()
                    .filter(member -> matchesPartial(member, partial))
                    .map(Member::getEffectiveName)
                    .forEach(builder::suggest);
        }

        return builder.buildFuture();
    };
}
