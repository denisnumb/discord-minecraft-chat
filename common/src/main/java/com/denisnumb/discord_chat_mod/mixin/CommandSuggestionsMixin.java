package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.utils.ColorUtils;
import com.denisnumb.discord_chat_mod.chat_images.model.AbstractImage;
import com.denisnumb.discord_chat_mod.chat_images.model.AnimatedImage;
import com.denisnumb.discord_chat_mod.chat_images.model.Image;
import com.denisnumb.discord_chat_mod.config.ConfigProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.CustomEmojiProvider;
import com.denisnumb.discord_chat_mod.discord.data_providers.StickersProvider;
import com.denisnumb.discord_chat_mod.discord.model.DiscordUserData;
import com.denisnumb.discord_chat_mod.locale.MinecraftLocaleProvider;
import com.denisnumb.discord_chat_mod.network.emoji.DiscordEmojisTransceiver;
import com.denisnumb.discord_chat_mod.network.mentions.DiscordMentionsTransceiver;
import com.denisnumb.discord_chat_mod.network.sticker.DiscordStickersTransceiver;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.vdurmont.emoji.Emoji;
import com.vdurmont.emoji.EmojiManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final EditBox input;
    @Shadow @Final Font font;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private static int getLastWordIndex(String p_93913_) { return 0; }
    @Shadow public abstract void showSuggestions(boolean p_93931_);
    @Shadow private CommandSuggestions.SuggestionsList suggestions;
    @Shadow @Final int fillColor;

    @Inject(
            method = "renderSuggestions",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/CommandSuggestions$SuggestionsList;render(Lnet/minecraft/client/gui/GuiGraphics;II)V",
                    shift = At.Shift.AFTER
            ),
            require = 0
    )
    private void renderExtras(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfoReturnable<Boolean> cir) {
        if (this.suggestions == null)
            return;

        Rect2i rect = ((SuggestionsListAccessorMixin) this.suggestions).getRect();
        List<Suggestion> suggestionList = ((SuggestionsListAccessorMixin) this.suggestions).getSuggestionsList();
        int current = ((SuggestionsListAccessorMixin) this.suggestions).getCurrent();
        Suggestion currentSuggestion = suggestionList.get(current);

        if (input.getValue().startsWith("/send_sticker")){
            discord_chat_mod$renderStickerPreview(guiGraphics, rect, currentSuggestion);
            return;
        }

        Integer color = ColorUtils.colorNameToInt.get(currentSuggestion.getText().replaceAll("[<>]", ""));
        if (color != null)
            discord_chat_mod$renderColors(guiGraphics, rect, color | 0xFF000000);
    }

    @Unique
    private void discord_chat_mod$renderColors(GuiGraphics guiGraphics, Rect2i rect, Integer color){
        String text = MinecraftLocaleProvider.coloredTextExample().getString();
        int boxHeight = font.lineHeight + 4;
        int boxWidth = font.width(text) + 4;
        int boxX = rect.getX() + rect.getWidth() + 2;
        int boxY = rect.getY() + rect.getHeight() - boxHeight;
        guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, fillColor);
        guiGraphics.drawString(font, text, boxX + 2, boxY + 2, color, false);
    }

    @Unique
    private void discord_chat_mod$renderStickerPreview(GuiGraphics guiGraphics, Rect2i rect, Suggestion currentSuggestion) {
        DiscordStickersTransceiver.requestDiscordStickers();
        AbstractImage image = StickersProvider.CLIENT_STICKER_CACHE.get(currentSuggestion.getText());

        if (image == null)
            return;

        Identifier imageLocation = image instanceof AnimatedImage animatedImage
                ? animatedImage.getCurrentFrame()
                : ((Image) image).resourceLocation;

        if (imageLocation == null)
            return;

        int boxSize = 75;
        int boxX = rect.getX() + rect.getWidth() + 2;
        int boxY = rect.getY() + rect.getHeight() - boxSize;

        guiGraphics.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, fillColor);

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED, imageLocation,
                boxX + 2, boxY + 2,
                0, 0,
                boxSize - 4, boxSize - 4,
                boxSize - 4, boxSize - 4
        );
    }


    @Inject(
            method = "updateCommandInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/CommandSuggestions;getLastWordIndex(Ljava/lang/String;)I",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void suggestMentions(CallbackInfo ci) throws CommandSyntaxException {
        String currentInput = input.getValue().substring(0, input.getCursorPosition());
        int lastWordIndex = getLastWordIndex(currentInput);

        if (currentInput.substring(lastWordIndex).startsWith("@")){
            this.pendingSuggestions = MENTIONS_PROVIDER.getSuggestions(null, new SuggestionsBuilder(currentInput, lastWordIndex));
            showSuggestions(true);
            if (!pendingSuggestions.join().isEmpty())
                ci.cancel();
        }

        if (!ConfigProvider.getConfig().isEmojifulCompatibilityEnabled()
                && currentInput.substring(lastWordIndex).startsWith(":")){
            this.pendingSuggestions = EMOJIS_PROVIDER.getSuggestions(null, new SuggestionsBuilder(currentInput, lastWordIndex));
            showSuggestions(true);
            if (!pendingSuggestions.join().isEmpty())
                ci.cancel();
        }

        if (currentInput.substring(lastWordIndex).startsWith("<")){
            this.pendingSuggestions = COLORS_PROVIDER.getSuggestions(null, new SuggestionsBuilder(currentInput, lastWordIndex));
            showSuggestions(true);
            if (!pendingSuggestions.join().isEmpty())
                ci.cancel();
        }
    }


    @Unique
    private static final SuggestionProvider<String> COLORS_PROVIDER = (context, builder) -> {
        String partial = builder.getRemaining().toLowerCase();

        Stream<String> colorNames = ColorUtils.colorNameToInt.keySet().stream()
                .map(colorName -> "<" + colorName + ">")
                .filter(colorName -> colorName.toLowerCase().startsWith(partial));

        colorNames.forEach(builder::suggest);

        return builder.buildFuture();
    };

    @Unique
    private static final SuggestionProvider<String> EMOJIS_PROVIDER = (context, builder) -> {
        DiscordEmojisTransceiver.requestDiscordEmojis();

        String partial = builder.getRemaining().toLowerCase().substring(1);

        Stream<String> customEmojis = CustomEmojiProvider.CLIENT_EMOJI_CACHE.keySet().stream()
                .map(emojiName -> ":" + emojiName + ":")
                .filter(emojiName -> emojiName.toLowerCase().contains(partial));

        Stream<String> unicodeEmojis = EmojiManager.getAll().stream()
                .filter(emoji -> emoji.getAliases().stream().anyMatch(alias -> alias.contains(partial)))
                .map(Emoji::getUnicode);

        Stream.concat(customEmojis, unicodeEmojis).forEach(builder::suggest);

        return builder.buildFuture();
    };

    @Unique
    private static boolean discord_minecraft_chat$matchesPartial(DiscordUserData member, String partial) {
        return member.displayName.toLowerCase().contains(partial)
                || member.discordName.toLowerCase().contains(partial);
    }

    @Unique
    private static final SuggestionProvider<String> MENTIONS_PROVIDER = (context, builder) -> {
        DiscordMentionsTransceiver.requestDiscordMemberData();

        String partial = builder.getRemaining().toLowerCase().substring(1);
        StringRange range = StringRange.between(builder.getStart(), builder.getInput().length());

        List<Suggestion> suggestions = ChannelMembersProvider.CLIENT_MEMBER_CACHE.stream()
                .filter(data -> discord_minecraft_chat$matchesPartial(data, partial))
                .map(data -> new Suggestion(range, data.getPrettyMention()))
                .toList();

        return CompletableFuture.completedFuture(new Suggestions(range, suggestions));
    };
}
