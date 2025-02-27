package com.denisnumb.discord_chat_mod.discord;

import com.denisnumb.discord_chat_mod.discord.model.DiscordMemberData;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

import java.util.List;
import java.util.stream.Stream;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.isDiscordConnected;

public class ChannelMembersProvider {
    private static long lastGetChannelMembers = 0;
    private static List<Member> cachedMembersList;
    public static List<DiscordMemberData> clientMemberData = List.of();

    public static List<DiscordMemberData> getMemberData(GuildMessageChannel channel) {
        if (!isDiscordConnected())
            return List.of();

        return getList(channel).stream()
                .map(member -> new DiscordMemberData(
                        member.getEffectiveName(),
                        member.getUser().getEffectiveName(),
                        member.getUser().getName(),
                        member.getAsMention(),
                        member.getColor()
                ))
                .toList();
    }

    private static List<Member> getList(GuildMessageChannel channel){
        if (System.currentTimeMillis() - lastGetChannelMembers < 5000) {
            return cachedMembersList;
        }

        lastGetChannelMembers = System.currentTimeMillis();
        List<Member> members = channel.getGuild().getMembers().stream().filter(member -> member.hasAccess(channel)).toList();

        Stream<Member> onlineStream = members.stream()
                .filter(member -> member.getOnlineStatus() != OnlineStatus.OFFLINE)
                .sorted((m1, m2) -> {
                    int m1RolePosition = m1.getRoles().isEmpty() ? 0 : m1.getRoles().get(0).getPosition();
                    int m2RolePosition = m2.getRoles().isEmpty() ? 0 : m2.getRoles().get(0).getPosition();

                    if (m1RolePosition != m2RolePosition)
                        return Integer.compare(m2RolePosition, m1RolePosition);
                    return m1.getEffectiveName().compareToIgnoreCase(m2.getEffectiveName());
                });

        Stream<Member> offlineStream = members.stream()
                .filter(member -> member.getOnlineStatus() == OnlineStatus.OFFLINE)
                .sorted((m1, m2) -> m1.getEffectiveName().compareToIgnoreCase(m2.getEffectiveName()));

        return cachedMembersList = Stream.concat(onlineStream, offlineStream).toList();
    }
}
