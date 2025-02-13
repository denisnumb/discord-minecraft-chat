package com.denisnumb.discord_chat_mod.markdown;

import java.util.HashMap;
import java.util.regex.Pattern;

public class MarkdownPattern{
    public static final Pattern LINK = Pattern.compile("(?<!\\\\)\\[(.+?)\\]\\((https?://\\S+)\\)");
    public static final Pattern UNDERLINED_ITALIC = Pattern.compile("►(?<!\\\\)_(.+?)(?<!\\\\)_►");
    public static final Pattern UNDERLINED = Pattern.compile("(?<!\\\\)►(.+?)(?<!\\\\)►");
    public static final Pattern ITALIC_underline = Pattern.compile("(?<!\\\\)_(.+?)(?<!\\\\)_");
    public static final Pattern BOLD_ITALIC = Pattern.compile("▬(?<!\\\\)\\*(.+?)(?<!\\\\)\\*▬");
    public static final Pattern BOLD = Pattern.compile("(?<!\\\\)▬(.+?)(?<!\\\\)▬");
    public static final Pattern ITALIC_star = Pattern.compile("(?<!\\\\)\\*(.+?)(?<!\\\\)\\*");
    public static final Pattern STRIKETHROUGH = Pattern.compile("(?<!\\\\)~(?<!\\\\)~(.+?)(?<!\\\\)~(?<!\\\\)~");
    public static final Pattern OBFUSCATED = Pattern.compile("(?<!\\\\)\\|(?<!\\\\)\\|(.+?)(?<!\\\\)\\|(?<!\\\\)\\|");
    public static final Pattern URL = Pattern.compile("(https?://\\S+)");
    public static final Pattern DISCORD_MENTION = Pattern.compile("(?<!\\\\)<((?<!\\\\)([@#][!&]?\\d+)|(:.+?:\\d+))(?<!\\\\)>");

    public static final HashMap<Pattern, MarkdownStyle> withStyle = new HashMap<>() {{
        put(LINK, MarkdownStyle.LINK);
        put(UNDERLINED_ITALIC, MarkdownStyle.UNDERLINED_ITALIC);
        put(UNDERLINED, MarkdownStyle.UNDERLINED);
        put(ITALIC_underline, MarkdownStyle.ITALIC_underline);
        put(BOLD_ITALIC, MarkdownStyle.BOLD_ITALIC);
        put(BOLD, MarkdownStyle.BOLD);
        put(ITALIC_star, MarkdownStyle.ITALIC_star);
        put(STRIKETHROUGH, MarkdownStyle.STRIKETHROUGH);
        put(OBFUSCATED, MarkdownStyle.OBFUSCATED);
        put(URL, MarkdownStyle.URL);
        put(DISCORD_MENTION, MarkdownStyle.DISCORD_MENTION);
    }};

    public static boolean isStyleExceptAnother(MarkdownStyle style, MarkdownStyle another){
        if (style == MarkdownStyle.UNDERLINED_ITALIC)
            return another == MarkdownStyle.UNDERLINED || another == MarkdownStyle.ITALIC_underline;
        if (style == MarkdownStyle.BOLD_ITALIC)
            return another == MarkdownStyle.BOLD || another == MarkdownStyle.ITALIC_star;
        return false;
    }
}
