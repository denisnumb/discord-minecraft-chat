package com.denisnumb.discord_chat_mod.discord.data_providers;

import com.denisnumb.discord_chat_mod.chat_images.ImageStorage;
import com.denisnumb.discord_chat_mod.chat_images.model.AbstractImage;
import com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry;
import com.denisnumb.discord_chat_mod.discord.model.DiscordGuildContext;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.isDiscordConnected;

public final class CustomEmojiProvider {
    private CustomEmojiProvider() {}

    public record EmojiData(String url, String mentionString) { }
    public static Map<String, AbstractImage> CLIENT_EMOJI_CACHE = new HashMap<>();
    private static long lastGetEmojiData = 0;
    private static Map<String, EmojiData> cachedEmojiData;

    public static void dropTimeouts(){
        lastGetEmojiData = 0;
    }

    public static Map<String, String> getNameToUrlMap() {
        return getNameToEmojiDataMap().entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().url()
                ));
    }

    public static Map<String, EmojiData> getNameToEmojiDataMap() {
        if (!isDiscordConnected())
            return Map.of();

        if (System.currentTimeMillis() - lastGetEmojiData < 300000)
            return cachedEmojiData;
        lastGetEmojiData = System.currentTimeMillis();

        return cachedEmojiData = DiscordChannelRegistry.getAllContexts()
                .stream()
                .map(DiscordGuildContext::getGuild)
                .flatMap(guild -> guild.getEmojis().stream())
                .collect(Collectors.groupingBy(RichCustomEmoji::getName))
                .entrySet()
                .stream()
                .flatMap(entry -> {
                    String baseName = entry.getKey();
                    List<RichCustomEmoji> sortedEmojis = entry.getValue()
                            .stream()
                            .sorted(Comparator.comparing(RichCustomEmoji::getTimeCreated))
                            .toList();

                    return IntStream.range(0, sortedEmojis.size())
                            .mapToObj(i -> {
                                RichCustomEmoji emoji = sortedEmojis.get(i);
                                String name = i == 0 ? baseName : baseName + "~" + i;
                                return Map.entry(name, new EmojiData(emoji.getImageUrl(), String.format("<:%s:%s>", baseName, emoji.getId())));
                            });
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    public static void loadClient(Map<String, String> rawEmojis){
        if (!rawEmojis.isEmpty())
            CLIENT_EMOJI_CACHE.clear();

        ExecutorService executor = Executors.newFixedThreadPool(10);

        rawEmojis.forEach((name, url) -> {
            if (!CLIENT_EMOJI_CACHE.containsKey(name)){
                executor.submit(() -> {
                    AbstractImage image = ImageStorage.parseEmojiOrSticker(url);
                    if (image != null)
                        CLIENT_EMOJI_CACHE.putIfAbsent(name, image);
                });
            }
        });
        executor.shutdown();

        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        monitor.execute(() -> {
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                monitor.shutdown();
            }
        });
    }
}
