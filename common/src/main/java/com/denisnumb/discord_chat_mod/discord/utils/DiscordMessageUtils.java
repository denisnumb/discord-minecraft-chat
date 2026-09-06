package com.denisnumb.discord_chat_mod.discord.utils;

import com.denisnumb.discord_chat_mod.discord.data_providers.StickersProvider;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.discord.model.DiscordGuildContext;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.denisnumb.discord_chat_mod.utils.JavaUtils;
import com.mojang.logging.LogUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.sticker.GuildSticker;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.logErrorToServer;
import static com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry.*;
import static com.denisnumb.discord_chat_mod.discord.utils.WebhookUtils.*;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.*;


public class DiscordMessageUtils {
    private static final Logger LOGGER = LogUtils.getLogger();
    public record ImageData(String fileName, byte[] data){}
    private static ExecutorService EXECUTOR;

    public static void initDiscordSendExecutor() {
        EXECUTOR = Executors.newFixedThreadPool(5);
    }

    public static void stopDiscordSendExecutor() {
        if (EXECUTOR != null){
            EXECUTOR.shutdownNow();
            EXECUTOR = null;
        }
    }

    public static void handleDiscord(Runnable prepareMessageFunc) {
        if (isDiscordConnected() && EXECUTOR != null)
            EXECUTOR.submit(prepareMessageFunc);
    }

    public static Optional<String> sendMessageFromPlayer(
            ChannelCategory channelCategory,
            List<DiscordGuildContext> guildContexts,
            Player player,
            DiscordMessageComponents messageComponentsWebhook,
            DiscordMessageComponents messageComponents,
            ImageData imageData
    ) {
        Optional<String> optionalImageUrl = Optional.empty();

        for (DiscordGuildContext guildContext : guildContexts){
            GuildMessageChannel channel = guildContext.getChannel(channelCategory);

            if (isChannelCategoryDisabled(channel))
                continue;

            Optional<String> optionalNewUrl = sendImageFromPlayerToChannel(guildContext, channel, player, messageComponentsWebhook, messageComponents, imageData);
            if (optionalImageUrl.isEmpty())
                optionalImageUrl = optionalNewUrl;

            duplicateMessageToDefaultChannel(guildContext, channelCategory,
                    () -> sendImageFromPlayerToChannel(guildContext, guildContext.defaultChannel, player, messageComponentsWebhook, messageComponents, imageData)
            );
        }

        return optionalImageUrl;
    }

    private static Optional<String> sendImageFromPlayerToChannel(
            DiscordGuildContext guildContext,
            GuildMessageChannel channel,
            Player player,
            DiscordMessageComponents messageComponentsWebhook,
            DiscordMessageComponents messageComponents,
            ImageData imageData
    ) {
        Optional<Webhook> optionalWebhook = guildContext.getWebhook(channel);

        if (optionalWebhook.isEmpty()) {
            return prepareDiscordMessage(channel, messageComponents).flatMap(
                    mca -> sendDiscordMessage(mca.addFiles(FileUpload.fromData(imageData.data, imageData.fileName)), channel, true).flatMap(
                            message -> message.getAttachments().stream().findFirst().map(Message.Attachment::getUrl)
                    )
            );
        } else {
            try {
                WebhookPayload payload = messageComponentsWebhook.hasContentAndEmbed()
                        ? new WebhookPayload(messageComponentsWebhook.getContent(), messageComponentsWebhook.getEmbed())
                        : messageComponentsWebhook.hasNoEmbed()
                        ? new WebhookPayload(messageComponentsWebhook.getContent())
                        : new WebhookPayload(messageComponentsWebhook.getEmbed());

                return sendWebhookWithImage(
                        optionalWebhook.get().getUrl(),
                        payload.setUsername(player.getDisplayName().getString())
                                .setAvatarUrl(getPlayerAvatarUrl(player)),
                        new WebhookAttachment(imageData.data(), imageData.fileName())
                ).get();
            } catch (Exception ignored) {
                return Optional.empty();
            }
        }
    }

    public static void sendMessageFromServer(
            ChannelCategory channelCategory,
            List<DiscordGuildContext> guildContexts,
            DiscordMessageComponents messageComponents
    ) {
        sendMessage(channelCategory, guildContexts, null, messageComponents, messageComponents, null, null);
    }
    public static void sendMessageFromServer(
            ChannelCategory channelCategory,
            List<DiscordGuildContext> guildContexts,
            DiscordMessageComponents messageComponents,
            ImageData imageData
    ) {
        sendMessage(channelCategory, guildContexts, null, messageComponents, messageComponents, imageData, null);
    }

    public static void sendMessageFromPlayer(
            ChannelCategory channelCategory,
            List<DiscordGuildContext> guildContexts,
            Player player,
            DiscordMessageComponents messageComponentsWebhook,
            DiscordMessageComponents messageComponents
    ) {
        sendMessage(channelCategory, guildContexts, player, messageComponentsWebhook, messageComponents, null, null);
    }

    public static void sendMessageFromPlayer(
            ChannelCategory channelCategory,
            List<DiscordGuildContext> guildContexts,
            Player player,
            DiscordMessageComponents messageComponentsWebhook,
            DiscordMessageComponents messageComponents,
            StickersProvider.StickerData stickerData
    ) {
        sendMessage(channelCategory, guildContexts, player, messageComponentsWebhook, messageComponents, null, stickerData);
    }

    private static void sendMessage(
            ChannelCategory channelCategory,
            List<DiscordGuildContext> guildContexts,
            @Nullable Player player,
            DiscordMessageComponents messageComponentsWebhook,
            DiscordMessageComponents messageComponents,
            @Nullable ImageData imageData,
            @Nullable StickersProvider.StickerData stickerData
    ) {
        for (DiscordGuildContext guildContext : guildContexts){
            GuildMessageChannel channel = guildContext.getChannel(channelCategory);

            if (isChannelCategoryDisabled(channel))
                continue;

            duplicateMessageToDefaultChannel(guildContext, channelCategory,
                    () -> sendMessageToChannel(guildContext, guildContext.defaultChannel, player, messageComponentsWebhook, messageComponents, imageData, stickerData)
            );

            sendMessageToChannel(guildContext, channel, player, messageComponentsWebhook, messageComponents, imageData, stickerData);
        }
    }

    private static void sendMessageToChannel(
            DiscordGuildContext guildContext,
            GuildMessageChannel channel,
            @Nullable Player player,
            DiscordMessageComponents messageComponentsWebhook,
            DiscordMessageComponents messageComponents,
            @Nullable ImageData imageData,
            @Nullable StickersProvider.StickerData stickerData
    ) {
        guildContext.getWebhook(channel).ifPresentOrElse(
                webhook -> sendDiscordWebhookMessage(webhook.getUrl(), player, messageComponentsWebhook, imageData),
                () -> prepareDiscordMessage(channel, messageComponents).ifPresent(mca -> {
                    if (imageData != null)
                        mca.addFiles(FileUpload.fromData(imageData.data, imageData.fileName));

                    if (stickerData != null){
                        GuildSticker sticker = guildContext.guild.getStickerById(stickerData.discordId());
                        if (sticker != null)
                            mca = mca.setStickers(sticker);
                        else{
                            try {
                                mca.addFiles(FileUpload.fromData(
                                        JavaUtils.getInputStreamFromUrl(stickerData.imageUrl()).readAllBytes(),
                                        getStickerFileName(stickerData.imageUrl())
                                ));
                            } catch (Exception ignored) {}
                        }
                    }

                    sendDiscordMessage(mca, channel, false);
                })
        );
    }

    private static void sendDiscordWebhookMessage(
            String webhookUrl,
            @Nullable Player player,
            DiscordMessageComponents components,
            @Nullable ImageData imageData
    ) {
        WebhookPayload payload = components.hasContentAndEmbed()
                ? new WebhookPayload(components.getContent(), components.getEmbed())
                : components.hasNoEmbed()
                ? new WebhookPayload(components.getContent())
                : new WebhookPayload(components.getEmbed());

        payload.setUsername(player != null
                ? player.getDisplayName().getString()
                : getWebhookServerName()
        );

        if (player != null)
            payload.setAvatarUrl(getPlayerAvatarUrl(player));

        if (imageData != null)
            sendWebhookWithImage(webhookUrl, payload, new WebhookAttachment(imageData.data, imageData.fileName));
        else
            sendWebhook(webhookUrl, () -> payload);
    }

    public static Optional<MessageCreateAction> prepareDiscordMessage(GuildMessageChannel channel, DiscordMessageComponents messageComponents) {
        if (!isDiscordConnected())
            return Optional.empty();
        try {
            MessageCreateAction mca = messageComponents.hasContentAndEmbed()
                    ? channel.sendMessage(messageComponents.getContent()).addEmbeds(messageComponents.getEmbed())
                    : messageComponents.hasNoEmbed()
                    ? channel.sendMessage(messageComponents.getContent())
                    : channel.sendMessageEmbeds(messageComponents.getEmbed());

            return Optional.of(mca);
        } catch (InsufficientPermissionException e) {
            logErrorToServer(MinecraftLocaleProvider.Discord.Error.sendMessageWithCause(channel.getName(), e.getMessage()));
            LOGGER.error("", e);
        } catch (ErrorResponseException e) {
            logErrorToServer(MinecraftLocaleProvider.Discord.Error.sendMessageWithCause(channel.getName(), e.getMeaning()));
            LOGGER.error("", e);
        } catch (Exception e) {
            logErrorToServer(MinecraftLocaleProvider.Discord.Error.sendMessage(channel.getName()));
            LOGGER.error("", e);
        }

        return Optional.empty();
    }

    public static Optional<Message> sendDiscordMessage(MessageCreateAction mca, GuildMessageChannel channel, boolean complete) {
        try {
            if (complete)
                return Optional.of(mca.complete());
            else
                mca.queue();
        } catch (InsufficientPermissionException e) {
            logErrorToServer(MinecraftLocaleProvider.Discord.Error.sendMessageWithCause(channel.getName(), e.getMessage()));
            LOGGER.error("", e);
        } catch (ErrorResponseException e) {
            logErrorToServer(MinecraftLocaleProvider.Discord.Error.sendMessageWithCause(channel.getName(), e.getMeaning()));
            LOGGER.error("", e);
        } catch (Exception e) {
            logErrorToServer(MinecraftLocaleProvider.Discord.Error.sendMessage(channel.getName()));
            LOGGER.error("", e);
        }

        return Optional.empty();
    }

    public static void editMessage(Message message, DiscordMessageComponents components) {
        try {
            if (components.hasContentAndEmbed())
                message.editMessage(components.getContent()).setEmbeds(components.getEmbed()).queue();
            else if (components.hasNoEmbed())
                message.editMessage(components.getContent()).queue();
            else
                message.editMessageEmbeds(components.getEmbed()).queue();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    private static void duplicateMessageToDefaultChannel(DiscordGuildContext context, ChannelCategory category, Runnable sendMessageFunction) {
        if (isDiscordConnected()
                && context.duplicateMessages
                && !context.defaultChannel.equals(context.getChannel(category))) {
            sendMessageFunction.run();
        }
    }

    public static String getStickerFileName(String stickerUrl){
        String[] parts = stickerUrl.split("\\.");
        return "sticker" + "." + parts[parts.length - 1];
    }
}