package com.denisnumb.discord_chat_mod.chat_images.clipboard;

import static com.sun.jna.Platform.*;

public final class ClipboardImageUtils {
    private ClipboardImageUtils() {}

    public static byte[] getImageBytes() throws IllegalStateException {
        return switch (detectPlatform()) {
            case WINDOWS -> new WindowsClipboardImageReader().read();
            case MAC -> new MacOSClipboardImageReader().read();
            case LINUX -> new LinuxClipboardImageReader().read();
            default -> new byte[0];
        };
    }

    private static int detectPlatform() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) return WINDOWS;
        if (os.contains("mac")) return MAC;
        return LINUX;
    }
}