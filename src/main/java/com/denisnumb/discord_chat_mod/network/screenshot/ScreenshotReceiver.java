package com.denisnumb.discord_chat_mod.network.screenshot;

import com.denisnumb.discord_chat_mod.markdown.tellraw.TellRawComponent;
import com.denisnumb.discord_chat_mod.markdown.tellraw.TellRawComponentEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.FileUpload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.denisnumb.discord_chat_mod.ColorUtils.Color.CHAT_LINK_COLOR;
import static com.denisnumb.discord_chat_mod.ColorUtils.getHexColor;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.discordChannel;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.executeServerCommand;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.getTranslate;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.SCREENSHOT;
import static com.denisnumb.discord_chat_mod.discord.DiscordUtils.prepareTellRawCommand;

public class ScreenshotReceiver {
    private static final Map<Long, ArrayList<byte[]>> receivedParts = new HashMap<>();

    public static void receivePart(ScreenshotPartPacket packet, ServerPlayer player) {
        receivedParts.putIfAbsent(packet.imageId, new ArrayList<>());
        receivedParts.get(packet.imageId).add(packet.partIndex, packet.data);

        if (receivedParts.get(packet.imageId).size() == packet.totalParts) {
            byte[] screenshotBytes = mergeParts(receivedParts.get(packet.imageId));
            receivedParts.remove(packet.imageId);

             Message message = discordChannel.sendMessage("`<" + player.getName().getString() + ">`")
                     .addFiles(FileUpload.fromData(screenshotBytes, System.currentTimeMillis() + ".png"))
                     .complete();

            executeServerCommand(prepareTellRawCommand(new ArrayList<>(){{
                add(new TellRawComponent("<" + player.getName().getString() + "> "));
                add(new TellRawComponent(getTranslate(SCREENSHOT, "Screenshot"))
                        .setColor(getHexColor(CHAT_LINK_COLOR))
                        .addClickEvent(new TellRawComponentEvent("open_url", message.getAttachments().get(0).getUrl()))
                        .addHoverEvent(new TellRawComponentEvent("show_text", message.getAttachments().get(0).getUrl()))
                );
            }}));
        }
    }

    private static byte[] mergeParts(ArrayList<byte[]> parts) {
        int totalSize = parts.stream().mapToInt(arr -> arr.length).sum();
        byte[] result = new byte[totalSize];

        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }

        return result;
    }
}
