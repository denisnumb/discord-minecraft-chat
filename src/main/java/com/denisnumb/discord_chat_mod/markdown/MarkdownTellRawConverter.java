package com.denisnumb.discord_chat_mod.markdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MarkdownTellRawConverter{
    private final ArrayList<TellRawTextComponent> result = new ArrayList<>();
    private final List<MarkdownToken> tokens;
    private final Map<String, DiscordMentionData> mentions;

    public MarkdownTellRawConverter(List<MarkdownToken> tokens){
        this(tokens, new HashMap<>());
    }

    public MarkdownTellRawConverter(List<MarkdownToken> tokens, Map<String, DiscordMentionData> mentions){
        this.tokens = tokens;
        this.mentions = mentions;
    }

    public List<TellRawTextComponent> convertMarkdownTokensToTellRaw() {
        for (MarkdownToken token : tokens)
            convertToken(token);

        return result.stream().toList();
    }

    private void addPart(MarkdownToken token, String textPart){
        TellRawTextComponent part = new TellRawTextComponent(textPart);

        if (mentions.containsKey(textPart)){
            part.text = mentions.get(textPart).prettyMention;
            part.color = mentions.get(textPart).color;
            textPart = part.text;
        }

        if (!textPart.isBlank()){
            setPartStyles(part, token);

            if (token.obfuscated)
                part.hoverEvent = new TellRawTextComponentEvent("show_text", textPart);

            if (token.isUrl()){
                part.color = "aqua";
                part.clickEvent = new TellRawTextComponentEvent("open_url", token.url);
                String hoverValue = token.obfuscated ? String.format("%s (%s)", textPart, token.url) : token.url;
                part.hoverEvent = new TellRawTextComponentEvent("show_text", hoverValue);
            }
        }

        result.add(part);
    }

    private void convertToken(MarkdownToken token){
        if (token.getInnerTokens().isEmpty())
            addPart(token, token.text);
        else {
            int currentPos = 0;

            for (MarkdownToken innerToken : token.getInnerTokens()){
                Matcher match = Pattern.compile(Pattern.quote(innerToken.rawText)).matcher(token.text.substring(currentPos));
                int startIndex = (match.find() ? match.start() : 0) + currentPos;

                if (startIndex > currentPos){
                    String textPart = token.text.substring(currentPos, startIndex).replaceAll("^[_*~|]+|[_*~|]+$", "");
                    addPart(token, textPart);
                }

                convertToken(innerToken);
                currentPos = startIndex + innerToken.rawText.length();
            }

            if (currentPos < token.text.length()){
                String textPart = token.text.substring(currentPos).replaceAll("^[_*~|]+|[_*~|]+$", "");
                if (!textPart.isBlank())
                    addPart(token, textPart);
            }
        }
    }

    private static void setPartStyles(TellRawTextComponent part, MarkdownToken token){
        if (token.bold) part.bold = true;
        if (token.italic) part.italic = true;
        if (token.strikethrough) part.strikethrough = true;
        if (token.underlined) part.underlined = true;
        if (token.obfuscated) part.obfuscated = true;
    }
}