package com.denisnumb.discord_chat_mod.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownParser{
    public static List<MarkdownToken> parseMarkdown(String rawText){
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

            if (style == MarkdownStyle.URL){
                token = new MarkdownToken(matchedText);
                token.url = matchedText;
            } else if (style == MarkdownStyle.LINK) {
                token = new MarkdownToken(matchedText, innerText);
                token.url = matcher.group(2);
            } else if (style == MarkdownStyle.DISCORD_MENTION){
                token = new MarkdownToken(matchedText);
                token.isMention = true;
            } else {
                token = new MarkdownToken(matchedText, innerText);
                setTokenStyles(token, style);
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

    private static List<MarkdownToken> parseInnerTokens(MarkdownToken token){
        return parseMarkdown(token.text).stream().filter(t -> !t.hasNoMarkdown()).toList();
    }

    private static void setTokenStyles(MarkdownToken token, MarkdownStyle style){
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

    private static void addToken(ArrayList<MarkdownToken> tokens, MarkdownToken token){
        token.text = replaceDoubleSpecialCharactersBack(unescapeSpecialCharacters(token.text));
        token.rawText = replaceDoubleSpecialCharactersBack(unescapeSpecialCharacters(token.rawText));
        tokens.add(token);
    }

    private static void addTextPart(ArrayList<MarkdownToken> tokens, String textPart){
        if (!textPart.isEmpty())
            addToken(tokens, new MarkdownToken(textPart));
    }

    private static String unescapeSpecialCharacters(String text) {
        return text.replaceAll("\\\\([*_~|@><])", "$1");
    }

    private static String replaceDoubleSpecialCharacters(String text){
        return text.replaceAll("__(.+?)_(.+?)___", "►$1_$2_►")
                .replaceAll("___(.+?)_(.+?)__", "►_$1_$2►")
                .replaceAll("\\*\\*(.+?)\\*(.+?)\\*\\*\\*", "▬$1*$2*▬")
                .replaceAll("\\*\\*\\*(.+?)\\*(.+?)\\*\\*", "▬*$1*$2▬")
                .replaceAll("(?<!\\\\)_{3}(.+?)(?<!\\\\)_{3}", "►_$1_►")
                .replaceAll("(?<!\\\\)\\*{3}(.+?)(?<!\\\\)\\*{3}", "▬*$1*▬")
                .replaceAll("(?<!\\\\)\\*{2}(.+?)(?<!\\\\)\\*{2}", "▬$1▬")
                .replaceAll("(?<!\\\\)_{2}(.+?)(?<!\\\\)_{2}", "►$1►");
    }

    private static String replaceDoubleSpecialCharactersBack(String text){
        return text.replace("▬", "**").replace("►", "__");
    }
}
