package com.denisnumb.discord_chat_mod.utils;

import com.denisnumb.discord_chat_mod.markdown.TellRawTextComponent;
import com.google.gson.Gson;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;

public class DiscordUtils {
    public static class Color{

        public static final int RED = 0xE74C3C;
        public static final int GREEN = 0x2ECC71;
        public static final int DARK_GREEN = 0x1F8B4C;
        public static final int DEFAULT = 0;
        public static final int GOLD = 0xF1C40F;
        public static final int WHITE = 0xFFFFFF;
        public static final int PURPLE = 0xA700A7;
    }

    private static final Gson gson = new Gson();

    public static Optional<Message> findPinnedStatusMessage(){
        return discordChannel.retrievePinnedMessages()
                .complete()
                .stream()
                .filter(message -> message.getAuthor().getId().equals(jda.getSelfUser().getId()))
                .findFirst();
    }

    public static MessageEmbed buildEmbed(String title, String description, int color){
        return new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .build();
    }

    public static MessageEmbed buildEmbed(String description, int color){
        return new EmbedBuilder()
                .setDescription(description)
                .setColor(color)
                .build();
    }

    public static Message sendShortEmbedMessage(String text, int color){
        if (!isDiscordConnected())
            return null;
        return discordChannel.sendMessageEmbeds(buildEmbed(text, color)).complete();
    }

    public static Message sendEmbedMessage(String title, String description, int color){
        if (!isDiscordConnected())
            return null;
        return discordChannel.sendMessageEmbeds(buildEmbed(title, description, color)).complete();
    }

    @SafeVarargs
    public static String prepareTellRawCommand(List<TellRawTextComponent>... parts){
        List<Object> commandJson = new ArrayList<>();
        commandJson.add("");
        for (var part : parts)
            commandJson.addAll(part);

        return "/tellraw @a " + gson.toJson(commandJson);
    }
}
