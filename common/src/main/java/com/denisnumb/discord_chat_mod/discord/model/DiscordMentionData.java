package com.denisnumb.discord_chat_mod.discord.model;

import com.denisnumb.discord_chat_mod.utils.ColorUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import static com.denisnumb.discord_chat_mod.utils.ColorUtils.Color.CHANNEL_MENTION_COLOR;
import static com.denisnumb.discord_chat_mod.discord.data_providers.ChannelMembersProvider.getMemberDisplayName;

public class DiscordMentionData {
    public DiscordUserData memberData;
    public String prettyMention;
    public int[] colors;

    public DiscordMentionData(Member member){
        this(new DiscordUserData(
                getMemberDisplayName(member),
                member.getUser().getName(),
                member.getAsMention(),
                member.getColors()
        ));
    }

    public DiscordMentionData(Role role){
        this("@" + role.getName(), ColorUtils.parseRoleColors(role.getColors()));
    }

    public DiscordMentionData(GuildChannel channel){
        this("#" + channel.getName(), new int[] { CHANNEL_MENTION_COLOR });
    }

    public DiscordMentionData(DiscordUserData memberData){
        this(memberData.prettyMention, memberData.colors);
        this.memberData = memberData;
    }

    public DiscordMentionData(String prettyMention, int[] colors){
        this.prettyMention = prettyMention;
        this.colors = colors;
    }
}
