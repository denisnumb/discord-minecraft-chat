package com.denisnumb.discord_chat_mod.discord.model;

import com.denisnumb.discord_chat_mod.utils.ColorUtils;
import net.dv8tion.jda.api.entities.RoleColors;

public class DiscordUserData {
    public String displayName;
    public String discordName;
    public String prettyMention;
    public String mentionString;
    public int[] colors;

    public DiscordUserData(
            String displayName,
            String discordName,
            String mentionString,
            RoleColors colors
    ){
        this.displayName = displayName;
        this.discordName = discordName;
        this.prettyMention = "@" + displayName;
        this.mentionString = mentionString;
        this.colors = ColorUtils.parseRoleColors(colors);
    }

    public String getPrettyMention() {
        return prettyMention;
    }
}
