package com.denisnumb.discord_chat_mod;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.utils.FileUpload;

import java.io.File;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;

public class DiscordUtils {
    public static void sendEmbedMessage(String text, int color){
        if (!isDiscordConnected() || !isServerStarted())
            return;

        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setDescription(text);
        embedBuilder.setColor(color);
        discordChannel.sendMessageEmbeds(embedBuilder.build()).queue();
    }
}
