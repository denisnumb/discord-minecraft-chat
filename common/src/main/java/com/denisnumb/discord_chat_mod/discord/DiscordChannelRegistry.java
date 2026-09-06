package com.denisnumb.discord_chat_mod.discord;

import com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils;
import com.denisnumb.discord_chat_mod.config.IConfigProvider;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.config.configs.DiscordGuildsConfig;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.discord.model.DiscordGuildContext;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.denisnumb.discord_chat_mod.utils.JavaUtils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.jda;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.*;

public class DiscordChannelRegistry {
    private static final Map<String, DiscordGuildContext> GUILD_CONTEXTS = new HashMap<>();
    private static Icon webhookAvatar;

    private static final List<Permission> requiredPermissions = List.of(
            Permission.VIEW_CHANNEL,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.MESSAGE_EMBED_LINKS,
            Permission.MESSAGE_ATTACH_FILES,
            Permission.PIN_MESSAGES,
            Permission.MESSAGE_HISTORY
    );

    public static DiscordGuildContext getContext(String guildId) {
        return GUILD_CONTEXTS.get(guildId);
    }

    public static List<DiscordGuildContext> getAllContexts() {
        return GUILD_CONTEXTS.values().stream().toList();
    }

    public static boolean isChannelCategoryDisabled(@Nullable GuildMessageChannel channel){
        return channel == null;
    }

    public static void initDiscordChannels(List<DiscordGuildsConfig.DiscordGuildConfig> guildConfigs) {
        GUILD_CONTEXTS.clear();

        IConfigProvider config = ConfigProvider.getConfig();

        String webhookAvatarUrl = ImageUtils.isImageUrl(ImageUtils.getMimeType(config.webhookServerAvatarUrl()))
                ? config.webhookServerAvatarUrl()
                : jda.getSelfUser().getAvatarUrl();

        try {
            webhookAvatar = webhookAvatarUrl == null
                    ? null
                    : Icon.from(JavaUtils.getInputStreamFromUrl(webhookAvatarUrl));
        } catch (Exception ignored) {}

        for (DiscordGuildsConfig.DiscordGuildConfig guildConfig : guildConfigs){
            if (guildConfig.guildId().isEmpty())
                continue;

            Guild guild = jda.getGuildById(guildConfig.guildId());
            if (guild == null) {
                logErrorToServer(MinecraftLocaleProvider.Discord.Error.invalidGuild(guildConfig.guildId()));
                continue;
            }

            DiscordGuildContext context = new DiscordGuildContext(
                    guild,
                    guildConfig.duplicateMessages(),
                    guildConfig.enablePinnedStatusMessage(),
                    guildConfig.enableSlashCommands(),
                    guildConfig.slashCommandPermissionsConfig()
            );
            try{
                context.setDefaultChannel(getDiscordChannel(context, guildConfig.defaultChannelId()));
            } catch (IllegalStateException e){
                logErrorToServer(MinecraftLocaleProvider.Discord.Error.invalidDefaultChannel(guild.getName()));
                continue;
            }

            context.setServerLogsChannel(getDiscordChannel(context, guildConfig.serverLogsChannelId()));

            for (var entry : guildConfig.channelOverrides().entrySet()) {
                ChannelCategory category = ChannelCategory.fromConfigName(entry.getKey());
                String channelId = entry.getValue();

                if (channelId.equals("-1")){
                    if (category != ChannelCategory.PINNED_STATUS)
                        context.setChannel(category, null);
                    continue;
                }

                GuildMessageChannel channel = getDiscordChannel(context, channelId);
                if (channel != null)
                    context.setChannel(category, channel);
            }

            GUILD_CONTEXTS.put(guildConfig.guildId(), context);
        }

        if (GUILD_CONTEXTS.isEmpty())
            logWarnToServer(MinecraftLocaleProvider.Discord.Warn.noGuildsConfigured());
    }

    static class InvalidChannelException extends IllegalStateException {
        private final Component errorMessage;

        public InvalidChannelException(Component errorMessage){
            this.errorMessage = errorMessage;
        }

        public Component getErrorMessage() {
            return errorMessage;
        }
    }

    private static @Nullable GuildMessageChannel getDiscordChannel(@Nullable DiscordGuildContext context, String channelId) {
        if (context != null && context.CHANNEL_CACHE.containsKey(channelId))
            return context.CHANNEL_CACHE.get(channelId);

        if (!channelId.isEmpty()){
            try {
                GuildMessageChannel channel = context == null
                        ? jda.getChannelById(GuildMessageChannel.class, channelId)
                        : context.guild.getChannelById(GuildMessageChannel.class, channelId);

                if (channel == null)
                    throw new InvalidChannelException(MinecraftLocaleProvider.Discord.Error.invalidChannel(channelId));

                checkDiscordBotPermissionsInChannel(channel);
                initChannelWebhook(context, channel);
                if (context != null)
                    context.CHANNEL_CACHE.put(channelId, channel);

                return channel;
            } catch (InvalidChannelException e) {
                logErrorToServer(e.getErrorMessage());
            } catch (Exception e) {
                logErrorToServer(Component.literal(e.getMessage()));
            }
        }

        return null;
    }

    private static void initChannelWebhook(@Nullable DiscordGuildContext context, GuildMessageChannel channel){
        if (context == null || !ConfigProvider.getConfig().isWebhookModeEnabled())
            return;

        if (channel instanceof TextChannel textChannel){
            if (!textChannel.getGuild().getSelfMember().hasPermission(textChannel, Permission.MANAGE_WEBHOOKS)){
                logErrorToServer(
                        MinecraftLocaleProvider.Discord.Webhook.Error.init(
                                MinecraftLocaleProvider.Discord.Webhook.Error.missingPermission("#" + channel.getName())
                        )
                );
                return;
            }

            Webhook webhook = textChannel.retrieveWebhooks()
                    .complete()
                    .stream()
                    .filter(wh -> jda.getSelfUser().equals(wh.getOwnerAsUser()))
                    .findFirst()
                    .orElse(null);

            try{
                if (webhook == null)
                    webhook = textChannel.createWebhook("DC & Chat Images").setAvatar(webhookAvatar).complete();
                else
                    webhook.getManager().setAvatar(webhookAvatar).queue();

                context.registerWebhook(channel, webhook);
            } catch (Exception e) {
                logWarnToServer(MinecraftLocaleProvider.Discord.Webhook.Error.init(e.getMessage()));
            }

        } else {
            logWarnToServer(
                    MinecraftLocaleProvider.Discord.Webhook.Error.init(
                            MinecraftLocaleProvider.Discord.Webhook.Error.invalidChannelType("#" + channel.getName())
                    )
            );
        }
    }

    private static void checkDiscordBotPermissionsInChannel(GuildMessageChannel channel) throws InvalidChannelException {
        Member selfMember = channel.getGuild().getSelfMember();
        EnumSet<Permission> missingPermissions = requiredPermissions.stream()
                .filter(perm -> !selfMember.hasPermission(channel, perm))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Permission.class)));

        if (!missingPermissions.isEmpty()){


            throw new InvalidChannelException(MinecraftLocaleProvider.Discord.Error.missingPermissions(
                    "#" + channel.getName(),
                    String.join("\n", missingPermissions.stream().map(Permission::getName).toList())
            ));
        }
    }


}
