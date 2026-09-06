package com.denisnumb.discord_chat_mod.markdown;

import com.denisnumb.discord_chat_mod.utils.MinecraftUtils;
import com.denisnumb.discord_chat_mod.discord.model.DiscordMentionData;
import net.minecraft.network.chat.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.denisnumb.discord_chat_mod.utils.ColorUtils.Color.CHAT_LINK_COLOR;

public class MarkdownToComponentConverter{
    private final MutableComponent result = Component.empty();
    private final List<MarkdownToken> tokens;
    private final Map<String, DiscordMentionData> mentions;

    public MarkdownToComponentConverter(List<MarkdownToken> tokens){
        this.tokens = tokens;
        this.mentions = Map.of();
    }

    public MarkdownToComponentConverter(List<MarkdownToken> tokens, Map<String, DiscordMentionData> mentions){
        this.tokens = tokens;
        this.mentions = mentions;
    }

    public MutableComponent convertMarkdownTokensToComponent() {
        for (MarkdownToken token : tokens)
            convertToken(token, false);

        return result;
    }

    public MutableComponent convertMarkdownTokensWithSpecialCharsToComponent() {
        for (MarkdownToken token : tokens)
            convertToken(token, true);

        return result;
    }

    private void addPart(MarkdownToken token, String textPart){
        MutableComponent component = Component.literal(textPart);

        if (mentions.containsKey(textPart)){
            DiscordMentionData mentionData = mentions.get(textPart);
            textPart = mentionData.prettyMention;
            component = MinecraftUtils.buildGradientComponent(textPart, mentionData.colors);

            if (mentionData.memberData != null){
                component = component.withStyle(style -> style
                        .withInsertion(mentionData.prettyMention)
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(mentionData.memberData.discordName)))
                );
            }
        }

        if (!textPart.isBlank()){
            String finalTextPart = textPart;
            component = component.withStyle(style -> {
                style = style.withBold(token.bold)
                        .withItalic(token.italic)
                        .withStrikethrough(token.strikethrough)
                        .withUnderlined(token.underlined)
                        .withObfuscated(token.obfuscated);

                if (token.obfuscated)
                    style = style.withHoverEvent(new HoverEvent.ShowText(Component.literal(finalTextPart)));

                if (token.isColored())
                    style = style.withColor(token.color);

                if (token.isUrl()){
                    String hoverValue = token.obfuscated ? String.format("%s (%s)", finalTextPart, token.url) : token.url;
                    style = style.withColor(CHAT_LINK_COLOR)
                            .withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverValue)));
                    try{
                        style = style.withClickEvent(new ClickEvent.OpenUrl(URI.create(token.url)));
                    } catch (IllegalArgumentException ignored) {}
                }

                return style;
            });
        }

        result.append(component);
    }

    private void convertToken(MarkdownToken token, boolean handleSpecialChars){
        if (token.getInnerTokens().isEmpty() || (handleSpecialChars && token.isSpecialCharacters))
            addPart(token, token.text);
        else {
            int currentPos = 0;

            for (MarkdownToken innerToken : token.getInnerTokens()){
                Matcher match = Pattern.compile(Pattern.quote(innerToken.rawText)).matcher(token.text.substring(currentPos));
                int startIndex = (match.find() ? match.start() : 0) + currentPos;

                if (startIndex > currentPos){
                    String textPart = token.text.substring(currentPos, startIndex);
                    addPart(token, textPart);
                }

                if (handleSpecialChars && innerToken.isSpecialCharacters){
                    addPart(innerToken, innerToken.rawText);
                    currentPos = startIndex;
                } else {
                    convertToken(innerToken, handleSpecialChars);
                    currentPos = startIndex + innerToken.rawText.length();
                }
            }

            if (currentPos < token.text.length()){
                String textPart = token.text.substring(currentPos);
                if (!textPart.isBlank())
                    addPart(token, textPart);
            }
        }
    }
}
