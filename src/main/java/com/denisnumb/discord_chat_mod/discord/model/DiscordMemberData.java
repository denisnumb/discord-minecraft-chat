package com.denisnumb.discord_chat_mod.discord.model;

import com.denisnumb.discord_chat_mod.ColorUtils;

import java.awt.*;

public class DiscordMemberData {
    public String guildNickname;
    public String discordNickName;
    public String discordName;
    public String prettyMention;
    public String mentionString;
    public String color;

    public DiscordMemberData(
            String guildNickname,
            String discordNickName,
            String discordName,
            String mentionString,
            Color color
    ){
        this.guildNickname = guildNickname;
        this.discordNickName = discordNickName;
        this.discordName = discordName;
        this.prettyMention = "@" + guildNickname;
        this.mentionString = mentionString;
        this.color = ColorUtils.getHexColor(color);
    }

    public String getPrettyMention() {
        return prettyMention;
    }

    public String getGuildNickname(){
        return guildNickname;
    }
}
