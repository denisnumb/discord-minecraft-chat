package com.denisnumb.discord_chat_mod.utils;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class JavaUtils {
    private JavaUtils() {}

    @SafeVarargs
    public static <T> Map<String, T> mergeMaps(Map<String, T>... parameterMaps){
        return Arrays.stream(parameterMaps)
                .flatMap(m -> m.entrySet().stream())
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }

    @SafeVarargs
    public static <K, V> LinkedHashMap<K, V> newLinkedHashMapOf(Map.Entry<? extends K, ? extends V>... entries) {
        LinkedHashMap<K, V> map = new LinkedHashMap<>(entries.length);
        for (Map.Entry<? extends K, ? extends V> entry : entries) {
            map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    public static <T> T nullSafeElse(T first, T second) {
        return first != null ? first : second;
    }

    public static OffsetDateTime getDateTimeWithUtcOffset(int offsetHours){
        Instant nowUtc = Instant.now();
        ZoneOffset offset = ZoneOffset.ofHours(offsetHours);

        return nowUtc.atOffset(offset);
    }

    public static InputStream getInputStreamFromUrl(String url) throws IOException, URISyntaxException {
        HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                        + "AppleWebKit/537.36 (KHTML, like Gecko) "
                        + "Chrome/124.0.0.0 Safari/537.36");

        return conn.getInputStream();
    }

    @Nullable
    public static <K, V> V waitForLocalResource(Map<K, V> map, K key, long timeoutMillis, long pollIntervalMillis) {
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            synchronized (map) {
                if (map.containsKey(key)) {
                    return map.get(key);
                }
            }

            try {
                Thread.sleep(pollIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return null;
    }
}
