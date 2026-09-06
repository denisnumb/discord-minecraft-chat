package com.denisnumb.discord_chat_mod.mixin.chat_style;

import com.denisnumb.discord_chat_mod.MinecraftEvents;
import com.denisnumb.discord_chat_mod.utils.MinecraftUtils;
import com.denisnumb.discord_chat_mod.chat_style.CustomChatTypeRegistry;
import com.denisnumb.discord_chat_mod.chat_style.MinecraftChatStyleProvider;
import com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider;
import com.denisnumb.discord_chat_mod.discord.chat_style.MessageType;
import com.denisnumb.discord_chat_mod.discord.model.ChannelCategory;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.Map;
import java.util.Optional;

import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.processChatMessage;
import static com.denisnumb.discord_chat_mod.utils.JavaUtils.mergeMaps;
import static com.denisnumb.discord_chat_mod.chat_style.CustomChatTypeRegistry.buildBound;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.MESSAGE;
import static com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry.getAllContexts;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.buildPlayerParameters;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.getDiscordMessageComponents;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils.handleDiscord;
import static com.denisnumb.discord_chat_mod.discord.utils.DiscordMessageUtils.sendMessageFromPlayer;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @ModifyVariable(
            method = "placeNewPlayer",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lnet/minecraft/network/chat/Component;translatable(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;"
            ),
            require = 0
    )
    private MutableComponent modifyJoinMessage(MutableComponent value, @Local(argsOnly = true)ServerPlayer serverPlayer) {
        return MinecraftEvents.handleJoinLeave(serverPlayer, true).orElse(value).copy();
    }

    @ModifyArgs(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V"
            ),
            require = 0
    )
    private void modifyPlayerChatMessage(
            Args args,
            PlayerChatMessage originalMessage,
            ServerPlayer player,
            ChatType.Bound bound
    ) {
        Component playerComponent = player.getDisplayName();
        Component originalContent = originalMessage.decoratedContent();

        MinecraftUtils.ProcessChatMessageResult chatMessage = processChatMessage(originalContent.getString(), ChannelCategory.PLAYER_CHAT);

        handleDiscord(() -> {
            Map<String, String> parameters = mergeMaps(Map.of(MESSAGE, chatMessage.forDiscord()), buildPlayerParameters(player));
            Optional<DiscordChatStyleProvider.DiscordMessageComponents> chatComponentsOpt = getDiscordMessageComponents(MessageType.CHAT, parameters);
            Optional<DiscordChatStyleProvider.DiscordMessageComponents> webhookComponentsOpt = getDiscordMessageComponents(MessageType.CHAT_WEBHOOK, parameters);

            if (chatComponentsOpt.isPresent() && webhookComponentsOpt.isPresent())
                sendMessageFromPlayer(ChannelCategory.PLAYER_CHAT, getAllContexts(), player, webhookComponentsOpt.get(), chatComponentsOpt.get());
        });

        Component withMarkdown = chatMessage.forMinecraft();
        args.set(0, originalMessage.withUnsignedContent(withMarkdown));

        MinecraftEvents.handleChatMessage(
                CustomChatTypeRegistry.CHAT,
                new MinecraftChatStyleProvider.ChatMessageComponents(playerComponent, withMarkdown, null, player)
        ).ifPresent(styledContent -> {
            ChatType.Bound styledBound = buildBound(CustomChatTypeRegistry.CHAT, player.level().registryAccess(), playerComponent, withMarkdown);
            args.set(0, originalMessage.withUnsignedContent(styledContent));
            args.set(3, styledBound);
        });
    }
}
