package com.denisnumb.discord_chat_mod;

import com.denisnumb.discord_chat_mod.markdown.MarkdownParser;
import com.denisnumb.discord_chat_mod.markdown.MarkdownTellRawConverter;
import com.denisnumb.discord_chat_mod.markdown.TellRawTextComponent;
import com.denisnumb.discord_chat_mod.markdown.TellRawTextComponentEvent;
import com.denisnumb.discord_chat_mod.markdown.DiscordMentionData;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.utils.DiscordUtils.prepareTellRawCommand;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.executeServerCommand;

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
        String roleColor = DiscordMentionData.getHexColor(member.getColor());

        ArrayList<TellRawTextComponent> basePart = new ArrayList<>() {{
            add(new TellRawTextComponent("[discord]").setBold().setColor("#F1C40F"));
            add(new TellRawTextComponent(" <"));
            add(new TellRawTextComponent(userName).setColor(roleColor));
            add(new TellRawTextComponent("> "));
        }};

        if (!message.getContentRaw().isEmpty()){
            Map<String, DiscordMentionData> mentions = new HashMap<>();

            for (Member user : message.getMentions().getMembers())
                mentions.put(user.getAsMention(), new DiscordMentionData(user));
            for (Role role : message.getMentions().getRoles())
                mentions.put(role.getAsMention(), new DiscordMentionData(role));
            for (GuildChannel channel : message.getMentions().getChannels())
                mentions.put(channel.getAsMention(), new DiscordMentionData(channel));

            List<TellRawTextComponent> textPart;
            try {
                textPart = new MarkdownTellRawConverter(MarkdownParser.parseMarkdown(message.getContentRaw()), mentions).convertMarkdownTokensToTellRaw();
            } catch (Exception ignored) {
                String content = message.getContentRaw();
                for (var entry : mentions.entrySet())
                    content = content.replace(entry.getKey(), entry.getValue().prettyMention);
                textPart = List.of(new TellRawTextComponent(content));
            }

            commands.add(prepareTellRawCommand(basePart, textPart));
        }

        if (!message.getAttachments().isEmpty()){
            List<TellRawTextComponent> attachmentPart = new ArrayList<>() {{
                int index = 0;
                List<Message.Attachment> attachments = message.getAttachments();
                for (var file : attachments){
                    add(new TellRawTextComponent(file.getFileName() + (++index < attachments.size() ? "\n" : ""))
                            .setItalic()
                            .setColor("aqua")
                            .addClickEvent(new TellRawTextComponentEvent("open_url", file.getUrl()))
                            .addHoverEvent(new TellRawTextComponentEvent("show_text", file.getUrl())));
                }
            }};
            commands.add(prepareTellRawCommand(basePart, attachmentPart));
        }

        if (!message.getStickers().isEmpty()){
            List<TellRawTextComponent> stickerPart = new ArrayList<>() {{
                add(new TellRawTextComponent(String.format("*sticker* (%s)", message.getStickers().get(0).getName())).setItalic());
            }};
            commands.add(prepareTellRawCommand(basePart, stickerPart));
        }

        return commands;
    }
}
