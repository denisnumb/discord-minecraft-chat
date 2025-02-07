package com.denisnumb.discord_chat_mod.network.screenshot;

import net.dv8tion.jda.api.utils.FileUpload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.discordChannel;

public class ScreenshotReceiver {
    private static final Map<Long, ArrayList<byte[]>> receivedParts = new HashMap<>();

    public static void receivePart(ScreenshotPartPacket packet, ServerPlayer player) {
        receivedParts.putIfAbsent(packet.imageId, new ArrayList<>());
        receivedParts.get(packet.imageId).add(packet.partIndex, packet.data);

        if (receivedParts.get(packet.imageId).size() == packet.totalParts) {
            byte[] screenshotBytes = mergeParts(receivedParts.get(packet.imageId));
            receivedParts.remove(packet.imageId);

            discordChannel.sendMessage("`<" + player.getName().getString() + ">`")
                    .addFiles(FileUpload.fromData(screenshotBytes, System.currentTimeMillis() + ".png"))
                    .queue();
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
