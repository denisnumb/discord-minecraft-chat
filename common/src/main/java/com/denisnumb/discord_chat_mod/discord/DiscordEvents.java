package com.denisnumb.discord_chat_mod.discord;

import com.denisnumb.discord_chat_mod.discord.model.WebhookPayload;
import com.denisnumb.discord_chat_mod.utils.ColorUtils;
import com.denisnumb.discord_chat_mod.utils.EmojiUtils;
import com.denisnumb.discord_chat_mod.utils.JavaUtils;
import com.denisnumb.discord_chat_mod.utils.MinecraftUtils;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.model.DiscordGuildContext;
import com.denisnumb.discord_chat_mod.discord.model.DiscordMentionData;
import com.denisnumb.discord_chat_mod.discord.utils.DiscordMentionsUtils;
import com.denisnumb.discord_chat_mod.discord.utils.EmbedToComponentConverter;
import com.denisnumb.discord_chat_mod.locale.DiscordLocaleProvider;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.denisnumb.discord_chat_mod.markdown.MarkdownParser;
import com.denisnumb.discord_chat_mod.markdown.MarkdownToComponentConverter;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.sticker.StickerItem;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.denisnumb.discord_chat_mod.utils.ColorUtils.Color.CHAT_LINK_COLOR;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.jda;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.server;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.getServerPlayerCount;
import static com.denisnumb.discord_chat_mod.chat_style.ChatStyleUtils.applyParametersToTemplate;
import static com.denisnumb.discord_chat_mod.chat_style.ChatStyleUtils.parseConfigTemplateMarkdown;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.*;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordUrlsUtils.retrieveMessageEmbedUrls;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils.*;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordWebhookUtils.sendWebhookWithFiles;

public final class DiscordEvents extends ListenerAdapter {
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isWebhookMessage() || event.getAuthor().getId().equals(jda.getSelfUser().getId()))
            return;
        if (!isTrackedChannel(event.getMessage().getChannelId()))
            return;

        if (ConfigProvider.getConfig().isDiscordMessagesLoggingEnabled()){
            System.out.printf("[Discord] <%s> %s%n",
                    event.getAuthor().getEffectiveName(),
                    event.getMessage().getContentDisplay());
        }

        if (getServerPlayerCount(server) > 0){
            prepareComponents(event.getMessage())
                    .thenAccept(components -> components.forEach(MinecraftUtils::sendSystemMessageToAllPlayers));
        }

        DiscordChannelRegistry.getAllContexts().stream()
                .filter(ctx -> !ctx.defaultChannel.getId().equals(event.getMessage().getChannelId()))
                .forEach(ctx -> retranslateGuildMessage(ctx, event));
    }

    private static void retranslateGuildMessage(DiscordGuildContext guildContext, MessageReceivedEvent event) {
        List<MessageEmbed> embeds = event.getMessage().getEmbeds();
        MessageEmbed embed = embeds.isEmpty() ? null : event.getMessage().getEmbeds().getFirst();

        List<WebhookPayload.WebhookAttachment> attachments = new ArrayList<>();

        for (Message.Attachment attachment : event.getMessage().getAttachments()) {
            try {
                attachments.add(new WebhookPayload.WebhookAttachment(JavaUtils.getInputStreamFromUrl(attachment.getUrl()).readAllBytes(), attachment.getFileName()));
            } catch (Exception ignored) {}
        }

        if (attachments.size() < 10 && !event.getMessage().getStickers().isEmpty()){
            StickerItem sticker = event.getMessage().getStickers().getFirst();
            try {
                attachments.add(new WebhookPayload.WebhookAttachment(
                        JavaUtils.getInputStreamFromUrl(sticker.getIconUrl()).readAllBytes(),
                        getStickerFileName(sticker.getIconUrl())
                ));
            } catch (Exception ignored) {}
        }

        String userName = ConfigProvider.getConfig()
                .discordGuildForwardedMessageUserNameStyle()
                .replace(USER, event.getAuthor().getEffectiveName())
                .replace(MEMBER, event.getMember().getEffectiveName())
                .replace(GUILD, event.getGuild().getName());
        String messageContent = event.getMessage().getContentRaw();

        guildContext.getWebhook(guildContext.defaultChannel).ifPresentOrElse(
                webhook -> {
                    sendWebhookWithFiles(
                            webhook.getUrl(),
                            new WebhookPayload(messageContent, embed)
                                    .withAvatarUrl(event.getAuthor().getAvatarUrl())
                                    .withUsername(userName),
                            attachments
                    );
                },
                () -> {
                    String message = DiscordLocaleProvider.Discord.forwardedGuildMessage(event.getMember().getEffectiveName(), event.getGuild().getName()) + "\n" + messageContent;
                    prepareDiscordMessage(
                            guildContext.defaultChannel,
                            new DiscordChatStyleProvider.DiscordMessageComponents(
                                    Optional.of(message),
                                    embed == null
                                        ? Optional.empty()
                                        : Optional.of(embed)
                            )
                    ).ifPresent(mca -> {
                        for (WebhookPayload.WebhookAttachment attachment : attachments)
                            mca.addFiles(FileUpload.fromData(attachment.data(), attachment.fileName()));
                        sendDiscordMessage(mca, guildContext.defaultChannel, false);
                    });
                }
        );
    }

    private static CompletableFuture<List<Component>> prepareComponents(Message message) {
        Member member = Objects.requireNonNull(message.getMember());
        MessageContext ctx = buildMessageContext(member, message);

        CompletableFuture<Optional<Component>> textFuture = buildTextComponent(message, ctx);
        Optional<Component> attachmentComponent = buildAttachmentsComponent(message, ctx);
        Optional<Component> stickerComponent = buildStickerComponent(message, ctx);
        List<Component> embedComponents = buildEmbedComponents(message);

        return textFuture.thenApply(textOpt -> {
            List<Component> result = new ArrayList<>();
            textOpt.ifPresent(result::add);
            attachmentComponent.ifPresent(result::add);
            stickerComponent.ifPresent(result::add);
            if (textOpt.isEmpty() && !embedComponents.isEmpty())
                result.add(wrap(Component.empty(), ctx));
            result.addAll(embedComponents);
            return result;
        });
    }

    private record MessageContext(
            Component guild,
            Component userName,
            Component replyPrefix,
            String configTemplate
    ) {}

    private static MessageContext buildMessageContext(Member member, Message message) {
        Component guild = Component.literal(message.getGuild().getName());
        Component userName = buildUserNameComponent(member);
        Component replyPrefix = buildReplyPrefix(message.getReferencedMessage());

        return new MessageContext(
                guild,
                userName,
                replyPrefix,
                ConfigProvider.getConfig().minecraftDiscordMessagesStyle()
        );
    }

    private static Component buildUserNameComponent(Member member) {
        String displayName = ChannelMembersProvider.getMemberDisplayName(member);
        MutableComponent root = Component.empty().withStyle(style -> style
                .withInsertion("@" + displayName)
                .withClickEvent(new ClickEvent.SuggestCommand("/mention " + displayName))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(member.getUser().getName())))
        );

        int[] colors = ColorUtils.parseRoleColors(member.getColors());
        return root.append(MinecraftUtils.buildGradientComponent(member.getEffectiveName(), colors));
    }

    private static Component buildReplyPrefix(Message referencedMessage) {
        if (referencedMessage == null)
            return Component.empty();

        String replyAuthor = referencedMessage.getMember() != null
                ? ChannelMembersProvider.getMemberDisplayName(referencedMessage.getMember())
                : referencedMessage.getAuthor().getEffectiveName();

        String rawPreview = referencedMessage.getContentDisplay();
        String preview = rawPreview.length() > 50
                ? rawPreview.substring(0, 50) + "..."
                : rawPreview;

        return MinecraftLocaleProvider.reply(replyAuthor)
                .append(" ")
                .withColor(0x7A7A7A)
                .withStyle(style -> style
                        .withItalic(true)
                        .withHoverEvent(new HoverEvent.ShowText(
                                Component.literal(replyAuthor + ": " + preview)))
                );
    }

    private static CompletableFuture<Optional<Component>> buildTextComponent(Message message, MessageContext ctx) {
        if (message.getContentRaw().isEmpty() && ctx.replyPrefix().getString().isEmpty())
            return CompletableFuture.completedFuture(Optional.empty());

        if (message.getContentRaw().isEmpty())
            return CompletableFuture.completedFuture(Optional.of(wrap(ctx.replyPrefix(), ctx)));

        Map<String, DiscordMentionData> mentions = DiscordMentionsUtils.collectMessageMentions(message);
        String rawContent = EmojiUtils.replaceDiscordEmojiMentionsToEmojiNames(message.getContentRaw());

        return retrieveMessageEmbedUrls(message)
                .thenApply(embedUrls ->
                        new MarkdownToComponentConverter(
                                MarkdownParser.parseMarkdown(rawContent, embedUrls),
                                mentions
                        ).convertMarkdownTokensToComponent()
                )
                .exceptionally(ignored -> fallbackTextComponent(message, mentions))
                .thenApply(textPart -> {
                    Component combined = ctx.replyPrefix().getString().isEmpty()
                            ? textPart
                            : Component.empty().append(ctx.replyPrefix()).append(textPart);
                    return Optional.of(wrap(combined, ctx));
                });
    }

    private static MutableComponent fallbackTextComponent(Message message, Map<String, DiscordMentionData> mentions) {
        String content = message.getContentRaw();
        for (var entry : mentions.entrySet())
            content = content.replace(entry.getKey(), entry.getValue().prettyMention);

        return Component.literal(content);
    }

    private static Optional<Component> buildAttachmentsComponent(Message message, MessageContext ctx) {
        if (message.getAttachments().isEmpty())
            return Optional.empty();

        MutableComponent part = Component.empty();
        List<Message.Attachment> attachments = message.getAttachments();
        for (int i = 0; i < attachments.size(); i++) {
            var file = attachments.get(i);
            boolean isLast = i == attachments.size() - 1;
            part.append(
                    Component.literal(file.getFileName() + (isLast ? "" : "\n"))
                            .withColor(CHAT_LINK_COLOR)
                            .withStyle(style -> style
                                    .withItalic(true)
                                    .withClickEvent(new ClickEvent.OpenUrl(URI.create(file.getUrl())))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.literal(file.getUrl())))
                            )
            );
        }
        return Optional.of(wrap(part, ctx));
    }

    private static Optional<Component> buildStickerComponent(Message message, MessageContext ctx) {
        if (message.getStickers().isEmpty())
            return Optional.empty();

        StickerItem sticker = message.getStickers().getFirst();
        Component part = MinecraftLocaleProvider.sticker(sticker.getName())
                .withStyle(style -> style
                        .withItalic(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(sticker.getIconUrl())))
                );

        return Optional.of(wrap(part, ctx));
    }

    private static List<Component> buildEmbedComponents(Message message) {
        List<MessageEmbed> embeds = message.getEmbeds();
        if (embeds.isEmpty())
            return Collections.emptyList();

        List<Component> result = new ArrayList<>();
        for (MessageEmbed embed : embeds) {
            if (embed.getType() != EmbedType.RICH)
                continue;

            Map<String, DiscordMentionData> mentions = DiscordMentionsUtils.collectEmbedMentions(embed, message.getGuild());
            EmbedToComponentConverter.buildEmbedComponent(embed, mentions).ifPresent(result::add);
        }

        return result;
    }

    private static Component wrap(Component messageComponent, MessageContext ctx) {
        return applyParametersToTemplate(
                parseConfigTemplateMarkdown(ctx.configTemplate()),
                Map.of(GUILD, ctx.guild(), MEMBER, ctx.userName(), MESSAGE, messageComponent)
        );
    }

    private static boolean isTrackedChannel(String channelId) {
        return DiscordChannelRegistry.getAllContexts().stream()
                .map(ctx -> ctx.getDefaultChannel().getId())
                .anyMatch(id -> id.equals(channelId));
    }
}
