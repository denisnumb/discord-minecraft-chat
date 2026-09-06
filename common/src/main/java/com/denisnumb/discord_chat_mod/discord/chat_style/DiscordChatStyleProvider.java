package com.denisnumb.discord_chat_mod.discord.chat_style;

import com.denisnumb.discord_chat_mod.ColorUtils;
import com.denisnumb.discord_chat_mod.DeathMessageUtils;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.config.IConfigProvider;
import com.denisnumb.discord_chat_mod.discord.utils.WebhookUtils;
import com.denisnumb.discord_chat_mod.locale.DiscordLocaleProvider;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.denisnumb.discord_chat_mod.chat_style.ChatStyleUtils.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.Translatable.*;

public class DiscordChatStyleProvider {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();

    public record DiscordMessageComponents(Optional<String> content, Optional<MessageEmbed> embed) {
        public boolean hasContentAndEmbed(){
            return content.isPresent() && embed.isPresent();
        }

        public boolean hasNoEmbed(){
            return content.isPresent() && embed.isEmpty();
        }

        public @Nullable String getContent(){
            return content.orElse(null);
        }

        public @Nullable MessageEmbed getEmbed(){
            return embed.orElse(null);
        }
    }

    private static Optional<MessageEmbed> parseEmbedJson(JsonObject jsonTemplate){
        if (!jsonTemplate.has("embed"))
            return Optional.empty();

        JsonObject embed = jsonTemplate.getAsJsonObject("embed");

        if (embed.has("color") && embed.getAsJsonPrimitive("color").isString()){
            Integer parsedColor = ColorUtils.parseColor(embed.get("color").getAsString());
            if (parsedColor == null)
                embed.remove("color");
            else
                embed.addProperty("color", parsedColor);
        }

        return Optional.of(EmbedBuilder.fromData(DataObject.fromJson(GSON.toJson(embed))).build());
    }

    private static String escapeSpecialCharacters(String text){
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static DiscordMessageComponents parseDiscordConfigTemplate(String jsonTemplate, Map<String, String> parameterMap) {
        for (Map.Entry<String, String> param : parameterMap.entrySet())
            jsonTemplate = jsonTemplate.replace(param.getKey(), escapeSpecialCharacters(param.getValue()));

        JsonObject parsedTemplate = GSON.fromJson(jsonTemplate, JsonObject.class);

        Optional<String> content = parsedTemplate.has("content")
                ? Optional.of(parsedTemplate.get("content").getAsString())
                : Optional.empty();

        Optional<MessageEmbed> embed = parseEmbedJson(parsedTemplate);

        return new DiscordMessageComponents(content, embed);
    }

    public static String formatDeathMessageComponents(DeathMessageUtils.DeathMessageComponents components){
        IConfigProvider config = ConfigProvider.getConfig();

        String playerTemplate = config.discordPlayerDeathNameStyle().replace(PLAYER, getTranslatedComponent(components.diedEntity()));
        String killerTemplate = config.discordPlayerDeathSecondEntityStyle().replace(SECOND_ENTITY,
                components.killerEntity() == null ? "" : getTranslatedComponent(components.killerEntity())
        );
        String weaponTemplate = config.discordPlayerDeathWeaponStyle().replace(ITEM,
                components.item() == null ? "" : components.item().getString()
        );

        return String.format(
                config.discordPlayerDeathCauseStyle().replace(DEATH_CAUSE, DiscordLocaleProvider.getTranslate(components.deathCauseLocaleKey())),
                playerTemplate,
                killerTemplate,
                weaponTemplate
        );
    }

    private static String getTranslatedComponent(Component component){
        return component.getContents() instanceof TranslatableContents tc
                ? DiscordLocaleProvider.getTranslate(tc.getKey())
                : component.getString();
    }

    public static Map<String, String> buildPlayerParameters(CommandSourceStack source){
        return source.getPlayer() == null
                ? buildPlayerParameters(source.getDisplayName().getString(), null)
                : buildPlayerParameters(source.getDisplayName().getString(), source.getEntity());
    }

    public static Map<String, String> buildPlayerParameters(Entity entity){
        return buildPlayerParameters(entity.getDisplayName().getString(), entity);
    }

    public static Map<String, String> buildPlayerParameters(String displayName, Entity entity){
        HashMap<String, String> result = new HashMap<>();
        result.put(PLAYER, displayName);
        if (entity instanceof Player player)
            result.put(PLAYER_AVATAR_URL, WebhookUtils.getPlayerAvatarUrl(player));
        else
            result.put(PLAYER_AVATAR_URL, ConfigProvider.getConfig().webhookServerAvatarUrl());

        return mergeMaps(result, buildPositionParameters(entity));
    }

    public static Optional<DiscordMessageComponents> getDiscordMessageComponents(MessageType messageType, Map<String, String> parameterMap){
        IConfigProvider config = ConfigProvider.getConfig();
        OffsetDateTime now = getDateTimeWithUtcOffset();

        parameterMap = mergeMaps(
                parameterMap,
                Map.of(TIMESTAMP, String.valueOf(now.toEpochSecond())),
                Map.of(DATETIME, now.format(DateTimeFormatter.ISO_INSTANT))
        );

        try{
            DiscordMessageComponents result = switch (messageType){
                case PINNED_STATUS_AVAILABLE ->  parseDiscordConfigTemplate(
                        setConfigTemplateTranslatableParameters(config.discordPinnedStatusMessageServerAvailableStyle(), SERVER_AVAILABLE),
                        parameterMap
                );
                case PINNED_STATUS_UNAVAILABLE ->  parseDiscordConfigTemplate(
                        setConfigTemplateTranslatableParameters(config.discordPinnedStatusMessageServerUnavailableStyle(), SERVER_UNAVAILABLE),
                        parameterMap
                );
                case PINNED_STATUS_PLAYERS ->  parseDiscordConfigTemplate(config.discordPinnedStatusMessageStyle(), parameterMap);
                case SERVER_START -> parseDiscordConfigTemplate(
                        setConfigTemplateTranslatableParameters(config.discordServerStartedMessageStyle(), SERVER_STARTED),
                        parameterMap
                );
                case LOCAL_SERVER_START -> parseDiscordConfigTemplate(config.discordLocalServerStartedMessageStyle(), parameterMap);
                case SERVER_STOP -> parseDiscordConfigTemplate(
                        setConfigTemplateTranslatableParameters(config.discordServerClosedMessageStyle(), SERVER_CLOSED),
                        parameterMap
                );
                case CHAT -> parseDiscordConfigTemplate(config.discordPlayerMessageStyle(), parameterMap);
                case CHAT_WEBHOOK -> parseDiscordConfigTemplate(config.discordPlayerMessageWebhookStyle(), parameterMap);
                case IMAGE -> parseDiscordConfigTemplate(config.discordImageMessageStyle(), parameterMap);
                case IMAGE_WEBHOOK -> parseDiscordConfigTemplate(config.discordImageMessageWebhookStyle(), parameterMap);
                case LEFT -> parseDiscordConfigTemplate(
                        setConfigTemplateTranslatableParameters(config.discordPlayerLeftStyle(), PLAYER_LEFT),
                        parameterMap
                );
                case JOIN -> parseDiscordConfigTemplate(
                        setConfigTemplateTranslatableParameters(config.discordPlayerJoinedStyle(), PLAYER_JOINED),
                        parameterMap
                );
                case DEATH, PET_DEATH -> parseDiscordConfigTemplate(config.discordPlayerDeathMessageStyle(), parameterMap);
                case ADVANCEMENT_GOAL -> parseDiscordConfigTemplate(
                        setConfigTemplateTranslatableParameters(config.discordPlayerAdvancementGoalStyle(), ADVANCEMENT_GOAL),
                        parameterMap
                );
                case ADVANCEMENT_TASK -> parseDiscordConfigTemplate(
                        setConfigTemplateTranslatableParameters(config.discordPlayerAdvancementTaskStyle(), ADVANCEMENT_TASK),
                        parameterMap
                );
                case ADVANCEMENT_CHALLENGE -> parseDiscordConfigTemplate(
                        setConfigTemplateTranslatableParameters(config.discordPlayerAdvancementChallengeStyle(), ADVANCEMENT_CHALLENGE),
                        parameterMap
                );
                case SAY_COMMAND -> parseDiscordConfigTemplate(config.discordSayCommandStyle(), parameterMap);
                case ME_COMMAND -> parseDiscordConfigTemplate(config.discordMeCommandStyle(), parameterMap);
                case ME_COMMAND_WEBHOOK -> parseDiscordConfigTemplate(config.discordMeCommandWebhookStyle(), parameterMap);
                case TELLRAW_COMMAND -> parseDiscordConfigTemplate(config.discordTellrawCommandStyle(), parameterMap);
                case COMMAND_LOG -> parseDiscordConfigTemplate(config.discordCommandLogStyle(), parameterMap);
            };

            return Optional.of(result);
        } catch (Exception e){
            LOGGER.error("Error parsing discord message style for message type [{}]", messageType);
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private static String setConfigTemplateTranslatableParameters(String configTemplate, String... translatableParameters) {
        for (String param : translatableParameters)
            configTemplate = configTemplate.replace(param, clearTranslatedString(DiscordLocaleProvider.getTranslate(unwrapBraces(param))));

        return configTemplate;
    }

    private static String clearTranslatedString(String text) {
        return text.replaceAll("%s|:|«|»", "").trim();
    }
}
