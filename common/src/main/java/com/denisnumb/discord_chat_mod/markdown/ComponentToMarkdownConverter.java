package com.denisnumb.discord_chat_mod.markdown;

import com.denisnumb.discord_chat_mod.locale.DiscordLocaleProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class ComponentToMarkdownConverter {
    private ComponentToMarkdownConverter() {}

    public static String componentToDiscordMarkdown(Component component) {
        StringBuilder sb = new StringBuilder();
        appendComponent(component, sb);
        return sb.toString();
    }

    private static void appendComponent(Component component, StringBuilder sb) {
        String raw = resolveOwnText(component.getContents());

        if (!raw.isEmpty()) {
            appendStyled(raw, component.getStyle(), sb);
        }

        for (Component sibling : component.getSiblings()) {
            appendComponent(sibling, sb);
        }
    }

    private static String resolveOwnText(ComponentContents contents) {
        if (contents instanceof PlainTextContents plain) {
            return plain.text();
        }

        if (contents instanceof TranslatableContents translatable) {
            String translation = DiscordLocaleProvider.getTranslate(translatable.getKey());

            Object[] args = translatable.getArgs();
            String[] resolvedArgs = new String[args.length];
            for (int i = 0; i < args.length; i++) {
                resolvedArgs[i] = resolveArg(args[i]);
            }

            return formatTranslation(translation, resolvedArgs);
        }

        return "";
    }

    private static String resolveArg(Object arg) {
        if (arg instanceof Component c) {
            StringBuilder tmp = new StringBuilder();
            appendComponent(c, tmp);
            return tmp.toString();
        }

        return String.valueOf(arg);
    }

    private static String formatTranslation(String pattern, String[] args) {
        try {
            return String.format(pattern, (Object[]) args);
        } catch (Exception e) {
            return pattern;
        }
    }

    private static void appendStyled(String text, Style style, StringBuilder sb) {
        String result = escapeDiscordMarkdown(text);

        if (style.isStrikethrough()) result = "~~" + result + "~~";
        if (style.isUnderlined()) result = "__" + result + "__";
        if (style.isItalic()) result = "*" + result + "*";
        if (style.isBold()) result = "**" + result + "**";
        if (style.isObfuscated()) result = "||" + result + "||";

        sb.append(result);
    }

    private static String escapeDiscordMarkdown(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("*", "\\*")
                .replace("_", "\\_")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace("|", "\\|");
    }
}
