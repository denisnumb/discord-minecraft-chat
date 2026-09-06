package com.denisnumb.discord_chat_mod.utils;

import com.denisnumb.discord_chat_mod.MinecraftEvents;
import com.denisnumb.discord_chat_mod.chat_style.ChatStyleUtils;
import com.denisnumb.discord_chat_mod.chat_style.CustomChatTypeRegistry;
import com.denisnumb.discord_chat_mod.chat_style.MinecraftChatStyleProvider;
import com.denisnumb.discord_chat_mod.commands.set_avatar.AvatarUrlStorage;
import com.denisnumb.discord_chat_mod.config.ConfigDefaults;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.config.IConfigProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.discord.model.DiscordUserData;
import com.denisnumb.discord_chat_mod.discord.model.DiscordMentionData;
import com.denisnumb.discord_chat_mod.markdown.MarkdownParser;
import com.denisnumb.discord_chat_mod.markdown.MarkdownToComponentConverter;
import com.denisnumb.discord_chat_mod.markdown.MinecraftFormattingConverter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.properties.Property;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import com.denisnumb.discord_chat_mod.compat.VanishCompatProvider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.isDiscordConnected;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.server;
import static com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils.getMimeType;
import static com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils.isImageUrl;
import static com.denisnumb.discord_chat_mod.chat_style.ChatStyleUtils.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.Translatable.*;
import static com.denisnumb.discord_chat_mod.utils.JavaUtils.newLinkedHashMapOf;

public final class MinecraftUtils {
    private MinecraftUtils() {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Style TEAMMSG_SUGGEST_STYLE = Style.EMPTY
            .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.type.team.hover")))
            .withClickEvent(new ClickEvent.SuggestCommand("/teammsg "));

    public record ProcessChatMessageResult(Component forMinecraft, String forDiscord) {
    }

    public static ProcessChatMessageResult processChatMessage(String message, ChannelCategory chatCategoryToParseMembers) {
        Map<String, DiscordMentionData> mentions = Map.of();
        String forDiscord = message;

        if (isDiscordConnected()) {
            List<DiscordUserData> memberData
                    = ChannelMembersProvider.getMemberData(chatCategoryToParseMembers);

            for (DiscordUserData member : memberData)
                if (message.contains(member.prettyMention))
                    message = message.replace(member.prettyMention, member.mentionString);

            mentions = new HashMap<>() {{
                for (DiscordUserData member : memberData)
                    put(member.mentionString, new DiscordMentionData(member));
            }};

            forDiscord = MarkdownParser.removeColorTags(EmojiUtils.replaceEmojiCodesToDiscordMentions(message));
            forDiscord = MinecraftFormattingConverter.toDiscordMarkdown(forDiscord);
        }

        Component forMinecraft = new MarkdownToComponentConverter(MarkdownParser.parseMarkdown(message), mentions)
                .convertMarkdownTokensToComponent();

        return new ProcessChatMessageResult(forMinecraft, forDiscord);
    }

    public static FormattedCharSequence subFormattedCharSequence(FormattedCharSequence text, int start, int end) {
        if (start >= end || start < 0) {
            return FormattedCharSequence.EMPTY;
        }

        List<FormattedCharSequence> parts = new ArrayList<>();
        AtomicInteger index = new AtomicInteger();

        text.accept((i, style, codePoint) -> {
            if (index.get() >= start && index.get() < end)
                parts.add(FormattedCharSequence.codepoint(codePoint, style));
            index.getAndIncrement();
            return true;
        });

        return FormattedCharSequence.composite(parts);
    }

    public static boolean hasRunCommandClickEvent(FormattedCharSequence seq, String command) {
        boolean[] found = {false};
        seq.accept((index, style, codePoint) -> {
            if (style.getClickEvent() instanceof ClickEvent.RunCommand(String cmd)
                    && cmd.equals(command)) {
                found[0] = true;
                return false;
            }
            return true;
        });
        return found[0];
    }

    public static List<ServerPlayer> getPlayerListBySelector(String selector){
        try {
            CommandSourceStack fakeSource = server.createCommandSourceStack()
                    .withSuppressedOutput()
                    .withPermission(LevelBasedPermissionSet.ADMIN);
            EntitySelectorParser parser = new EntitySelectorParser(new StringReader(selector), true);

            return parser.parse().findPlayers(fakeSource);
        } catch (Exception e){
            return List.of();
        }
    }

    public static void sendSystemMessageToPlayersBySelector(Component message, String selector) {
        try {
            for (ServerPlayer player : getPlayerListBySelector(selector))
                player.sendSystemMessage(ComponentUtils.updateForEntity(null, message, player, 0), false);
        } catch (CommandSyntaxException e) {
            LOGGER.error("CommandSyntaxException", e);
        } catch (Exception ignored) {}
    }

    public static void sendSystemMessageToAllPlayers(Component message) {
        sendSystemMessageToPlayersBySelector(message, "@a");
    }

    public static void sendMessageToAllPlayersFromPlayer(ServerPlayer player, Component content){
        PlayerList playerList = server.getPlayerList();
        if (playerList == null)
            return;

        Component preparedContent = MinecraftEvents.handleChatMessage(
                CustomChatTypeRegistry.CHAT,
                new MinecraftChatStyleProvider.ChatMessageComponents(player.getDisplayName(), content, null, player)
        ).orElseGet(() -> applyParametersToTemplate(
                parseConfigTemplateMarkdown(ConfigDefaults.MINECRAFT_PLAYER_MESSAGE_STYLE_DEFAULT),
                Map.of(PLAYER, player.getDisplayName(), MESSAGE, content)
        ));

        try {
            for (ServerPlayer serverPlayer : playerList.getPlayers())
                serverPlayer.sendSystemMessage(preparedContent);
        } catch (Exception ignored) {}
    }

    public static void sendTellMessageToTargetPlayersFromPlayer(
            ServerPlayer player,
            List<ServerPlayer> targetPlayers,
            Component content,
            boolean singleOutgoing
    ){
        if (targetPlayers.isEmpty())
            return;

        Component preparedContent = MinecraftEvents.handleChatMessage(
                CustomChatTypeRegistry.MSG_COMMAND_INCOMING,
                new MinecraftChatStyleProvider.ChatMessageComponents(player.getDisplayName(), content, null, player)
        ).orElseGet(() -> ChatStyleUtils.getStyledTranslatableMessage(
                parseConfigTemplateMarkdown(ConfigDefaults.MINECRAFT_TELL_MESSAGE_RECEIVED_STYLE_DEFAULT),
                COMMANDS_MESSAGE_DISPLAY_INCOMING,
                newLinkedHashMapOf(
                        Map.entry(SENDER, player.getDisplayName()),
                        Map.entry(MESSAGE, content)
                ),
                Map.of()
        ));

        for (ServerPlayer serverPlayer : targetPlayers) {
            serverPlayer.sendSystemMessage(preparedContent);
        }

        if (singleOutgoing) {
            Component receivers = targetPlayers.stream()
                    .map(ServerPlayer::getDisplayName)
                    .reduce((a, b) -> Component.literal("").append(a).append(", ").append(b))
                    .orElse(Component.empty());

            Component outgoingContent = MinecraftEvents.handleChatMessage(
                    CustomChatTypeRegistry.MSG_COMMAND_OUTGOING,
                    new MinecraftChatStyleProvider.ChatMessageComponents(receivers, content, null, player)
            ).orElseGet(() -> ChatStyleUtils.getStyledTranslatableMessage(
                    parseConfigTemplateMarkdown(ConfigDefaults.MINECRAFT_TELL_MESSAGE_SENT_STYLE_DEFAULT),
                    COMMANDS_MESSAGE_DISPLAY_OUTGOING,
                    newLinkedHashMapOf(
                            Map.entry(RECEIVER, receivers),
                            Map.entry(MESSAGE, content)
                    ),
                    Map.of()
            ));

            player.sendSystemMessage(outgoingContent);
        } else {
            for (ServerPlayer serverPlayer : targetPlayers) {
                Component outgoingContent = MinecraftEvents.handleChatMessage(
                        CustomChatTypeRegistry.MSG_COMMAND_OUTGOING,
                        new MinecraftChatStyleProvider.ChatMessageComponents(serverPlayer.getDisplayName(), content, null, player)
                ).orElseGet(() -> ChatStyleUtils.getStyledTranslatableMessage(
                        parseConfigTemplateMarkdown(ConfigDefaults.MINECRAFT_TELL_MESSAGE_SENT_STYLE_DEFAULT),
                        COMMANDS_MESSAGE_DISPLAY_OUTGOING,
                        newLinkedHashMapOf(
                                Map.entry(RECEIVER, serverPlayer.getDisplayName()),
                                Map.entry(MESSAGE, content)
                        ),
                        Map.of()
                ));

                player.sendSystemMessage(outgoingContent);
            }
        }
    }

    public static void sendTeamMessageFromPlayer(ServerPlayer player, PlayerTeam team, Component content){
        PlayerList playerList = server.getPlayerList();
        if (playerList == null)
            return;

        Component teamDisplayName = team.getFormattedDisplayName().withStyle(TEAMMSG_SUGGEST_STYLE);
        MinecraftChatStyleProvider.ChatMessageComponents chatMessageComponents
                = new MinecraftChatStyleProvider.ChatMessageComponents(player.getDisplayName(), content, teamDisplayName, player);

        Component preparedContent = MinecraftEvents.handleChatMessage(CustomChatTypeRegistry.TEAM_MSG_COMMAND_INCOMING, chatMessageComponents).orElse(
                applyParametersToTemplate(
                        parseConfigTemplateMarkdown(ConfigDefaults.MINECRAFT_TEAM_MESSAGE_RECEIVED_STYLE_DEFAULT),
                        Map.of(TEAM, teamDisplayName, PLAYER, player.getDisplayName(), MESSAGE, content)
                )
        );

        for (String playerName : team.getPlayers()){
            ServerPlayer serverPlayer = playerList.getPlayerByName(playerName);
            if (serverPlayer == null || player.equals(serverPlayer))
                continue;

            serverPlayer.sendSystemMessage(preparedContent);
        }

        Component outgoingContent = MinecraftEvents.handleChatMessage(CustomChatTypeRegistry.TEAM_MSG_COMMAND_OUTGOING, chatMessageComponents).orElse(
                applyParametersToTemplate(
                        parseConfigTemplateMarkdown(ConfigDefaults.MINECRAFT_TEAM_MESSAGE_SENT_STYLE_DEFAULT),
                        Map.of(TEAM, teamDisplayName, PLAYER, player.getDisplayName(), MESSAGE, content)
                )
        );

        player.sendSystemMessage(outgoingContent);
    }

    public static MutableComponent buildGradientComponent(String text, int[] gradientColors) {
        if (gradientColors.length == 1){
            return Component.literal(text).withColor(gradientColors[0]);
        }

        MutableComponent result = Component.empty();
        int length = text.codePointCount(0, text.length());
        if (length == 0) {
            return result;
        }

        if (length == 1) {
            return result.append(Component.literal(text).withColor(gradientColors[0]));
        }

        int segments = gradientColors.length - 1;
        int index = 0;

        int i = 0;
        while (i < text.length()) {
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            String ch = text.substring(i, i + charCount);
            double t = (double) index / (length - 1);

            int color = ColorUtils.interpolateGradient(gradientColors, segments, t);
            result.append(Component.literal(ch).withColor(color));

            i += charCount;
            index++;
        }

        return result;
    }

    public static void showTitleBarMessage(Component message) {
        Minecraft.getInstance().gui.setOverlayMessage(message, false);
    }

    public static String getPlayerAvatarUrl(Player player){
        IConfigProvider config = ConfigProvider.getConfig();

        if (config.isSetAvatarUrlCommandEnabled()){
            String customAvatarUrl = AvatarUrlStorage.getUrl(player);
            if (customAvatarUrl != null)
                return customAvatarUrl;
        }

        String avatarUrlTemplate = config.webhookPlayerAvatarUrl();
        String defaultAvatarUrl = config.webhookPlayerDefaultAvatarUrl();
        String playerName = player.getName().getString();

        avatarUrlTemplate = avatarUrlTemplate.replace("<name>", playerName);

        if (avatarUrlTemplate.contains("<uuid>")){
            Optional<String> optionalUUID = MinecraftUtils.getUUIDFromMojangAPI(playerName);
            if (optionalUUID.isPresent())
                avatarUrlTemplate = avatarUrlTemplate.replace("<uuid>", optionalUUID.get());
        }

        if (avatarUrlTemplate.contains("<texture>")){
            Optional<String> optionalTexture = MinecraftUtils.getPlayerTextureHash(player);
            if (optionalTexture.isPresent())
                avatarUrlTemplate = avatarUrlTemplate.replace("<texture>", optionalTexture.get());
        }

        if (isImageUrl(getMimeType(avatarUrlTemplate)))
            return avatarUrlTemplate;

        return isImageUrl(getMimeType(defaultAvatarUrl))
                ? defaultAvatarUrl
                : "https://mc-heads.net/avatar/steve_head_png";
    }

    private static Optional<String> getPlayerTextureHash(Player player) {
        try {
            Collection<Property> textures = player.getGameProfile().properties().get("textures");
            if (textures.isEmpty())
                return Optional.empty();

            String encoded = textures.iterator().next().value();
            String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(decoded).getAsJsonObject();
            if (!json.has("textures"))
                return Optional.empty();

            JsonObject texturesObject = json.getAsJsonObject("textures");
            if (!texturesObject.has("SKIN"))
                return Optional.empty();

            String skinUrl = texturesObject.getAsJsonObject("SKIN").get("url").getAsString();
            int slashIndex = skinUrl.lastIndexOf('/');
            if (slashIndex < 0 || slashIndex == skinUrl.length() - 1)
                return Optional.empty();

            return Optional.of(skinUrl.substring(slashIndex + 1));
        } catch (Exception ignored) {}

        return Optional.empty();
    }

    private static Optional<String> getUUIDFromMojangAPI(String username) {
        try {
            URL url = new URI("https://api.mojang.com/users/profiles/minecraft/" + username).toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                response.append(inputLine);
            in.close();

            JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
            return Optional.of(json.get("id").getAsString());
        } catch (Exception ignored){}

        return Optional.empty();
    }

    public static void logErrorToServer(Component message) {
        LOGGER.error(message.getString());
        if (ConfigProvider.getConfig().isLoggingDiscordErrorsToServerChatEnabled())
            sendSystemMessageToPlayersBySelector(buildLogMessageComponent(message, ChatFormatting.RED.getColor()), ConfigProvider.getConfig().discordErrorsChatPlayerSelector());
    }

    public static void logWarnToServer(Component message) {
        LOGGER.warn(message.getString());
        if (ConfigProvider.getConfig().isLoggingDiscordErrorsToServerChatEnabled())
            sendSystemMessageToPlayersBySelector(buildLogMessageComponent(message, ChatFormatting.YELLOW.getColor()), ConfigProvider.getConfig().discordErrorsChatPlayerSelector());
    }

    public static int getServerPlayerCount(@Nullable MinecraftServer server) {
        return VanishCompatProvider.get().getVisiblePlayerCount(server);
    }

    public static int getServerMaxPlayers(@Nullable MinecraftServer server) {
        if (server != null && server.getPlayerList() != null)
            return server.getMaxPlayers();
        return 20;
    }

    public static String[] getServerPlayerNames(@Nullable MinecraftServer server) {
        if (server != null && server.getPlayerList() != null)
            return VanishCompatProvider.get().filterVanishedPlayers(server, server.getPlayerNames());
        return new String[0];
    }

    private static Component buildLogMessageComponent(Component message, int color) {
        return Component.empty()
                .append(Component.literal("[discord_chat_mod] ")
                        .withStyle(style -> style.withBold(true))
                )
                .append(message).withColor(color);
    }
}
