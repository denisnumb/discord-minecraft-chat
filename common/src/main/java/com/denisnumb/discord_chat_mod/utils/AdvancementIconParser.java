package com.denisnumb.discord_chat_mod.utils;

import com.denisnumb.discord_chat_mod.DiscordChatMod;
import com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils;
import com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Optional;

public final class AdvancementIconParser {
    private AdvancementIconParser() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private static String resolveItemTexture(String namespace, String name) throws IOException {
        return resolveModelTexture(String.format("/assets/%s/models/item/%s.json", namespace, name), namespace, name);
    }

    private static String resolveModelTexture(String modelPath, String namespace, String defaultName) throws IOException {
        try (InputStream input = getResourceAsStream(modelPath)) {
            if (input == null)
                return defaultName;

            JsonObject modelJson = GSON.fromJson(new InputStreamReader(input), JsonObject.class);

            if (modelJson.has("textures")) {
                JsonObject textures = modelJson.getAsJsonObject("textures");
                for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
                    String tex = entry.getValue().getAsString();
                    if (!tex.startsWith("#")) {
                        String[] parts = tex.split("/");
                        return parts[parts.length - 1];
                    }
                }
            }

            if (modelJson.has("parent")) {
                String parent = modelJson.get("parent").getAsString();
                if (!parent.contains(":")) parent = namespace + ":" + parent;
                String[] parts = parent.split(":");
                String parentNamespace = parts[0];
                String parentPath = parts[1];
                return resolveModelTexture(String.format("/assets/%s/models/%s.json", parentNamespace, parentPath), namespace, defaultName);
            }
        }

        return defaultName;
    }

    public static Optional<DiscordMessageUtils.ImageData> parseAdvancementIcon(DisplayInfo displayInfo) {
        try {
            ItemStack stack = displayInfo.getIcon();
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String namespace = id.getNamespace();
            String name = id.getPath();

            if (namespace.equals("minecraft"))
                namespace = DiscordChatMod.MOD_ID;

            String textureName = resolveItemTexture(namespace, name);

            if (textureName.equals(name))
                textureName = resolveModelTexture(String.format("/assets/%s/models/block/%s.json", namespace, name), namespace, name);

            try (InputStream input = getResourceAsStream(String.format("/assets/%s/textures/item/%s.png", namespace, textureName))) {
                if (input != null)
                    return Optional.of(new DiscordMessageUtils.ImageData("icon.png", ImageUtils.scaleImage(input.readAllBytes(), 3)));
            }

            try (InputStream input = getResourceAsStream(String.format("/assets/%s/textures/block/%s.png", namespace, textureName))) {
                if (input != null)
                    return Optional.of(new DiscordMessageUtils.ImageData("icon.png", ImageUtils.scaleImage(input.readAllBytes(), 3)));
            }

        } catch (Exception e) {
            LOGGER.error("AdvancementIconParseError [{}]: {}", displayInfo.getIcon().getItem().getDescriptionId(), e.getMessage());
        }

        return Optional.empty();
    }

    @Nullable
    private static InputStream getResourceAsStream(String path)  {
        InputStream input = DiscordChatMod.class.getResourceAsStream(path);
        if (input != null)
            return input;

        return DiscordChatMod.class.getClassLoader().getResourceAsStream(path);
    }
}
