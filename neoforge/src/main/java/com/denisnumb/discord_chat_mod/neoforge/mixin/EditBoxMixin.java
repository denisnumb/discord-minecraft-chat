package com.denisnumb.discord_chat_mod.neoforge.mixin;

import com.denisnumb.discord_chat_mod.markdown.MarkdownEditBoxParser;
import com.denisnumb.discord_chat_mod.markdown.MarkdownToComponentConverter;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.denisnumb.discord_chat_mod.utils.MinecraftUtils.subFormattedCharSequence;

@Mixin(EditBox.class)
public abstract class EditBoxMixin {
    @Shadow @Final private Font font;
    @Shadow private String value;
    @Shadow private int displayPos;
    @Shadow private int cursorPos;
    @Shadow public abstract int getInnerWidth();

    @Unique
    private FormattedCharSequence discord_chat_mod$cachedMarkdown = null;
    @Unique
    private boolean discord_chat_mod$markdownValid = false;
    @Unique
    private String discord_chat_mod$lastInput = "";

    @Inject(
            method = "renderWidget",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/lang/String;isEmpty()Z",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void prepareMarkdown(GuiGraphics guiGraphics, int i, int j, float f, CallbackInfo ci, @Local String string) {
        if (!string.isEmpty() && !value.startsWith("/")) {
            if (!string.equals(discord_chat_mod$lastInput)){
                discord_chat_mod$lastInput = string;
                Component markdown = new MarkdownToComponentConverter(
                        MarkdownEditBoxParser.parseMarkdown(string)
                ).convertMarkdownTokensWithSpecialCharsToComponent();

                this.discord_chat_mod$markdownValid = markdown.getString().equals(string);
                this.discord_chat_mod$cachedMarkdown = this.discord_chat_mod$markdownValid ? markdown.getVisualOrderText() : null;
            }
        } else {
            this.discord_chat_mod$markdownValid = false;
            this.discord_chat_mod$cachedMarkdown = null;
        }
    }

    @ModifyExpressionValue(
            method = "renderWidget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/EditBox;applyFormat(Ljava/lang/String;I)Lnet/minecraft/util/FormattedCharSequence;",
                    ordinal = 0
            ),
            require = 0
    )
    public FormattedCharSequence modifyBeforeCursorDrawString(FormattedCharSequence formattedCharSequence){
        String currentInput = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
        int stringLenBeforeCursor = this.cursorPos - this.displayPos;
        boolean cursorInBounds = stringLenBeforeCursor >= 0 && stringLenBeforeCursor <= currentInput.length();

        return discord_chat_mod$markdownValid
                ? cursorInBounds
                ? subFormattedCharSequence(discord_chat_mod$cachedMarkdown, 0, stringLenBeforeCursor)
                : discord_chat_mod$cachedMarkdown
                : formattedCharSequence;
    }

    @ModifyArg(
            method = "renderWidget",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
                    ordinal = 1
            ),
            require = 0
    )
    public FormattedCharSequence modifyAfterCursorDrawString(FormattedCharSequence formattedCharSequence){
        String currentInput = this.font.plainSubstrByWidth(this.value.substring(this.displayPos), this.getInnerWidth());
        int stringLenBeforeCursor = this.cursorPos - this.displayPos;

        return discord_chat_mod$markdownValid
                ? subFormattedCharSequence(discord_chat_mod$cachedMarkdown, stringLenBeforeCursor, currentInput.length())
                : formattedCharSequence;
    }
}
