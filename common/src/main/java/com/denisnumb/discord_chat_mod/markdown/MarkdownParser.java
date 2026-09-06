package com.denisnumb.discord_chat_mod.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.denisnumb.discord_chat_mod.utils.ColorUtils.parseColor;

public abstract class MarkdownParser {
    private static final Pattern[] colorTagPatterns = {
            MarkdownPattern.COLOR_RANGE,
            MarkdownPattern.COLOR_SINGLE,
            MarkdownPattern.COLOR_OPEN
    };

    public static String removeColorTags(String text){
        for (Pattern pattern : colorTagPatterns){
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()){
                String coloredText = matcher.group(2);
                if (parseColor(matcher.group(1)) != null && !coloredText.isBlank())
                    text = text.replace(matcher.group(0), coloredText);
            }
        }

        return text;
    }

    public static List<MarkdownToken> parseMarkdown(String rawText){
        return parseMarkdown(rawText, Map.of());
    }

    public static List<MarkdownToken> parseMarkdown(String rawText, Map<String, String> urlMapping) {
        ArrayList<MarkdownToken> tokens = new ArrayList<>();
        int currentPos = 0;

        rawText = replaceDoubleSpecialCharacters(rawText);

        while (currentPos < rawText.length()) {
            Matcher matcher = null;
            MarkdownStyle style = null;

            for (Map.Entry<Pattern, MarkdownStyle> entry : MarkdownPattern.withStyle.entrySet()){
                Pattern pattern = entry.getKey();
                MarkdownStyle currentStyle = entry.getValue();
                Matcher currentMatcher = pattern.matcher(rawText.substring(currentPos));

                if (currentMatcher.find()){
                    if (matcher != null){
                        if ((MarkdownPattern.isStyleExceptAnother(style, currentStyle) && matcher.start() == currentMatcher.start())
                                || matcher.start() < currentMatcher.start())
                            continue;
                    }

                    matcher = currentMatcher;
                    style = currentStyle;
                }
            }

            if (matcher == null){
                addTextPart(tokens, rawText.substring(currentPos));
                break;
            }

            addTextPart(tokens, rawText.substring(currentPos, currentPos + matcher.start()));

            String matchedText = matcher.group(0);
            String innerText = matcher.groupCount() > 0 ? matcher.group(1) : null;

            MarkdownToken token;

            switch (style){
                case MarkdownStyle.URL -> {
                    token = new MarkdownToken(matchedText);
                    token.url = urlMapping.getOrDefault(matchedText, matchedText);
                }
                case MarkdownStyle.LINK -> {
                    token = new MarkdownToken(matchedText, innerText);
                    String url = matcher.group(2);
                    token.url = urlMapping.getOrDefault(url, url);
                }
                case MarkdownStyle.DISCORD_MENTION -> {
                    token = new MarkdownToken(matchedText);
                    token.isMention = true;
                }
                case MarkdownStyle.EMOJI -> token = new MarkdownToken(matchedText);
                case MarkdownStyle.COLOR_RANGE,
                     MarkdownStyle.COLOR_SINGLE,
                     MarkdownStyle.COLOR_OPEN -> {
                    Integer parsedColor = parseColor(matcher.group(1));
                    String coloredText = matcher.group(2);
                    if (parsedColor != null && !coloredText.isBlank()){
                        token = new MarkdownToken(matchedText, coloredText);
                        token.color = parsedColor;
                    } else
                        token = new MarkdownToken(matchedText);
                }
                default -> {
                    token = new MarkdownToken(matchedText, innerText);
                    setTokenStyles(token, style);
                }
            }

            if (!token.rawText.equals(token.text)){
                List<MarkdownToken> innerTokens = parseInnerTokens(token);
                if (!innerTokens.isEmpty())
                    token.setInnerTokens(innerTokens);
            }

            addToken(tokens, token);
            currentPos += matcher.end();
        }

        return tokens.stream().toList();
    }

    protected static List<MarkdownToken> parseInnerTokens(MarkdownToken token){
        return parseMarkdown(token.text).stream().filter(t -> !t.hasNoMarkdown()).toList();
    }

    protected static void setTokenStyles(MarkdownToken token, MarkdownStyle style){
        switch (style){
            case UNDERLINED_ITALIC -> {
                token.underlined = true;
                token.italic = true;
            }
            case BOLD_ITALIC -> {
                token.bold = true;
                token.italic = true;
            }
            case ITALIC_underline, ITALIC_star -> token.italic = true;
            case UNDERLINED -> token.underlined = true;
            case BOLD -> token.bold = true;
            case STRIKETHROUGH -> token.strikethrough = true;
            case OBFUSCATED -> token.obfuscated = true;
        }
    }

    protected static void addToken(ArrayList<MarkdownToken> tokens, MarkdownToken token){
        token.text = replaceDoubleSpecialCharactersBack(unescapeSpecialCharacters(token.text));
        token.rawText = replaceDoubleSpecialCharactersBack(unescapeSpecialCharacters(token.rawText));
        tokens.add(token);
    }

    protected static void addTextPart(ArrayList<MarkdownToken> tokens, String textPart){
        if (!textPart.isEmpty())
            addToken(tokens, new MarkdownToken(textPart));
    }

    protected static String unescapeSpecialCharacters(String text) {
        return text.replaceAll("\\\\([*_~|@><])", "$1");
    }

    protected static String replaceDoubleSpecialCharacters(String text){
        return text.replaceAll("__(.+?)_(.+?)___", "►$1_$2_►")
                .replaceAll("___(.+?)_(.+?)__", "►_$1_$2►")
                .replaceAll("\\*\\*(.+?)\\*(.+?)\\*\\*\\*", "▬$1*$2*▬")
                .replaceAll("\\*\\*\\*(.+?)\\*(.+?)\\*\\*", "▬*$1*$2▬")
                .replaceAll("(?<!\\\\)_{3}(.+?)(?<!\\\\)_{3}", "►_$1_►")
                .replaceAll("(?<!\\\\)\\*{3}(.+?)(?<!\\\\)\\*{3}", "▬*$1*▬")
                .replaceAll("(?<!\\\\)\\*{2}(.+?)(?<!\\\\)\\*{2}", "▬$1▬")
                .replaceAll("(?<!\\\\)_{2}(.+?)(?<!\\\\)_{2}", "►$1►");
    }

    protected static String replaceDoubleSpecialCharactersBack(String text){
        return text.replace("▬", "**").replace("►", "__");
    }
}
