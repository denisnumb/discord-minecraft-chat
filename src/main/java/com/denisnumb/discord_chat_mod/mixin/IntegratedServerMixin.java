package com.denisnumb.discord_chat_mod.mixin;

import net.minecraft.client.server.IntegratedServer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.onIntegratedServerStarted;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Inject(method = "publishServer", at = @At("RETURN"))
    private void publishServer(GameType gameType, boolean allowCheats, int port, CallbackInfoReturnable<Boolean> cir){
        if (cir.getReturnValue())
            onIntegratedServerStarted();
    }
}
