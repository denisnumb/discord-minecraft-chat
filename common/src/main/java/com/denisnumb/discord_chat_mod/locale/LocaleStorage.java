package com.denisnumb.discord_chat_mod.locale;

import com.denisnumb.discord_chat_mod.DiscordChatMod;
import com.denisnumb.discord_chat_mod.utils.JavaUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.locale.Language;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public final class LocaleStorage {
    private LocaleStorage() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<String, String>>(){}.getType();
    private static final Map<String, String> LANGUAGE_DATA = new HashMap<>();
    private static final String CACHE_DIR_NAME = "locale_cache";
    private static final String BASE_GITHUB_URL = "https://raw.githubusercontent.com/denisnumb/discord-chat-mod/1.21.11/data/minecraft_locales/";

    public interface LocaleLoader {
        void loadLocalization();
    }
    private static LocaleLoader localeLoader;

    public static void setLocaleLoader(LocaleLoader impl){
        localeLoader = impl;
    }

    public static void loadLocalization() {
        if (localeLoader != null)
            localeLoader.loadLocalization();
    }

    static String getTranslate(String key) {
        return LANGUAGE_DATA.containsKey(key)
                ? LANGUAGE_DATA.get(key)
                : Language.getInstance().getOrDefault(key);
    }

    public static void loadMinecraftLocale(String locale){
        if (loadLocaleFromPath(getCachedFile(locale)))
            return;

        String url = String.format(BASE_GITHUB_URL + locale + ".json");
        try (Reader reader = new InputStreamReader(JavaUtils.getInputStreamFromUrl(url), StandardCharsets.UTF_8)) {
            Map<String, String> data = GSON.fromJson(reader, TYPE);
            saveMinecraftLocaleToCache(locale, data);
            addLanguageData(data);
        } catch (Exception e) {
            LOGGER.warn("Failed to load minecraft localization from {}", url, e);
        }
    }

    public static void loadLocaleFromResource(@Nullable String localeResourcePath){
        try{
            InputStream localeInputStream = getLocaleInputStream(localeResourcePath);
            if (localeInputStream != null){
                addLanguageData(GSON.fromJson(new String(localeInputStream.readAllBytes()), TYPE));
            }
        } catch (Exception e){
            LOGGER.warn("Failed to load localization {}", localeResourcePath);
        }
    }

    private static boolean loadLocaleFromPath(@Nullable Path localePath){
        try{
            if (localePath != null && Files.exists(localePath)){
                addLanguageData(GSON.fromJson(Files.readString(localePath), TYPE));
                return true;
            }
        } catch (Exception e){
            LOGGER.warn("Failed to load localization {}", localePath);
        }

        return false;
    }

    @Nullable
    private static InputStream getLocaleInputStream(String pathToLocale) throws IllegalStateException {
        InputStream input = DiscordChatMod.class.getResourceAsStream(pathToLocale);
        if (input != null)
            return input;

        return DiscordChatMod.class.getClassLoader().getResourceAsStream(pathToLocale);
    }

    private static void addLanguageData(Map<String, String> languageData){
        LANGUAGE_DATA.putAll(languageData);
    }

    @Nullable
    private static Path getCachedFile(String locale){
        Path cacheDir = Paths.get(CACHE_DIR_NAME);
        if (!Files.exists(cacheDir)) {
            try {
                Files.createDirectories(cacheDir);
            } catch (IOException e) {
                return null;
            }
        }

        return cacheDir.resolve(locale + ".json");
    }

    private static void saveMinecraftLocaleToCache(String locale, Map<String, String> data){
        Path cachedFile = getCachedFile(locale);

        if (cachedFile != null){
            if (Files.exists(cachedFile))
                return;
            try {
                Files.writeString(cachedFile, GSON.toJson(data), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException ignored) {}
        }
    }
}
