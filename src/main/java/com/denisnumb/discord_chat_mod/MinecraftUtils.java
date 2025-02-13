package com.denisnumb.discord_chat_mod;

import com.denisnumb.discord_chat_mod.markdown.tellraw.TellRawComponent;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.locating.IModFile;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.denisnumb.discord_chat_mod.ColorUtils.Color.*;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.ColorUtils.getHexColor;
import static com.denisnumb.discord_chat_mod.discord.DiscordUtils.prepareTellRawCommand;

public class MinecraftUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    public static void executeServerCommand(String command) {
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    public static void logErrorToServer(String message) {
        LOGGER.error(message);
        executeServerCommand(buildLogMessageCommand(message, RED));
    }

    private static String buildLogMessageCommand(String message, int color) {
        String hexColor = getHexColor(color);
        ArrayList<TellRawComponent> components = new ArrayList<>() {{
           add(new TellRawComponent("[discord_chat_mod] ").setBold().setColor(hexColor));
           add(new TellRawComponent(message).setColor(hexColor));
        }};

        return prepareTellRawCommand(components);
    }

    public static String getTranslateClient(String key, String defaultValue){
        return Language.getInstance().getLanguageData().getOrDefault(key, defaultValue);
    }

    public static String getTranslateClient(String key){
        return Language.getInstance().getLanguageData().get(key);
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

        String resourcePath = String.format("/assets/%s/lang/%s.json", namespace, locale);
        try {
            IModFile modFile = ModList.get().getModFileById(namespace).getFile();
            Path path = modFile.findResource(resourcePath);
            localeStorage.put(namespace, new Gson().fromJson(Files.readString(path), new TypeToken<Map<String, String>>(){}.getType()));
        } catch (Exception e) {
            LOGGER.error("Failed to load localization {}", resourcePath);
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

    @Nullable
    public static JsonObject getAdvancementFile(ResourceLocation resourceLocation)
    {
        try {
            Optional<Resource> resource = server.getResourceManager().getResource(resourceLocation);
            InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8);
            return GSON.fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            LOGGER.error("AdvancementFileNotFound: {}", resourceLocation);
            e.printStackTrace();
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

            return !item.isEmpty() && item.has(DataComponents.CUSTOM_NAME)
                    ? String.format(getTranslate(attackBase + ".item"), diedEntityName, killerEntity, item.getDisplayName().getString())
                    : String.format(getTranslate(attackBase), diedEntityName, killerEntity);
        }
    }
}
