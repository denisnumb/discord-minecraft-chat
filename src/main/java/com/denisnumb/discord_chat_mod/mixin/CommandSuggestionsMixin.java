package com.denisnumb.discord_chat_mod.mixin;

import com.denisnumb.discord_chat_mod.discord.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.model.DiscordMemberData;
import com.denisnumb.discord_chat_mod.network.ModNetworking;
import com.denisnumb.discord_chat_mod.network.RequestDiscordMentionsPacket;
import com.google.common.base.Strings;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow
    @Final
    EditBox input;

    @Shadow
    private ParseResults<SharedSuggestionProvider> currentParse;

    @Shadow
    boolean keepSuggestions;

    @Shadow
    private CommandSuggestions.SuggestionsList suggestions;

    @Shadow
    @Final
    private List<FormattedCharSequence> commandUsage;

    @Shadow
    @Final
    private boolean commandsOnly;

    @Shadow
    @Final
    Minecraft minecraft;

    @Shadow
    @Final
    private boolean onlyShowIfCursorPastError;

    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow
    private void updateUsageInfo(){}

    @Shadow
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");

    @Shadow
    private static int getLastWordIndex(String p_93913_) {
        if (Strings.isNullOrEmpty(p_93913_)) {
            return 0;
        } else {
            int $$1 = 0;
            for(Matcher $$2 = WHITESPACE_PATTERN.matcher(p_93913_); $$2.find(); $$1 = $$2.end()) {}

            return $$1;
        }
    }

    @Shadow public abstract void showSuggestions(boolean p_93931_);


    /**
     * @author denisnumb
     * @reason for implement discord @mentions within game chat
     */
    @Overwrite
    public void updateCommandInfo() throws CommandSyntaxException {
        String inputValue = this.input.getValue();
        if (this.currentParse != null && !this.currentParse.getReader().getString().equals(inputValue)) {
            this.currentParse = null;
        }

        if (!this.keepSuggestions) {
            this.input.setSuggestion((String)null);
            this.suggestions = null;
        }

        this.commandUsage.clear();
        StringReader stringReader = new StringReader(inputValue);
        boolean isCommand = stringReader.canRead() && stringReader.peek() == '/';
        if (isCommand) {
            stringReader.skip();
        }

        isCommand = this.commandsOnly || isCommand;
        int cursorPosition = this.input.getCursorPosition();
        if (isCommand) {
            CommandDispatcher<SharedSuggestionProvider> commands = this.minecraft.player.connection.getCommands();
            if (this.currentParse == null) {
                this.currentParse = commands.parse(stringReader, this.minecraft.player.connection.getSuggestionsProvider());
            }

            int $$6 = this.onlyShowIfCursorPastError ? stringReader.getCursor() : 1;
            if (cursorPosition >= $$6 && (this.suggestions == null || !this.keepSuggestions)) {
                this.pendingSuggestions = commands.getCompletionSuggestions(this.currentParse, cursorPosition);
                this.pendingSuggestions.thenRun(() -> {
                    if (this.pendingSuggestions.isDone()) {
                        this.updateUsageInfo();
                    }
                });
            }
        } else {
            String currentInput = inputValue.substring(0, cursorPosition);
            int lastWordIndex = getLastWordIndex(currentInput);

            if (currentInput.substring(lastWordIndex).startsWith("@")){
                this.pendingSuggestions = MENTIONS_PROVIDER.getSuggestions(null, new SuggestionsBuilder(currentInput, lastWordIndex));
                showSuggestions(true);
                if (!pendingSuggestions.join().isEmpty())
                    return;
            }

            Collection<String> suggestions = this.minecraft.player.connection.getSuggestionsProvider().getCustomTabSugggestions();
            this.pendingSuggestions = SharedSuggestionProvider.suggest(suggestions, new SuggestionsBuilder(currentInput, lastWordIndex));
        }
    }

    @Unique
    private static boolean matchesPartial(DiscordMemberData member, String partial) {
        return member.guildNickname.toLowerCase().contains(partial)
                || member.discordNickName.toLowerCase().contains(partial)
                || member.discordName.toLowerCase().contains(partial);
    }

    private static final SuggestionProvider MENTIONS_PROVIDER = (context, builder) -> {
        ModNetworking.sendToServer(new RequestDiscordMentionsPacket());
        String partial = builder.getRemaining().toLowerCase().substring(1);

        ChannelMembersProvider.clientMemberData.stream()
                .filter(data -> matchesPartial(data, partial))
                .map(DiscordMemberData::getPrettyMention)
                .forEach(builder::suggest);

        return builder.buildFuture();
    };
}
