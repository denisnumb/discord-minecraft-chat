package com.denisnumb.discord_chat_mod;

import com.denisnumb.discord_chat_mod.commands.*;
import com.denisnumb.discord_chat_mod.commands.set_avatar.SetAvatarCommand;
import com.denisnumb.discord_chat_mod.commands.vanilla.*;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.config.IConfigProvider;
import com.denisnumb.discord_chat_mod.discord.chat_style.MessageType;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.markdown.ComponentToMarkdownConverter;
import com.denisnumb.discord_chat_mod.utils.DeathMessageUtils;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.denisnumb.discord_chat_mod.compat.VanishCompatProvider;

import static com.denisnumb.discord_chat_mod.utils.AdvancementIconParser.parseAdvancementIcon;
import static com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry.getAllContexts;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils.*;
import static com.denisnumb.discord_chat_mod.discord.ServerStatusController.updateServerStatusWithDelay;
import static com.denisnumb.discord_chat_mod.chat_style.MinecraftChatStyleProvider.*;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.*;
import static com.denisnumb.discord_chat_mod.utils.JavaUtils.mergeMaps;

public class MinecraftEvents {
    public static void handleRegisterCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        MentionCommand.register(dispatcher);
        SendStickerCommand.register(dispatcher);
        ReloadConfigCommand.register(dispatcher);
        SayCommand.register(dispatcher);
        TellrawCommand.register(dispatcher, context);
        MsgCommand.register(dispatcher);
        EmoteCommand.register(dispatcher);
        TeamMsgCommand.register(dispatcher);

        IConfigProvider config = ConfigProvider.getConfig();
        if (config.isSetAvatarUrlCommandEnabled())
            SetAvatarCommand.register(dispatcher);
    }

    public static Optional<Component> handleChatMessage(ResourceKey<ChatType> chatType, ChatMessageComponents components) {
        if (ConfigProvider.getConfig().isMinecraftChatCustomizationEnabled())
            return getStyledChatMessage(chatType, components);
        return Optional.empty();
    }

    public static Optional<Component> handlePlayerOrPetDie(List<CombatEntry> combatEntries, LivingEntity entity) {
        if (!(entity instanceof Player) && !(entity instanceof TamableAnimal a && a.isTame()))
            return Optional.empty();

        DeathMessageUtils.DeathMessageComponents components = DeathMessageUtils.getDeathMessageComponents(combatEntries, entity);

        if (entity instanceof Player){
            handleDiscord(() -> {
                Map<String, String> parameters = mergeMaps(
                        Map.of(DEATH_MESSAGE, formatDeathMessageComponents(components)),
                        buildPlayerParameters(components.diedEntity().getString(), entity)
                );
                getDiscordMessageComponents(MessageType.DEATH, parameters)
                        .ifPresent(discordMessageComponents -> sendMessageFromServer(ChannelCategory.DEATHS, getAllContexts(), discordMessageComponents));
            });
        }

        if (ConfigProvider.getConfig().isMinecraftChatCustomizationEnabled())
            return Optional.of(getStyledDeathMessage(components, entity));
        return Optional.empty();
    }

    public static Optional<Component> handleAdvancementMade(Player player, AdvancementHolder advancementHolder) {
        if (advancementHolder.value().display().isEmpty())
            return Optional.empty();

        DisplayInfo displayInfo = advancementHolder.value().display().get();
        if (!displayInfo.shouldAnnounceChat())
            return Optional.empty();

        handleDiscord(() -> {
            String formattedTitle = ComponentToMarkdownConverter.componentToDiscordMarkdown(displayInfo.getTitle());
            String formattedDescription = ComponentToMarkdownConverter.componentToDiscordMarkdown(displayInfo.getDescription());

            MessageType messageType = switch (displayInfo.getType()) {
                case TASK -> MessageType.ADVANCEMENT_TASK;
                case CHALLENGE -> MessageType.ADVANCEMENT_CHALLENGE;
                case GOAL -> MessageType.ADVANCEMENT_GOAL;
            };

            Map<String, String> parameters = mergeMaps(
                    Map.of(ADVANCEMENT, formattedTitle, DESCRIPTION, formattedDescription, ICON_URL, "attachment://icon.png"),
                    buildPlayerParameters(player)
            );

            getDiscordMessageComponents(messageType, parameters).ifPresent(components ->
                    parseAdvancementIcon(displayInfo).ifPresentOrElse(
                            iconData -> sendMessageFromServer(ChannelCategory.ADVANCEMENTS, getAllContexts(), components, iconData),
                            () -> sendMessageFromServer(ChannelCategory.ADVANCEMENTS, getAllContexts(), components)
                    )
            );
        });

        if (ConfigProvider.getConfig().isMinecraftChatCustomizationEnabled())
            return Optional.of(getStyledAdvancementMessage(player, displayInfo));
        return Optional.empty();
    }

    public static void handleCommandExecution(CommandSourceStack source, String command) {
        IConfigProvider config = ConfigProvider.getConfig();
        if (!config.isCommandLogEnabled())
            return;

        Player player = source.getPlayer();
        if (player == null)
            return;

        net.minecraft.server.permissions.PermissionCheck levelCheck = switch (Math.max(0, Math.min(4, config.commandLogMinPermissionLevel()))) {
            case 0 -> Commands.LEVEL_ALL;
            case 1 -> Commands.LEVEL_MODERATORS;
            case 3 -> Commands.LEVEL_ADMINS;
            case 4 -> Commands.LEVEL_OWNERS;
            default -> Commands.LEVEL_GAMEMASTERS;
        };
        if (!Commands.hasPermission(levelCheck).test(source))
            return;

        String trimmed = command.startsWith("/") ? command.substring(1) : command;
        int spaceIdx = trimmed.indexOf(' ');
        String rootCommand = (spaceIdx == -1 ? trimmed : trimmed.substring(0, spaceIdx))
                .toLowerCase(java.util.Locale.ROOT);
        if (rootCommand.isEmpty() || config.commandLogIgnoredCommands().contains(rootCommand))
            return;

        String displayCommand = "/" + trimmed;

        handleDiscord(() -> {
            Map<String, String> parameters = mergeMaps(
                    Map.of(COMMAND, displayCommand),
                    buildPlayerParameters(player)
            );
            getDiscordMessageComponents(MessageType.COMMAND_LOG, parameters)
                    .ifPresent(components -> sendMessageFromServer(ChannelCategory.COMMAND_LOG, getAllContexts(), components));
        });
    }

    public static Optional<Component> handleJoinLeave(Player player, boolean isJoin) {
        if (VanishCompatProvider.get().isVanished(player))
            return Optional.empty();

        handleDiscord(() -> {
            MessageType messageType = isJoin ? MessageType.JOIN : MessageType.LEFT;
            getDiscordMessageComponents(messageType, buildPlayerParameters(player))
                    .ifPresent(components -> sendMessageFromServer(ChannelCategory.PLAYER_JOIN_LEAVE, getAllContexts(), components));
            updateServerStatusWithDelay();
        });

        if (ConfigProvider.getConfig().isMinecraftChatCustomizationEnabled())
            return Optional.of(getStyledJoinedLeftMessage(player, isJoin));
        return Optional.empty();
    }
}

