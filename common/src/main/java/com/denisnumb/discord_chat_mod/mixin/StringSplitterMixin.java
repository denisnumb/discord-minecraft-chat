package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.chat_images.model.AbstractImage;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.CustomEmojiProvider;
import net.minecraft.client.StringSplitter;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.denisnumb.discord_chat_mod.utils.EmojiUtils.EMOJI_PATTERN;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.subFormattedCharSequence;

@Mixin(StringSplitter.class)
public abstract class StringSplitterMixin {
    @ModifyVariable(
            method = "stringWidth(Ljava/lang/String;)F",
            at = @At("HEAD"),
            argsOnly = true
    )
    private String handleDiscordCustomEmoji(String text){
        if (text == null)
            return null;

        Matcher matcher = EMOJI_PATTERN.matcher(text);

        if (ConfigProvider.getConfig().isEmojifulCompatibilityEnabled()
                || !matcher.find()
                || CustomEmojiProvider.CLIENT_EMOJI_CACHE.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        do {
            if (matcher.start() > lastEnd)
                result.append(text, lastEnd, matcher.start());

            AbstractImage abstractImage = CustomEmojiProvider.CLIENT_EMOJI_CACHE.get(matcher.group(1));

            if (abstractImage != null)
                result.append("  "); // space for emoji
            else
                result.append(text, matcher.start(), matcher.end());

            lastEnd = matcher.end();
        } while (matcher.find());

        if (lastEnd < text.length())
            result.append(text, lastEnd, text.length());

        return result.toString();
    }

    @ModifyVariable(
            method = "stringWidth(Lnet/minecraft/util/FormattedCharSequence;)F",
            at = @At("HEAD"),
            argsOnly = true
    )
    private FormattedCharSequence handleDiscordCustomEmoji(FormattedCharSequence text) {
        List<Integer> codepoints = new ArrayList<>();
        text.accept((index, style, codePoint) -> {
            codepoints.add(codePoint);
            return true;
        });

        if (codepoints.isEmpty())
            return text;

        StringBuilder rawBuilder = new StringBuilder();
        for (int cp : codepoints)
            rawBuilder.appendCodePoint(cp);

        String rawText = rawBuilder.toString();
        Matcher matcher = EMOJI_PATTERN.matcher(rawText);

        if (ConfigProvider.getConfig().isEmojifulCompatibilityEnabled()
                || !matcher.find()
                || CustomEmojiProvider.CLIENT_EMOJI_CACHE.isEmpty()) {
            return text;
        }

        List<FormattedCharSequence> result = new ArrayList<>();
        int lastEnd = 0;

        do {
            if (matcher.start() > lastEnd)
                result.add(subFormattedCharSequence(text, lastEnd, matcher.start()));

            AbstractImage abstractImage = CustomEmojiProvider.CLIENT_EMOJI_CACHE.get(matcher.group(1));

            if (abstractImage != null) {
                result.add(FormattedCharSequence.codepoint(' ', Style.EMPTY)); // space for emoji; width = 4
                result.add(FormattedCharSequence.codepoint(' ', Style.EMPTY.withBold(true))); // space for emoji; width = 5
            } else
                result.add(subFormattedCharSequence(text, matcher.start(), matcher.end()));

            lastEnd = matcher.end();

        } while (matcher.find());

        if (lastEnd < rawText.length())
            result.add(subFormattedCharSequence(text, lastEnd, rawText.length()));

        return FormattedCharSequence.composite(result);
    }
}
