package com.denisnumb.discord_chat_mod;

import javax.annotation.Nullable;

public class ColorUtils {
    public static class Color{

        public static final int RED = 0xE74C3C;
        public static final int LIME = 0x54FB54;
        public static final int GREEN = 0x2ECC71;
        public static final int DARK_GREEN = 0x1F8B4C;
        public static final int DEFAULT = 0;
        public static final int GOLD = 0xF1C40F;
        public static final int WHITE = 0xFFFFFF;
        public static final int PURPLE = 0xA700A7;
        public static final int CHANNEL_MENTION_COLOR = 0x6974c9;
        public static final int CHAT_LINK_COLOR = 0x00b7ff;
    }

    public static String getHexColor(@Nullable java.awt.Color color){
        if (color == null)
            return "#ffffff";
        return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }

    public static String getHexColor(int color){
        return getHexColor(new java.awt.Color(color));
    }
}
