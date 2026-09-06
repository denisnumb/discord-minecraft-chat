package com.denisnumb.discord_chat_mod.discord.utils;

import com.denisnumb.discord_chat_mod.discord.model.DiscordMentionData;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DiscordMentionsUtils {
    private DiscordMentionsUtils() {}
    private static final Pattern MENTION_PATTERN = Pattern.compile("<(@!?|@&|#)(\\d+)>");

    public static Map<String, DiscordMentionData> collectMessageMentions(Message message) {
        Map<String, DiscordMentionData> mentions = new HashMap<>();

        message.getMentions()
                .getMembers()
                .forEach(u -> mentions.put(u.getAsMention(), new DiscordMentionData(u)));

        message.getMentions()
                .getRoles()
                .forEach(r -> mentions.put(r.getAsMention(), new DiscordMentionData(r)));

        message.getMentions()
                .getChannels()
                .forEach(c -> mentions.put(c.getAsMention(), new DiscordMentionData(c)));

        return mentions;
    }

    public static Map<String, DiscordMentionData> collectEmbedMentions(MessageEmbed embed, Guild guild) {
        Map<String, DiscordMentionData> mentions = new HashMap<>();
        if (guild == null)
            return mentions;

        if (embed.getTitle() != null)
            collectMentionsFromText(embed.getTitle(), guild, mentions);

        if (embed.getDescription() != null)
            collectMentionsFromText(embed.getDescription(), guild, mentions);

        if (embed.getAuthor() != null && embed.getAuthor().getName() != null)
            collectMentionsFromText(embed.getAuthor().getName(), guild, mentions);

        if (embed.getFooter() != null && embed.getFooter().getText() != null)
            collectMentionsFromText(embed.getFooter().getText(), guild, mentions);

        List<MessageEmbed.Field> fields = embed.getFields();
        for (MessageEmbed.Field field : fields) {
            if (field.getName() != null)
                collectMentionsFromText(field.getName(), guild, mentions);
            if (field.getValue() != null)
                collectMentionsFromText(field.getValue(), guild, mentions);
        }

        return mentions;
    }

    private static void collectMentionsFromText(String text, Guild guild, Map<String, DiscordMentionData> mentions) {
        Matcher matcher = MENTION_PATTERN.matcher(text);

        while (matcher.find()) {
            String rawMention = matcher.group(0);
            if (mentions.containsKey(rawMention))
                continue;

            String marker = matcher.group(1);
            long id;
            try {
                id = Long.parseLong(matcher.group(2));
            } catch (NumberFormatException ignored) {
                continue;
            }

            DiscordMentionData mentionData = switch (marker) {
                case "@", "@!" -> resolveMember(guild, id);
                case "@&" -> resolveRole(guild, id);
                case "#" -> resolveChannel(guild, id);
                default -> null;
            };

            if (mentionData != null)
                mentions.put(rawMention, mentionData);
        }
    }

    private static DiscordMentionData resolveMember(Guild guild, long id) {
        Member member = guild.getMemberById(id);
        return member != null ? new DiscordMentionData(member) : null;
    }

    private static DiscordMentionData resolveRole(Guild guild, long id) {
        Role role = guild.getRoleById(id);
        return role != null ? new DiscordMentionData(role) : null;
    }

    private static DiscordMentionData resolveChannel(Guild guild, long id) {
        GuildChannel channel = guild.getGuildChannelById(id);
        return channel != null ? new DiscordMentionData(channel) : null;
    }
}