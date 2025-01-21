package com.denisnumb.discord_chat_mod;

import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.*;
import static com.denisnumb.discord_chat_mod.DiscordUtils.sendEmbedMessage;
import static com.denisnumb.discord_chat_mod.DiscordUtils.sendShortEmbedMessage;
import static com.denisnumb.discord_chat_mod.DiscordUtils.Color.*;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.*;
import static com.denisnumb.discord_chat_mod.ServerStatusController.updateServerStatusWithDelay;

@Mod.EventBusSubscriber(modid = DiscordChatMod.MODID)
public class MinecraftEvents {
    @SubscribeEvent
    public static void onChatMessage(ServerChatEvent event) {
        if (!isDiscordConnected())
            return;

        discordChannel.sendMessage(String.format("`<%s>` %s", event.getPlayer().getName().getString(), event.getRawText())).queue();
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
        DisplayInfo displayInfo = event.getAdvancement().getDisplay();
        if (displayInfo == null)
            return;
        if (!displayInfo.shouldAnnounceChat())
            return;


        String message = displayInfo.getFrame() == FrameType.TASK
                ? getTranslate("chat.type.advancement.task")
                : displayInfo.getFrame() == FrameType.GOAL
                ? getTranslate("chat.type.advancement.goal")
                : getTranslate("chat.type.advancement.challenge");

        ResourceLocation advancementId = event.getAdvancement().getId();
        ResourceLocation advancementResourceLocation = new ResourceLocation(
                advancementId.getNamespace(),
                "advancements/" + advancementId.getPath() + ".json"
        );

        String title = displayInfo.getTitle().getString();
        String description = displayInfo.getDescription().getString();

        var advancementFile = getAdvancementFile(advancementResourceLocation);
        if (advancementFile != null){
            title = getTranslate(advancementId.getNamespace(), getAdvancementField(advancementFile, "title"), title);
            description = getTranslate(advancementId.getNamespace(), getAdvancementField(advancementFile, "description"), description);
        }

        int color = displayInfo.getFrame() == FrameType.CHALLENGE ? PURPLE : GOLD;

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
