package com.denisnumb.discord_chat_mod.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.List;
import java.util.stream.Stream;

public class ChannelMembersProvider {
    private static Stream<Member> getStream(GuildMessageChannel channel) {
        return channel.getGuild().getMembers().stream().filter(member -> member.hasAccess(channel));
    }

    public static List<String> getNames(GuildMessageChannel channel) {
        return getStream(channel).map(Member::getEffectiveName).toList();
    }

    public static Member getMemberByName(GuildMessageChannel channel, String name) {
        return getStream(channel).filter(member -> member.getEffectiveName().equals(name)).findFirst().orElse(null);
    }
}
