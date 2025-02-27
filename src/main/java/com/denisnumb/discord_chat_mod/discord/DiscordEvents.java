package com.denisnumb.discord_chat_mod.discord;

import com.denisnumb.discord_chat_mod.Config;
import com.denisnumb.discord_chat_mod.discord.model.DiscordMentionData;
import com.denisnumb.discord_chat_mod.markdown.MarkdownParser;
import com.denisnumb.discord_chat_mod.markdown.MarkdownTellRawConverter;
import com.denisnumb.discord_chat_mod.markdown.tellraw.TellRawComponent;
import com.denisnumb.discord_chat_mod.markdown.tellraw.TellRawComponentEvent;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.denisnumb.discord_chat_mod.ColorUtils.Color.*;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.ColorUtils.getHexColor;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.getTranslate;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.STICKER;
import static com.denisnumb.discord_chat_mod.discord.DiscordUtils.prepareTellRawCommand;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.executeServerCommand;

public class DiscordEvents extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().getChannelId().equals(Config.discordChannelId)
                || event.getAuthor().getId().equals(jda.getSelfUser().getId()))
            return;

        if (Config.logDiscordMessages)
            System.out.printf("[Discord] <%s> %s%n", event.getAuthor().getEffectiveName(), event.getMessage().getContentDisplay());

        if (server.getPlayerCount() == 0)
            return;

        for (String command : prepareTellRawCommands(event.getMessage()))
            executeServerCommand(command);
    }

    private static @NotNull List<String> prepareTellRawCommands(Message message){
        ArrayList<String> commands = new ArrayList<>();

        Member member = Objects.requireNonNull(message.getMember());
        String userName = member.getEffectiveName();
        String roleColor = getHexColor(member.getColor());

        ArrayList<TellRawComponent> basePart = new ArrayList<>() {{
            add(new TellRawComponent("[discord] ").setBold().setColor(getHexColor(DISCORD_COLOR)));
            add(new TellRawComponent("<"));
            add(new TellRawComponent(userName)
                    .setColor(roleColor)
                    .setInsertion("@" + userName)
                    .addClickEvent(new TellRawComponentEvent("suggest_command", "/mention " + userName))
                    .addHoverEvent(new TellRawComponentEvent("show_text", member.getUser().getEffectiveName())));
            add(new TellRawComponent("> "));
        }};

        if (!message.getContentRaw().isEmpty()){
            Map<String, DiscordMentionData> mentions = new HashMap<>();

            for (Member user : message.getMentions().getMembers())
                mentions.put(user.getAsMention(), new DiscordMentionData(user));
            for (Role role : message.getMentions().getRoles())
                mentions.put(role.getAsMention(), new DiscordMentionData(role));
            for (GuildChannel channel : message.getMentions().getChannels())
                mentions.put(channel.getAsMention(), new DiscordMentionData(channel));

            List<TellRawComponent> textPart;
            try {
                textPart = new MarkdownTellRawConverter(MarkdownParser.parseMarkdown(message.getContentRaw()), mentions).convertMarkdownTokensToTellRaw();
            } catch (Exception ignored) {
                String content = message.getContentRaw();
                for (var entry : mentions.entrySet())
                    content = content.replace(entry.getKey(), entry.getValue().prettyMention);
                textPart = List.of(new TellRawComponent(content));
            }

            commands.add(prepareTellRawCommand(basePart, textPart));
        }

        if (!message.getAttachments().isEmpty()){
            List<TellRawComponent> attachmentPart = new ArrayList<>() {{
                int index = 0;
                List<Message.Attachment> attachments = message.getAttachments();
                for (var file : attachments){
                    add(new TellRawComponent(file.getFileName() + (++index < attachments.size() ? "\n" : ""))
                            .setItalic()
                            .setColor(getHexColor(CHAT_LINK_COLOR))
                            .addClickEvent(new TellRawComponentEvent("open_url", file.getUrl()))
                            .addHoverEvent(new TellRawComponentEvent("show_text", file.getUrl())));
                }
            }};
            commands.add(prepareTellRawCommand(basePart, attachmentPart));
        }

        if (!message.getStickers().isEmpty()){
            List<TellRawComponent> stickerPart = new ArrayList<>() {{
                StickerItem sticker = message.getStickers().getFirst();
                add(new TellRawComponent(String.format(getTranslate(STICKER, "*sticker* (%s)"), sticker.getName()))
                        .setItalic()
                        .addClickEvent(new TellRawComponentEvent("open_url", sticker.getIconUrl()))
                );
            }};
            commands.add(prepareTellRawCommand(basePart, stickerPart));
        }

        return commands;
    }
}
