package com.denisnumb.discord_chat_mod.markdown;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.denisnumb.discord_chat_mod.utils.ColorUtils.parseColor;

public class MarkdownEditBoxParser extends MarkdownParser {
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

            String linkString = null;
            String colorString = null;
            MarkdownToken token;

            switch (style){
                case MarkdownStyle.URL -> {
                    token = new MarkdownToken(matchedText);
                    token.url = matchedText;
                }
                case MarkdownStyle.LINK -> {
                    token = new MarkdownToken(matchedText, innerText);
                    token.url = matcher.group(2);
                    linkString = token.url;
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
                        colorString = matcher.group(1);
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

            addSpecialCharactersToken(tokens, style, colorString, linkString, false);
            addToken(tokens, token);
            addSpecialCharactersToken(tokens, style, colorString, linkString, true);
            currentPos += matcher.end();
        }

        return tokens.stream().toList();
    }

    protected static List<MarkdownToken> parseInnerTokens(MarkdownToken token){
        return parseMarkdown(token.text).stream().filter(t -> !t.hasNoMarkdown()).toList();
    }

    private static void addSpecialCharactersToken(
            ArrayList<MarkdownToken> tokens,
            MarkdownStyle style,
            @Nullable String colorString,
            String linkString,
            boolean isClosing
    ) {
        MarkdownToken token = null;
        switch (style) {
            case UNDERLINED_ITALIC -> token = new MarkdownToken("___");
            case BOLD_ITALIC -> token = new MarkdownToken("***");
            case ITALIC_underline -> token = new MarkdownToken("_");
            case ITALIC_star -> token = new MarkdownToken("*");
            case UNDERLINED -> token = new MarkdownToken("__");
            case BOLD -> token = new MarkdownToken("**");
            case STRIKETHROUGH -> token = new MarkdownToken("~~");
            case OBFUSCATED -> token = new MarkdownToken("||");
            case LINK -> token = isClosing
                    ? new MarkdownToken(String.format("](%s)", linkString))
                    : new MarkdownToken("[");
            case COLOR_RANGE -> {
                if (colorString != null)
                    token = isClosing
                            ? new MarkdownToken(String.format("<%s/>", colorString))
                            : new MarkdownToken(String.format("<%s>", colorString));
            }
            case COLOR_SINGLE -> {
                if (!isClosing && colorString != null)
                    token = new MarkdownToken(String.format("<%s/>", colorString));
            }
            case COLOR_OPEN -> {
                if (!isClosing && colorString != null)
                    token = new MarkdownToken(String.format("<%s>", colorString));
            }
        }

        if (token != null){
            token.color = 0x696969;
            token.isSpecialCharacters = true;
            addToken(tokens, token);
        }
    }

    protected static void addTextPart(ArrayList<MarkdownToken> tokens, String textPart) {
        if (!textPart.isEmpty())
            addToken(tokens, new MarkdownToken(textPart));
    }

    protected static void addToken(ArrayList<MarkdownToken> tokens, MarkdownToken token){
        token.text = replaceDoubleSpecialCharactersBack(token.text);
        token.rawText = replaceDoubleSpecialCharactersBack(token.rawText);
        tokens.add(token);
    }
}