package com.denisnumb.discord_chat_mod;

import com.denisnumb.discord_chat_mod.commands.MentionCommand;
import com.denisnumb.discord_chat_mod.discord.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.model.DiscordMemberData;
import com.denisnumb.discord_chat_mod.markdown.MarkdownParser;
import com.denisnumb.discord_chat_mod.markdown.MarkdownToComponentConverter;
import com.denisnumb.discord_chat_mod.network.mentions.DiscordMentionsPacket;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.denisnumb.discord_chat_mod.ColorUtils.Color.GOLD;
import static com.denisnumb.discord_chat_mod.ColorUtils.Color.PURPLE;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.discord.DiscordUtils.sendEmbedMessage;
import static com.denisnumb.discord_chat_mod.discord.DiscordUtils.sendShortEmbedMessage;
import static com.denisnumb.discord_chat_mod.ColorUtils.Color.*;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.*;
import static com.denisnumb.discord_chat_mod.discord.ServerStatusController.updateServerStatusWithDelay;

@EventBusSubscriber(modid = DiscordChatMod.MODID)
public class MinecraftEvents {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        MentionCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onChatMessage(ServerChatEvent event) {
        String message = event.getRawText();
        Map<String, DiscordMemberData> mentions = Map.of();

        if (isDiscordConnected()) {
            List<DiscordMemberData> memberData = ChannelMembersProvider.getMemberData(discordChannel);

            for (DiscordMemberData member : memberData)
                if (message.contains(member.prettyMention))
                    message = message.replace(member.prettyMention, member.mentionString);

            mentions = new HashMap<>(){{
               for (DiscordMemberData member : memberData)
                   put(member.mentionString, member);
            }};

            discordChannel.sendMessage(String.format("`<%s>` %s", event.getPlayer().getName().getString(), message)).queue();
        }

        event.setMessage(
                new MarkdownToComponentConverter(MarkdownParser.parseMarkdown(message), mentions)
                        .convertMarkdownTokensToComponent()
        );
    }

    @SubscribeEvent
    public static void onPlayerDieEvent(LivingDeathEvent event) {
        if (!isDiscordConnected())
            return;
        if (!(event.getEntity() instanceof Player))
            return;

        String message;
        try{
            message = getLocalizedDeathMessage(event.getSource(), event.getEntity());
        } catch (Exception ignored) {
            message = event.getSource().getLocalizedDeathMessage(event.getEntity()).getString();
        }
        sendShortEmbedMessage(message, DEFAULT);
    }

    @SubscribeEvent
    public static void onAdvancementMade(AdvancementEvent.AdvancementEarnEvent event) {
        if (!isDiscordConnected())
            return;
        if (event.getAdvancement().value().display().isEmpty())
            return;

        DisplayInfo displayInfo = event.getAdvancement().value().display().get();
        if (!displayInfo.shouldAnnounceChat())
            return;


        String message = displayInfo.getType() == AdvancementType.TASK
                ? getTranslate("chat.type.advancement.task")
                : displayInfo.getType() == AdvancementType.GOAL
                ? getTranslate("chat.type.advancement.goal")
                : getTranslate("chat.type.advancement.challenge");

        ResourceLocation advancementId = event.getAdvancement().id();
        ResourceLocation advancementResourceLocation = ResourceLocation.fromNamespaceAndPath(
                advancementId.getNamespace(),
                "advancement/" + advancementId.getPath() + ".json"
        );

        String title = displayInfo.getTitle().getString();
        String description = displayInfo.getDescription().getString();

        var advancementFile = getAdvancementFile(advancementResourceLocation);
        if (advancementFile != null){
            title = getTranslate(advancementId.getNamespace(), getAdvancementField(advancementFile, "title"), title);
            description = getTranslate(advancementId.getNamespace(), getAdvancementField(advancementFile, "description"), description);
        }

        int color = displayInfo.getType() == AdvancementType.CHALLENGE ? PURPLE : GOLD;

        sendEmbedMessage(
                String.format(
                    message,
                    "**" + event.getEntity().getName().getString() + "**",
                    "**`" + title + "`**"
                ),
                description,
                color
        );
    }

    @SubscribeEvent
    public static void onPlayerJoinEvent(PlayerEvent.PlayerLoggedInEvent event) {
        PacketDistributor.sendToPlayer(
                (ServerPlayer)event.getEntity(),
                new DiscordMentionsPacket(ChannelMembersProvider.getMemberData(discordChannel))
        );

        joinLeaveEvent(event);
    }

    @SubscribeEvent
    public static void onPlayerLeaveEvent(PlayerEvent.PlayerLoggedOutEvent event){
        joinLeaveEvent(event);
    }

    private static void joinLeaveEvent(PlayerEvent event) {
        if (!isDiscordConnected())
            return;

        boolean isJoin = event instanceof PlayerEvent.PlayerLoggedInEvent;
        String message = getTranslate(isJoin ? "multiplayer.player.joined" : "multiplayer.player.left");
        int color = isJoin ? GREEN : RED;

        sendShortEmbedMessage(String.format(message, "**" + event.getEntity().getName().getString() + "**"), color);
        updateServerStatusWithDelay();
    }
}
