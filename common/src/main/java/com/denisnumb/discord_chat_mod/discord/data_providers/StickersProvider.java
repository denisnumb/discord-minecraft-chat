package com.denisnumb.discord_chat_mod.discord.data_providers;

import com.denisnumb.discord_chat_mod.chat_images.ImageStorage;
import com.denisnumb.discord_chat_mod.chat_images.model.AbstractImage;
import com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry;
import com.denisnumb.discord_chat_mod.discord.model.DiscordGuildContext;
import net.dv8tion.jda.api.entities.sticker.GuildSticker;

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

public final class StickersProvider {
    private StickersProvider() {}

    public record StickerData(String imageUrl, String discordId, String originalName) {}
    public static Map<String, AbstractImage> CLIENT_STICKER_CACHE = new HashMap<>();
    private static long lastGetNameToRawStickerMapGet = 0;
    private static Map<String, StickerData> cachedNameToRawStickerDataMap;

    public static void dropTimeouts(){
        lastGetNameToRawStickerMapGet = 0;
    }

    public static Map<String, String> getNameToUrlMap() {
        return getNameToStickerDataMap().entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().imageUrl
                ));
    }

    public static Map<String, StickerData> getNameToStickerDataMap() {
        if (!isDiscordConnected())
            return Map.of();

        if (System.currentTimeMillis() - lastGetNameToRawStickerMapGet < 300000)
            return cachedNameToRawStickerDataMap;
        lastGetNameToRawStickerMapGet = System.currentTimeMillis();

        return cachedNameToRawStickerDataMap = DiscordChannelRegistry.getAllContexts()
                .stream()
                .map(DiscordGuildContext::getGuild)
                .flatMap(guild -> guild.getStickers().stream())
                .collect(Collectors.groupingBy(GuildSticker::getName))
                .entrySet()
                .stream()
                .flatMap(entry -> {
                    String baseName = entry.getKey();
                    List<GuildSticker> sortedStickers = entry.getValue()
                            .stream()
                            .sorted(Comparator.comparing(GuildSticker::getTimeCreated))
                            .toList();

                    return IntStream.range(0, sortedStickers.size())
                            .mapToObj(i -> {
                                GuildSticker sticker = sortedStickers.get(i);
                                String guildName = sticker.getGuild() == null ? "" : String.format("(%s)", sticker.getGuild().getName());
                                String nameWithIndex = i == 0 ? baseName : String.format("%s %d", baseName, i);
                                String name = String.format("%s %s", nameWithIndex, guildName);
                                return Map.entry(name, new StickerData(sticker.getIconUrl(), sticker.getId(), sticker.getName()));
                            });
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    public static void loadClient(Map<String, String> rawStickers){
         if (!rawStickers.isEmpty())
            CLIENT_STICKER_CACHE.clear();

        ExecutorService executor = Executors.newFixedThreadPool(10);

        rawStickers.forEach((name, url) -> {
            if (!CLIENT_STICKER_CACHE.containsKey(name)){
                executor.submit(() -> {
                    AbstractImage image = ImageStorage.parseEmojiOrSticker(url);
                    if (image != null)
                        CLIENT_STICKER_CACHE.putIfAbsent(name, image);
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
