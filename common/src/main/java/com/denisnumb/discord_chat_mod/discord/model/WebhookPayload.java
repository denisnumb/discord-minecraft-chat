package com.denisnumb.discord_chat_mod.discord.model;

import com.google.gson.annotations.SerializedName;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class WebhookPayload {
    public String username;
    @SerializedName("avatar_url")
    public String avatarUrl;
    public List<WebhookEmbed> embeds;
    public String content;

    public record EmbedImage(String url) {}
    public record EmbedAuthor(String name, String url, String icon_url) {}
    public record EmbedThumbnail(String url) {}
    public record EmbedFooter(String text, String icon_url) {}
    public record EmbedField(String name, String value, Boolean inline) {}

    public record WebhookEmbed(
            String title,
            String description,
            String url,
            int color,
            EmbedAuthor author,
            EmbedThumbnail thumbnail,
            EmbedImage image,
            List<EmbedField> fields,
            EmbedFooter footer,
            String timestamp
    ) {}

    public record WebhookAttachment(byte[] data, String fileName) {}

    public WebhookPayload(String content){
        this(content, null);
    }

    public WebhookPayload(MessageEmbed embed){
        this(null, embed);
    }

    public WebhookPayload(String content, MessageEmbed embed){
        if (content != null)
            content = content.isEmpty() ? null : content;
        this.content = content;
        if (embed != null){
            int color = embed.getColorRaw() == 536870911 ? 3881793 : embed.getColorRaw();
            EmbedImage image = embed.getImage() == null ? null : new EmbedImage(embed.getImage().getUrl());
            MessageEmbed.AuthorInfo authorInfo = embed.getAuthor();
            EmbedAuthor author = authorInfo == null ? null : new EmbedAuthor(authorInfo.getName(), authorInfo.getUrl(), authorInfo.getIconUrl());
            EmbedThumbnail thumbnail = embed.getThumbnail() == null ? null : new EmbedThumbnail(embed.getThumbnail().getUrl());
            List<EmbedField> embedFields = embed.getFields().stream().map(f -> new EmbedField(f.getName(), f.getValue(), f.isInline())).toList();
            EmbedFooter footer = embed.getFooter() == null ? null : new EmbedFooter(embed.getFooter().getText(), embed.getFooter().getIconUrl());

            this.embeds = List.of(new WebhookEmbed(
                    embed.getTitle(),
                    embed.getDescription(),
                    embed.getUrl(),
                    color,
                    author,
                    thumbnail,
                    image,
                    embedFields,
                    footer,
                    embed.getTimestamp() == null ? null : embed.getTimestamp().format(DateTimeFormatter.ISO_INSTANT)
            ));
        }
    }

    public WebhookPayload withUsername(String username){
        this.username = username;
        return this;
    }

    public WebhookPayload withAvatarUrl(String avatarUrl){
        this.avatarUrl = avatarUrl;
        return this;
    }
}
