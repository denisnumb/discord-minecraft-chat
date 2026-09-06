package com.denisnumb.discord_chat_mod.discord.data_providers;

import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.discord.model.DiscordUserData;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Member;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.isDiscordConnected;

public final class ChannelMembersProvider {
    private ChannelMembersProvider() {}

    private static long lastGetChannelMembers = 0;
    private static List<Member> cachedMembersList;
    public static List<DiscordUserData> CLIENT_MEMBER_CACHE = List.of();

    public static void dropTimeouts(){
        lastGetChannelMembers = 0;
    }

    public static List<DiscordUserData> getMemberData(ChannelCategory channelCategoryToParseMembers) {
        if (!isDiscordConnected())
            return List.of();

        boolean mentionBots = ConfigProvider.getConfig().mentionBots();

        return getList(channelCategoryToParseMembers).stream()
                .filter(member -> mentionBots || !member.getUser().isBot())
                .map(member -> new DiscordUserData(
                        getMemberDisplayName(member),
                        member.getUser().getName(),
                        member.getAsMention(),
                        member.getColors()
                ))
                .toList();
    }

    public static String getMemberDisplayName(Member member){
        return DiscordChannelRegistry.getAllContexts().size() > 1
                ? member.getUser().getEffectiveName()
                : member.getEffectiveName();
    }

    private static List<Member> getList(ChannelCategory channelCategoryToParseMembers) {
        if (System.currentTimeMillis() - lastGetChannelMembers < 5000)
            return cachedMembersList;
        lastGetChannelMembers = System.currentTimeMillis();

        List<Member> members = DiscordChannelRegistry.getAllContexts()
                .stream()
                .flatMap(guildContext -> {
                    var channel = guildContext.getDefaultChannelIfCategoryDisabled(channelCategoryToParseMembers);
                    if (channel == null)
                        return Stream.empty();
                    return guildContext.getGuild().getMembers()
                            .stream()
                            .filter(member -> member.hasAccess(channel));
                })
                .collect(Collectors.toMap(
                        member -> member.getUser().getIdLong(),
                        Function.identity(),
                        (first, duplicate) -> first
                ))
                .values()
                .stream()
                .toList();

        Stream<Member> onlineStream = members.stream()
                .filter(member -> member.getOnlineStatus() != OnlineStatus.OFFLINE)
                .sorted((m1, m2) -> {
                    int m1RolePosition = m1.getRoles().isEmpty() ? 0 : m1.getRoles().getFirst().getPosition();
                    int m2RolePosition = m2.getRoles().isEmpty() ? 0 : m2.getRoles().getFirst().getPosition();

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
