package com.denisnumb.discord_chat_mod.commands.set_avatar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class AvatarUrlStorage {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "discord_webhook_avatar_urls.json";
    private static Map<UUID, String> urlMap = new HashMap<>();

    public static void load(MinecraftServer server) {
        File file = getFile(server);
        if (file != null && file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<UUID, String>>() {}.getType();
                urlMap = GSON.fromJson(reader, type);
                if (urlMap == null)
                    urlMap = new HashMap<>();
            } catch (Exception e) {
                LOGGER.error("LoadAvatarUrlsError", e);
            }
        }
    }

    public static void save(MinecraftServer server) {
        File file = getFile(server);
        if (file == null)
            return;

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(urlMap, writer);
        } catch (Exception e) {
            LOGGER.error("SaveAvatarUrlsError", e);
        }
    }

    public static void unload(){
        urlMap = new HashMap<>();
    }

    public static void setUrl(UUID uuid, String url, MinecraftServer server) {
        urlMap.put(uuid, url);
        save(server);
    }

    public static void removeUrl(UUID uuid, MinecraftServer server) {
        urlMap.remove(uuid);
        save(server);
    }

    @Nullable
    public static String getUrl(Player player) {
        return urlMap.getOrDefault(player.getUUID(), null);
    }

    @Nullable
    private static File getFile(MinecraftServer server) {
        try {
            return server.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME).toFile();
        } catch (Exception ignored) {
            return null;
        }
    }
}
