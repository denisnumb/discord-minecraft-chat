package com.denisnumb.discord_chat_mod.network.screenshot;

import com.denisnumb.discord_chat_mod.network.ModNetworking;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Arrays;

public class ScreenshotSender {
    private static final int MAX_PART_SIZE = 32000;

    public static void sendScreenshot(byte[] screenshotBytes) {
        int totalParts = (int) Math.ceil(screenshotBytes.length / (double) MAX_PART_SIZE);
        long screenshotId = System.currentTimeMillis();

        for (int i = 0; i < totalParts; i++) {
            int start = i * MAX_PART_SIZE;
            int end = Math.min(start + MAX_PART_SIZE, screenshotBytes.length);
            byte[] part = Arrays.copyOfRange(screenshotBytes, start, end);
            PacketDistributor.sendToServer(new ScreenshotPartPacket(screenshotId, i, totalParts, part));
        }
    }
}
