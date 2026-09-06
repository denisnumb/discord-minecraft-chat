package com.denisnumb.discord_chat_mod.markdown;

import java.util.ArrayList;
import java.util.List;

public final class MinecraftFormattingConverter {
    private MinecraftFormattingConverter() {}

    private static final char SECTION_SIGN = '\u00A7';

    public static String toDiscordMarkdown(String text) {
        if (text.indexOf(SECTION_SIGN) < 0)
            return text;

        StringBuilder result = new StringBuilder(text.length());
        List<String> openTokens = new ArrayList<>();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c != SECTION_SIGN) {
                result.append(c);
                continue;
            }

            if (i + 1 >= text.length())
                break;

            char code = Character.toLowerCase(text.charAt(++i));
            String token = switch (code) {
                case 'l' -> "**";
                case 'o' -> "*";
                case 'n' -> "__";
                case 'm' -> "~~";
                default -> null;
            };

            if (token != null) {
                if (!openTokens.contains(token)) {
                    openTokens.add(token);
                    result.append(token);
                }
            } else if (code == 'r' || isColorCode(code)) {
                closeOpenTokens(result, openTokens);
            }
        }

        closeOpenTokens(result, openTokens);
        return result.toString();
    }

    private static boolean isColorCode(char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f');
    }

    private static void closeOpenTokens(StringBuilder result, List<String> openTokens) {
        for (int i = openTokens.size() - 1; i >= 0; i--)
            result.append(openTokens.get(i));
        openTokens.clear();
    }
}
