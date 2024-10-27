package com.denisnumb.discord_chat_mod.markdown;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import javax.annotation.Nullable;
import java.awt.*;

public class DiscordMentionData{
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
        this(channel.getName(), "#" + channel.getName(), "#6974c9");
    }

    private DiscordMentionData(String name, String prettyMention, String color){
        this.name = name;
        this.prettyMention = prettyMention;
        this.color = color;
    }

    public static String getHexColor(@Nullable Color color){
        if (color == null)
            return "#ffffff";
        return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }
}
