package com.denisnumb.discord_chat_mod.network.image;

import com.denisnumb.discord_chat_mod.DiscordChatMod;
import com.denisnumb.discord_chat_mod.utils.MinecraftUtils;
import com.denisnumb.discord_chat_mod.chat_images.ImageStorage;
import com.denisnumb.discord_chat_mod.discord.chat_style.MessageType;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.denisnumb.discord_chat_mod.network.BigPacketsTransceiver;
import com.denisnumb.discord_chat_mod.network.PlatformPacketDistributor;
import com.denisnumb.discord_chat_mod.network.image.model.ImagePartPacketPayload;
import com.denisnumb.discord_chat_mod.network.image.model.SendTarget;
import com.denisnumb.discord_chat_mod.network.image.model.SendTargetAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.PlayerTeam;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static com.denisnumb.discord_chat_mod.utils.ColorUtils.Color.CHAT_LINK_COLOR;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.isDiscordConnected;
import static com.denisnumb.discord_chat_mod.utils.JavaUtils.mergeMaps;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.*;
import static com.denisnumb.discord_chat_mod.chat_images.utils.ImageUtils.LOCAL_RESOURCE_PREFIX;
import static com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry.getAllContexts;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.*;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.*;

public class ImageTransceiver {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(SendTarget.class, new SendTargetAdapter())
            .create();
    private static final ExecutorService networkPool = Executors.newSingleThreadExecutor();
    private static final Map<Long, ArrayList<byte[]>> receivedParts = new HashMap<>();
    private static long lastImageSendTime = 0;

    public static void sendImage(Player fromPlayer, ImagePartPacketPayload payload) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastImageSendTime < 2000) {
            MinecraftUtils.showTitleBarMessage(MinecraftLocaleProvider.SendImage.cooldown()
                    .withColor(ChatFormatting.YELLOW.getColor()));
            return;
        }

        MinecraftUtils.showTitleBarMessage(MinecraftLocaleProvider.SendImage.sending());
        networkPool.execute(() -> {
            try {
                byte[] data = gson.toJson(payload).getBytes(StandardCharsets.UTF_8);
                lastImageSendTime = currentTime;

                BigPacketsTransceiver.send(data, (partIndex, totalParts, part) ->
                        PlatformPacketDistributor.sendToServer(new ImagePartPacket(currentTime, partIndex, totalParts, part))
                );
            } catch (Exception e) {
                DiscordChatMod.LOGGER.error("ImageSendError: ", e);
                sendErrorMessageToPlayer(fromPlayer, e.getMessage());
            }
        });
    }

    public static void receivePartClientSide(ImagePartPacket packet) {
        receivePartCommon(packet, ImageTransceiver::handleReceivedImageClient);
    }

    public static void receivePartServerSide(ImagePartPacket packet, ServerPlayer fromPlayer) {
        receivePartCommon(packet, rawData -> handleReceivedImageServer(rawData, fromPlayer));
    }

    private static void receivePartCommon(ImagePartPacket packet, Consumer<byte[]> onComplete) {
        BigPacketsTransceiver.receivePart(
                receivedParts,
                packet.imageId(),
                packet.partIndex(),
                packet.totalParts(),
                packet.data()
        ).ifPresent(rawData -> networkPool.execute(() -> onComplete.accept(rawData)));
    }

    private static void handleReceivedImageClient(byte[] rawPayload) {
        ImagePartPacketPayload payload = gson.fromJson(new String(rawPayload, StandardCharsets.UTF_8), ImagePartPacketPayload.class);
        try {
            ImageStorage.registerImageFromBytes(payload.url(), payload.mimeType(), payload.imageData());
        } catch (Exception e) {
            DiscordChatMod.LOGGER.error("ClientImageHandleError: ", e);
        }
    }

    private static void handleReceivedImageServer(byte[] rawPayload, ServerPlayer fromPlayer) {
        ImagePartPacketPayload payload = gson.fromJson(new String(rawPayload, StandardCharsets.UTF_8), ImagePartPacketPayload.class);
        SendTarget target = payload.sendTarget();

        if (target instanceof SendTarget.All){
            if (isDiscordConnected()) sendImageToDiscord(payload, fromPlayer);
            else sendImageToAllPlayers(payload, fromPlayer);
        } else if (target instanceof SendTarget.Players) {
            sendImageToTargetPlayers(payload, fromPlayer);
        } else if (target instanceof SendTarget.Team) {
            sendImageToPlayerTeam(payload, fromPlayer);
        }
    }

    private static void sendImageToAllPlayers(ImagePartPacketPayload payload, ServerPlayer fromPlayer) {
        List<ServerPlayer> players = getServerPlayerList(fromPlayer);
        sendImageDataToPlayers(payload, fromPlayer, players, localResourceUrl -> {
            Component imageComponent = makeImageComponent(payload.displayName(), localResourceUrl);
            sendMessageToAllPlayersFromPlayer(fromPlayer, imageComponent);
        });
    }

    private static void sendImageToTargetPlayers(ImagePartPacketPayload payload, ServerPlayer fromPlayer) {
        SendTarget.Players target = (SendTarget.Players) payload.sendTarget();
        List<ServerPlayer> players = getServerPlayerList(fromPlayer).stream()
                .filter(p -> target.nicknames().contains(p.getGameProfile().name()))
                .toList();

        sendImageDataToPlayers(payload, fromPlayer, players, localResourceUrl -> {
            Component imageComponent = makeImageComponent(payload.displayName(), localResourceUrl);
            sendTellMessageToTargetPlayersFromPlayer(fromPlayer, players, imageComponent, true);
        });
    }

    private static void sendImageToPlayerTeam(ImagePartPacketPayload payload, ServerPlayer fromPlayer) {
        SendTarget.Team target = (SendTarget.Team) payload.sendTarget();
        PlayerTeam team = fromPlayer.level().getScoreboard().getPlayerTeam(target.teamName());
        Collection<String> teamNicknames = team.getPlayers();
        List<ServerPlayer> players = getServerPlayerList(fromPlayer).stream()
                .filter(p -> teamNicknames.contains(p.getGameProfile().name()))
                .toList();

        sendImageDataToPlayers(payload, fromPlayer, players, localResourceUrl -> {
            Component imageComponent = makeImageComponent(payload.displayName(), localResourceUrl);
            sendTeamMessageFromPlayer(fromPlayer, team, imageComponent);
        });
    }

    private static List<ServerPlayer> getServerPlayerList(ServerPlayer fromPlayer) {
        return Objects.requireNonNull(fromPlayer.level().getServer()).getPlayerList().getPlayers();
    }

    private static Component makeImageComponent(String displayName, String localResourceUrl) {
        return Component.literal(displayName).withStyle(style ->
                style.withColor(CHAT_LINK_COLOR)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(localResourceUrl)))
        );
    }

    private static void sendImageDataToPlayers(
            ImagePartPacketPayload payload,
            ServerPlayer fromPlayer,
            List<ServerPlayer> recipients,
            Consumer<String> onSent
    ) {
        long currentTime = System.currentTimeMillis();
        String localResourceUrl = LOCAL_RESOURCE_PREFIX + currentTime + "/" + payload.fileName();

        List<ServerPlayer> allRecipients = new ArrayList<>(recipients);
        if (!allRecipients.contains(fromPlayer)) {
            allRecipients.add(fromPlayer);
        }

        networkPool.execute(() -> {
            try {
                byte[] data = gson.toJson(payload.withUrl(localResourceUrl)).getBytes();
                for (ServerPlayer player : allRecipients) {
                    try {
                        BigPacketsTransceiver.send(data, (partIndex, totalParts, part) ->
                                PlatformPacketDistributor.sendToPlayer(player, new ImagePartPacket(currentTime, partIndex, totalParts, part))
                        );
                    } catch (Exception ignored) {}
                }

                onSent.accept(localResourceUrl);
            } catch (Exception e) {
                DiscordChatMod.LOGGER.error("ImageSendError: ", e);
                sendErrorMessageToPlayer(fromPlayer, e.getMessage());
            }
        });
    }

    private static void sendImageToDiscord(ImagePartPacketPayload payload, ServerPlayer fromPlayer) {
        Map<String, String> parameters = mergeMaps(
                Map.of(IMAGE_URL, String.format("attachment://%s", payload.fileName())),
                buildPlayerParameters(fromPlayer)
        );

        Optional<DiscordMessageComponents> chatComponentsOpt = getDiscordMessageComponents(MessageType.IMAGE, parameters);
        Optional<DiscordMessageComponents> webhookComponentsOpt = getDiscordMessageComponents(MessageType.IMAGE_WEBHOOK, parameters);

        if (chatComponentsOpt.isPresent() && webhookComponentsOpt.isPresent()) {
            DiscordMessageUtils.ImageData dsImageData = new DiscordMessageUtils.ImageData(payload.fileName(), payload.imageData());
            DiscordMessageUtils.sendMessageFromPlayer(ChannelCategory.IMAGES, getAllContexts(), fromPlayer, webhookComponentsOpt.get(), chatComponentsOpt.get(), dsImageData)
                    .ifPresentOrElse(
                            discordUrl -> handleSuccessfulDiscordSend(payload.displayName(), discordUrl, fromPlayer),
                            () -> sendImageToAllPlayers(payload, fromPlayer)
                    );
        }
    }

    private static void handleSuccessfulDiscordSend(String displayName, String imageUrl, ServerPlayer fromPlayer) {
        Component imageComponent = Component.literal(displayName)
                .withStyle(style -> style.withColor(CHAT_LINK_COLOR)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(imageUrl)))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(imageUrl)))
                );

        sendMessageToAllPlayersFromPlayer(fromPlayer, imageComponent);
    }

    private static void sendErrorMessageToPlayer(Player player, String errorMessage) {
        player.displayClientMessage(MinecraftLocaleProvider.SendImage.Error.send(errorMessage).withStyle(ChatFormatting.RED), false);
    }
}
