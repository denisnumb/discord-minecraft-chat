package com.denisnumb.discord_chat_mod.discord.model;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import static com.denisnumb.discord_chat_mod.ColorUtils.Color.CHANNEL_MENTION_COLOR;
import static com.denisnumb.discord_chat_mod.ColorUtils.getHexColor;

public class DiscordMentionData {
    public String name;
    public String prettyMention;
    public String color;

    public DiscordMentionData(Member member){
        this(member.getEffectiveName(), "@" + member.getEffectiveName(), getHexColor(member.getColor()));
    }

    public DiscordMentionData(Role role){
        this(role.getName(), "@" + role.getName(), getHexColor(role.getColor()));
    }

    public DiscordMentionData(GuildChannel channel){
        this(channel.getName(), "#" + channel.getName(), getHexColor(CHANNEL_MENTION_COLOR));
    }

    public DiscordMentionData(String name, String prettyMention, String color){
        this.name = name;
        this.prettyMention = prettyMention;
        this.color = color;
    }
}
