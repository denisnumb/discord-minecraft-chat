package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.discord.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.model.DiscordMemberData;
import com.denisnumb.discord_chat_mod.network.mentions.RequestDiscordMentionsPacket;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow @Final EditBox input;
    @Shadow private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow private static int getLastWordIndex(String p_93913_) { return 0; }
    @Shadow public abstract void showSuggestions(boolean p_93931_);


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
    }

    @Unique
    private static boolean discord_minecraft_chat$matchesPartial(DiscordMemberData member, String partial) {
        return member.guildNickname.toLowerCase().contains(partial)
                || member.discordNickName.toLowerCase().contains(partial)
                || member.discordName.toLowerCase().contains(partial);
    }

    @Unique
    private static final SuggestionProvider MENTIONS_PROVIDER = (context, builder) -> {
        PacketDistributor.sendToServer(new RequestDiscordMentionsPacket());
        String partial = builder.getRemaining().toLowerCase().substring(1);

        ChannelMembersProvider.clientMemberData.stream()
                .filter(data -> discord_minecraft_chat$matchesPartial(data, partial))
                .map(DiscordMemberData::getPrettyMention)
                .forEach(builder::suggest);

        return builder.buildFuture();
    };
}
