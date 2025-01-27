package com.denisnumb.discord_chat_mod;

import com.denisnumb.discord_chat_mod.markdown.tellraw.TellRawComponent;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.denisnumb.discord_chat_mod.ColorUtils.Color.*;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.ColorUtils.getHexColor;
import static com.denisnumb.discord_chat_mod.discord.DiscordUtils.prepareTellRawCommand;
import static net.minecraft.util.datafix.fixes.BlockEntitySignTextStrictJsonFix.GSON;

public class MinecraftUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static void executeServerCommand(String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    public static void logInfoToServer(String message) {
        LOGGER.info(message);
        executeServerCommand(buildLogMessageCommand(message, WHITE));
    }

    public static void logErrorToServer(String message) {
        LOGGER.error(message);
        executeServerCommand(buildLogMessageCommand(message, RED));
    }

    public static void logWarningToServer(String message) {
        LOGGER.warn(message);
        executeServerCommand(buildLogMessageCommand(message, GOLD));
    }

    private static String buildLogMessageCommand(String message, int color) {
        String hexColor = getHexColor(color);
        ArrayList<TellRawComponent> components = new ArrayList<>() {{
           add(new TellRawComponent("[discord_chat_mod] ").setBold().setColor(hexColor));
           add(new TellRawComponent(message).setColor(hexColor));
        }};

        return prepareTellRawCommand(components);
    }

    public static String getTranslate(String namespace, String key, String defaultValue) {
        return getLocalization(namespace, Config.modLocale).getOrDefault(key, defaultValue);
    }

    public static String getTranslate(String key, String defaultValue) {
        return getLocalization(MODID, Config.modLocale).getOrDefault(key, defaultValue);
    }

    public static String getTranslate(String key) {
        return getLocalization(MODID, Config.modLocale).get(key);
    }

    public static Map<String, String> getLocalization(String namespace, String locale)
    {
        if (namespace.equals("minecraft"))
            namespace = MODID;

        if (localeStorage.containsKey(namespace))
            return localeStorage.get(namespace);

        if (namespace.equals(MODID) && locale.equals("en_us")){
            localeStorage.put(namespace, Language.getInstance().getLanguageData());
            return getLocalization(namespace, locale);
        }

        try {
            ResourceLocation resourceLocation = new ResourceLocation(namespace, String.format("lang/%s.json", locale));
            Optional<Resource> resource = server.getResourceManager().getResource(resourceLocation);
            InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8);
            localeStorage.put(namespace, GSON.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType()));
        } catch (Exception e) {
            logErrorToServer(String.format("Failed to load localization %s", namespace + "/" + String.format("lang/%s.json", locale)));
            if (namespace.equals(MODID))
                return getLocalization(namespace, "en_us");
            localeStorage.put(namespace, Collections.emptyMap());
        }
        return getLocalization(namespace, locale);
    }

    public static String getAdvancementField(JsonObject jsonObject, String key) {
        if (jsonObject.has("display")) {
            JsonObject display = jsonObject.getAsJsonObject("display");
            if (display.has(key)) {
                JsonObject description = display.getAsJsonObject(key);
                if (description.has("translate")) {
                    return description.get("translate").getAsString();
                }
            }
        }
        return null;
    }

    public static JsonObject getAdvancementFile(ResourceLocation resourceLocation)
    {
        try {
            Optional<Resource> resource = server.getResourceManager().getResource(resourceLocation);
            InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8);
            return GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            return null;
        }
    }

    public static String getLocalizedDeathMessage(DamageSource source, LivingEntity diedEntity) {
        String diedEntityName = diedEntity.getDisplayName().getString();
        String attackBase = "death.attack." + source.type().msgId();

        if (source.getEntity() == null && source.getDirectEntity() == null) {
            LivingEntity playerKiller = diedEntity.getKillCredit();
            String byPlayer = attackBase + ".player";

            return playerKiller != null
                    ? String.format(getTranslate(byPlayer), diedEntityName, playerKiller.getDisplayName().getString())
                    : String.format(getTranslate(attackBase), diedEntityName);
        } else {
            String killerEntity = source.getEntity() == null
                    ? getTranslate(source.getDirectEntity().getType().getDescriptionId())
                    : getTranslate(source.getEntity().getType().getDescriptionId());

            Entity entity = source.getEntity();
            ItemStack item = (entity instanceof LivingEntity)
                    ? ((LivingEntity)entity).getMainHandItem()
                    : ItemStack.EMPTY;

            return !item.isEmpty() && item.hasCustomHoverName()
                    ? String.format(getTranslate(attackBase + ".item"), diedEntityName, killerEntity, item.getDisplayName().getString())
                    : String.format(getTranslate(attackBase), diedEntityName, killerEntity);
        }
    }
}
