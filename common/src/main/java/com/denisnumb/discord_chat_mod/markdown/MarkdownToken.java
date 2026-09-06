package com.denisnumb.discord_chat_mod.markdown;

import java.util.List;

import static com.denisnumb.discord_chat_mod.utils.ColorUtils.getHexColor;

public class MarkdownToken {
    public String rawText;
    public String text;
    public String url = null;
    public boolean bold = false;
    public boolean italic = false;
    public boolean underlined = false;
    public boolean strikethrough = false;
    public boolean obfuscated = false;
    public boolean isMention = false;
    public Integer color = null;
    public boolean isSpecialCharacters = false;
    private List<MarkdownToken> innerTokens;

    public MarkdownToken(String rawText){
        this(rawText, rawText);
    }

    public MarkdownToken(String rawText, String text){
        this.rawText = rawText;
        this.text = text;
    }

    public boolean isUrl(){
        return url != null && !url.isEmpty();
    }

    public boolean isColored() {
        return color != null;
    }

    public boolean hasNoMarkdown(){
        return !(isUrl()
                || bold
                || italic
                || underlined
                || strikethrough
                || obfuscated
                || isMention
                || isColored()
                || isSpecialCharacters
        );
    }

    public List<MarkdownToken> getInnerTokens(){
        return innerTokens != null ? innerTokens : List.of();
    }

    public void setInnerTokens(List<MarkdownToken> tokens){
        innerTokens = tokens;
        updateStyles();
    }

    public void updateStyles(){
        for (MarkdownToken innerToken : getInnerTokens()){
            innerToken.combineStyles(this);
            innerToken.updateStyles();
        }
    }

    public void combineStyles(MarkdownToken another){
        if (isSpecialCharacters)
            return;

        url = isUrl() ? url : another.url;
        bold |= another.bold;
        italic |= another.italic;
        underlined |= another.underlined;
        strikethrough |= another.strikethrough;
        obfuscated |= another.obfuscated;
        color = isColored() ? color : another.color;
    }

    public String toString(){
        if (isSpecialCharacters)
            return rawText;

        StringBuilder result = new StringBuilder("[");

        result.append(String.format("rawText=\"%s\", text=\"%s\"", rawText, text));
        if (isUrl()) result.append(String.format(", url=\"%s\"", url));
        if (bold) result.append(", bold");
        if (italic) result.append(", italic");
        if (underlined) result.append(", underlined");
        if (strikethrough) result.append(", strikethrough");
        if (obfuscated) result.append(", obfuscated");
        if (isMention) result.append(", isMention");
        if (isColored()) result.append(String.format(", color=\"%s\"", getHexColor(color)));
        result.append("]");

        if (!getInnerTokens().isEmpty()){
            for (MarkdownToken innerToken : getInnerTokens())
                result.append("\n\t").append(innerToken.toString());
        }

        return result.toString();
    }
}
