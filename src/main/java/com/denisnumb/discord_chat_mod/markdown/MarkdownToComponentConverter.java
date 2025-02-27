package com.denisnumb.discord_chat_mod.markdown;

import com.denisnumb.discord_chat_mod.discord.model.DiscordMemberData;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.denisnumb.discord_chat_mod.ColorUtils.Color.CHAT_LINK_COLOR;

public class MarkdownToComponentConverter{
    private final MutableComponent result = Component.empty();
    private final List<MarkdownToken> tokens;
    private final Map<String, DiscordMemberData> mentions;

    public MarkdownToComponentConverter(List<MarkdownToken> tokens){
        this(tokens, new HashMap<>());
    }

    public MarkdownToComponentConverter(List<MarkdownToken> tokens, Map<String, DiscordMemberData> mentions){
        this.tokens = tokens;
        this.mentions = mentions;
    }

    public MutableComponent convertMarkdownTokensToComponent() {
        for (MarkdownToken token : tokens)
            convertToken(token);

        return result;
    }

    private void addPart(MarkdownToken token, String textPart){
        MutableComponent component = Component.literal(textPart);

        if (mentions.containsKey(textPart)){
            String mentionString = textPart;
            DiscordMemberData member = mentions.get(mentionString);
            textPart = member.prettyMention;
            component = Component.literal(textPart).withStyle(style ->
                    style.withColor(TextColor.parseColor(member.color))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(member.discordNickName)))
                            .withInsertion("@" + member.guildNickname)
            );
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
                    style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(finalTextPart)));

                if (token.isUrl()){
                    String hoverValue = token.obfuscated ? String.format("%s (%s)", finalTextPart, token.url) : token.url;
                    style = style.withColor(CHAT_LINK_COLOR)
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, token.url))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hoverValue)));
                }

                return style;
            });
        }

        result.append(component);
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
}
