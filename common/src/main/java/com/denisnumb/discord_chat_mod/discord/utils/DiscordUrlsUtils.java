package com.denisnumb.discord_chat_mod.discord.utils;

import com.denisnumb.discord_chat_mod.markdown.MarkdownPattern;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class DiscordUrlsUtils {
    private DiscordUrlsUtils() {}

    /**
     * Only retrieve embed URLs for Discord attachment links.
     * Don't retrieve embed URLs for other hosts as they may:
     * <ul>
     *     <li>Point media types that are incompatible with this mod, such as Tenor embed URLs pointing to video files.</li>
     *     <li>Be "incorrect", such as an embed URL for a social media post being a link to the post's image, causing the link to the post itself being overridden.</li>
     * </ul>
     */
    private static final Set<String> DISCORD_HOSTS = Set.of("cdn.discordapp.com", "media.discordapp.net");

    /**
     * Retrieves refreshed Discord attachment links asynchronously via message embeds.
     *
     * @param message The message to retrieve the refreshed URLs from.
     *
     * @return A future that resolves to a mapping of URLs in the message text to the corresponding refreshed URL.
     */
    public static @NotNull CompletableFuture<Map<String, String>> retrieveMessageEmbedUrls(Message message) {
        return retrieveMessageEmbedUrls(message, 3);
    }

    private static @NotNull CompletableFuture<Map<String, String>> retrieveMessageEmbedUrls(Message message, int retries) {
        Map<String, String> embedUrls = getMessageEmbedUrls(message);
        try {
            /*
             * Links that have been sent for the first time may not have embeds generated yet,
             * so refetch the message a few times to get the embeds.
             * Only refetch the message if it contains URLs.
             */
            if (retries > 0 && embedUrls.isEmpty() && hasUrls(message.getContentRaw())) {
                return message.getChannel()
                        .retrieveMessageById(message.getId())
                        .submit()
                        .exceptionally(ignored -> message)
                        .thenCompose(refetchedMessage -> retrieveMessageEmbedUrls(refetchedMessage, retries - 1));
            }
        } catch (Exception ignored) {
        }
        return CompletableFuture.completedFuture(embedUrls);
    }

    private static @NotNull Map<String, String> getMessageEmbedUrls(Message message) {
        return message.getEmbeds()
                .stream()
                .map((embed) -> {
                    String url = embed.getUrl();
                    if (url == null)
                        return null;
                    String mediaUrl = getMessageEmbedMediaUrl(embed);
                    if (mediaUrl == null)
                        return null;
                    URI uri = URI.create(url);
                    if (!DISCORD_HOSTS.contains(uri.getHost()))
                        return null;
                    return Map.entry(url, mediaUrl);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static @Nullable String getMessageEmbedMediaUrl(MessageEmbed embed) {
        if (embed.getImage() != null && embed.getImage().getUrl() != null)
            return embed.getImage().getUrl();
        else if (embed.getVideoInfo() != null && embed.getVideoInfo().getUrl() != null)
            return embed.getVideoInfo().getUrl();
        else if (embed.getThumbnail() != null && embed.getThumbnail().getUrl() != null)
            return embed.getThumbnail().getUrl();
        return null;
    }

    /**
     * Checks if the {@code text} contains any Discord CDN URLs.
     */
    private static boolean hasUrls(String text) {
        return MarkdownPattern.URL.matcher(text).results().anyMatch(matchResult -> {
            URI uri = URI.create(matchResult.group());
            return DISCORD_HOSTS.contains(uri.getHost());
        });
    }
}
