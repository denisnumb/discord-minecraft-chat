package com.denisnumb.discord_chat_mod;

import com.denisnumb.discord_chat_mod.markdown.TellRawTextComponent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.awt.Color;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.DiscordUtils.Color.*;
import static com.denisnumb.discord_chat_mod.DiscordUtils.prepareTellRawCommand;
import static com.denisnumb.discord_chat_mod.markdown.DiscordMentionData.getHexColor;
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
        String hexColor = getHexColor(new Color(color));
        ArrayList<TellRawTextComponent> components = new ArrayList<>() {{
           add(new TellRawTextComponent("[discord_chat_mod] ").setBold().setColor(hexColor));
           add(new TellRawTextComponent(message).setColor(hexColor));
        }};

        return prepareTellRawCommand(components);
    }

    public static String getTranslate(String key) {
        return locale.get(key);
    }

    public static String getTranslate(String key, String defaultValue) {
        return locale.getOrDefault(key, defaultValue);
    }

    public static Map<String, String> loadLocalization(String localization) {
        if (localization.equals("en_us"))
            return Language.getInstance().getLanguageData();

        ResourceLocation resourceLocation = new ResourceLocation(MODID, String.format("lang/%s.json", localization));
        try {
            var resource = server.getResourceManager().getResource(resourceLocation);
            InputStreamReader reader = new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8);
            return GSON.fromJson(reader, new TypeToken<Map<String, String>>(){}.getType());
        } catch (Exception e) {
            logErrorToServer(String.format("Failed to load localization %s", resourceLocation));
            return Language.getInstance().getLanguageData();
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
