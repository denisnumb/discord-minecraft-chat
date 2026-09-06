package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.utils.ColorUtils;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ARGB.class)
public class ARGBMixin {
    @Inject(method = "color(FI)I", at = @At("HEAD"), cancellable = true)
    private static void skipTransparentTagColor(float alpha, int color, CallbackInfoReturnable<Integer> cir) {
        if (color == ColorUtils.Color.TRANSPARENT_IMAGE_TAG_COLOR)
            cir.setReturnValue(0);
    }
}
