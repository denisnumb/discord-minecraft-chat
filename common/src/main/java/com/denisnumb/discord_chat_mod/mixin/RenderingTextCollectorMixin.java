package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.chat_images.model.AbstractImage;
import com.denisnumb.discord_chat_mod.chat_images.model.AnimatedImage;
import com.denisnumb.discord_chat_mod.chat_images.model.Image;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.CustomEmojiProvider;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import static com.denisnumb.discord_chat_mod.utils.EmojiUtils.EMOJI_PATTERN;
import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.subFormattedCharSequence;

@Mixin(targets = "net.minecraft.client.gui.GuiGraphics$RenderingTextCollector")
public abstract class RenderingTextCollectorMixin {
    @Shadow @Final GuiGraphics field_63856;

    @ModifyArgs(
            method = "accept(Lnet/minecraft/client/gui/TextAlignment;IILnet/minecraft/client/gui/ActiveTextCollector$Parameters;Lnet/minecraft/util/FormattedCharSequence;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/state/GuiTextRenderState;<init>(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;Lorg/joml/Matrix3x2fc;IIIIZZLnet/minecraft/client/gui/navigation/ScreenRectangle;)V"
            )
    )
    private void handleCustomDiscordEmojis(Args args) {
        Font font = args.get(0);
        FormattedCharSequence text = args.get(1);
        int x = args.get(3);
        int y = args.get(4);

        if (text == null || text == FormattedCharSequence.EMPTY)
            return;

        List<Integer> codepoints = new ArrayList<>();
        text.accept((index, style, codePoint) -> {
            codepoints.add(codePoint);
            return true;
        });

        if (codepoints.isEmpty())
            return;

        StringBuilder rawBuilder = new StringBuilder();
        for (int cp : codepoints)
            rawBuilder.appendCodePoint(cp);

        String rawText = rawBuilder.toString();
        Matcher matcher = EMOJI_PATTERN.matcher(rawText);

        if (ConfigProvider.getConfig().isEmojifulCompatibilityEnabled()
                || !matcher.find()
                || CustomEmojiProvider.CLIENT_EMOJI_CACHE.isEmpty()) {
            return;
        }

        List<FormattedCharSequence> result = new ArrayList<>();
        int currentX = x;
        int lastEnd = 0;

        do {
            if (matcher.start() > lastEnd) {
                FormattedCharSequence before = subFormattedCharSequence(text, lastEnd, matcher.start());
                result.add(before);
                currentX += font.width(before) + 1;
            }

            AbstractImage abstractImage = CustomEmojiProvider.CLIENT_EMOJI_CACHE.get(matcher.group(1));

            if (abstractImage != null) {

                Identifier emoji =
                        abstractImage instanceof AnimatedImage gif
                                ? gif.getCurrentFrame()
                                : ((Image) abstractImage).resourceLocation;

                int emojiSize = 9;
                this.field_63856.blit(RenderPipelines.GUI_TEXTURED, emoji, currentX, y - 1, 0, 0, emojiSize, emojiSize, emojiSize, emojiSize);
                result.add(FormattedCharSequence.codepoint(' ', Style.EMPTY)); // space for emoji; width = 4
                result.add(FormattedCharSequence.codepoint(' ', Style.EMPTY.withBold(true))); // space for emoji; width = 5
                currentX += emojiSize;

            } else {
                FormattedCharSequence notFound = subFormattedCharSequence(text, matcher.start(), matcher.end());
                result.add(notFound);
                currentX += font.width(notFound) + 1;
            }

            lastEnd = matcher.end();

        } while (matcher.find());

        if (lastEnd < rawText.length())
            result.add(subFormattedCharSequence(text, lastEnd, rawText.length()));

        args.set(1, FormattedCharSequence.composite(result));
    }
}
