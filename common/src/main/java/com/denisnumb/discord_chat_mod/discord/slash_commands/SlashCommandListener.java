package com.denisnumb.discord_chat_mod.discord.slash_commands;

import com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry;
import com.denisnumb.discord_chat_mod.discord.ServerStatusController;
import com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider;
import com.denisnumb.discord_chat_mod.discord.chat_style.MessageType;
import com.denisnumb.discord_chat_mod.discord.model.DiscordGuildContext;
import com.denisnumb.discord_chat_mod.discord.slash_commands.permissions.SlashCommandPermissions;
import com.denisnumb.discord_chat_mod.locale.DiscordLocaleProvider;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.suggestion.Suggestions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.denisnumb.discord_chat_mod.ColorUtils.Color.DISCORD_GREEN_COLOR;
import static com.denisnumb.discord_chat_mod.ColorUtils.Color.DISCORD_RED_COLOR;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.LOGGER;
import static com.denisnumb.discord_chat_mod.DiscordChatMod.server;
import static com.denisnumb.discord_chat_mod.discord.chat_style.DiscordChatStyleProvider.getDiscordMessageComponents;
import static com.denisnumb.discord_chat_mod.discord.slash_commands.permissions.SlashCommandPermissionUtils.getMemberPermissions;
import static com.denisnumb.discord_chat_mod.discord.slash_commands.permissions.SlashCommandPermissionUtils.hasCommandPermission;

public class SlashCommandListener extends ListenerAdapter {
    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equals("cmd"))
            return;
        if (!event.getFocusedOption().getName().equals("command"))
            return;

        Guild guild = event.getGuild();
        if (guild == null)
            return;

        DiscordGuildContext ctx = DiscordChannelRegistry.getContext(guild.getId());
        String input = event.getFocusedOption().getValue();
        String commandName = input.strip()
                .split(" ")[0]
                .replace("/", "");

        if (ctx == null
                || !ctx.enableSlashCommands
                || !hasCommandPermission(event.getMember(), commandName)
        ) {
            event.replyChoices(List.of()).queue();
            return;
        }

        List<String> suggestions = getMinecraftSuggestions(input);

        List<Command.Choice> choices = suggestions.stream()
                .map(s -> new Command.Choice(s, s))
                .collect(Collectors.toList());

        event.replyChoices(choices).queue();
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        Guild guild = event.getGuild();

        if (guild != null){
            DiscordGuildContext ctx = DiscordChannelRegistry.getContext(guild.getId());
            if (ctx == null || !ctx.enableSlashCommands){
                event.replyEmbeds(new EmbedBuilder()
                                .setColor(DISCORD_RED_COLOR)
                                .setDescription(DiscordLocaleProvider.Discord.SlashCommands.commandsAreDisabled())
                                .build())
                        .setEphemeral(true)
                        .queue();
                return;
            }

            switch (event.getName()) {
                case "list" -> handleList(event);
                case "uptime" -> handleUptime(event);
                case "tps" -> handleTps(event);
                case "allowed_commands" -> handleAllowedCommands(event);
                case "cmd" -> handleCmd(event);
            }
        }
    }
    private void replyWithDiscordMessageComponents(
            SlashCommandInteractionEvent event,
            DiscordChatStyleProvider.DiscordMessageComponents components
    ){
        if (components.hasContentAndEmbed())
            event.reply(components.getContent()).addEmbeds(components.getEmbed()).setEphemeral(true).queue();
        else if (components.hasNoEmbed())
            event.reply(components.getContent()).setEphemeral(true).queue();
        else
            event.replyEmbeds(components.getEmbed()).setEphemeral(true).queue();
    }

    private void replyServerIsUnavailable(SlashCommandInteractionEvent event){
        replyWithDiscordMessageComponents(event, getDiscordMessageComponents(MessageType.PINNED_STATUS_UNAVAILABLE, Map.of()).orElseThrow());
    }

    private void handleList(SlashCommandInteractionEvent event) {
        if (server == null)
            replyServerIsUnavailable(event);
        else
            replyWithDiscordMessageComponents(event, ServerStatusController.createServerStatusMessageComponents());
    }

    private void handleUptime(SlashCommandInteractionEvent event) {
        if (server == null) {
            replyServerIsUnavailable(event);
            return;
        }

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        String formatted = formatDuration(uptimeMs);

        event.reply(DiscordLocaleProvider.Discord.SlashCommands.Uptime.success(formatted))
                .setEphemeral(true)
                .queue();
    }

    private void handleTps(SlashCommandInteractionEvent event) {
        MinecraftServer srv = server;
        if (srv == null) {
            replyServerIsUnavailable(event);
            return;
        }

        double mspt = srv.getCurrentSmoothedTickTime();
        double tps = Math.min(20.0, 1000.0 / mspt);
        String tpsDisplay = String.format("%.1f", tps);
        String msptDisplay = String.format("%.1f", mspt);

        String indicator;
        if (tps >= 19.0) indicator = "\uD83D\uDFE2"; // green
        else if (tps >= 15.0) indicator = "\uD83D\uDFE1"; // yellow
        else indicator = "\uD83D\uDD34"; // red

        event.reply(indicator + " **TPS:** " + tpsDisplay + "/20.0 (" + msptDisplay + " ms/tick)")
                .setEphemeral(true).queue();
    }

    private void handleAllowedCommands(SlashCommandInteractionEvent event) {
        MinecraftServer srv = server;
        if (srv == null) {
            replyServerIsUnavailable(event);
            return;
        }

        SlashCommandPermissions perms = getMemberPermissions(event.getMember());
        boolean allowAll = perms.allow().contains("*");
        boolean denyAll = perms.deny().contains("*");

        String result;
        int color = DISCORD_GREEN_COLOR;
        if (allowAll && !denyAll) {
            String denied = perms.deny().isEmpty()
                    ? ""
                    : DiscordLocaleProvider.Discord.SlashCommands.AllowedCommands.except() + formatCommands(perms.deny());
            result = DiscordLocaleProvider.Discord.SlashCommands.AllowedCommands.allAllowed() + denied;
        } else if (!perms.allow().isEmpty()) {
            result = DiscordLocaleProvider.Discord.SlashCommands.AllowedCommands.allowedList() + formatCommands(perms.allow());
        } else {
            result = DiscordLocaleProvider.Discord.SlashCommands.AllowedCommands.allDenied();
            color = DISCORD_RED_COLOR;
        }

        event.replyEmbeds(new EmbedBuilder().setColor(color).setDescription(result).build())
                .setEphemeral(true)
                .queue();
    }

    private static String formatCommands(List<String> commands) {
        return commands.stream()
                .map(cmd -> "`/" + cmd + "`")
                .collect(Collectors.joining(", "));
    }

    private void handleCmd(SlashCommandInteractionEvent event) {
        MinecraftServer srv = server;
        if (srv == null) {
            replyServerIsUnavailable(event);
            return;
        }

        String command = event.getOption("command").getAsString();
        String commandName = command.split(" ")[0].replace("/", "");

        if (!hasCommandPermission(event.getMember(), commandName)) {
            event.replyEmbeds(new EmbedBuilder()
                            .setColor(DISCORD_RED_COLOR)
                            .setDescription(DiscordLocaleProvider.Discord.SlashCommands.Cmd.Error.missingPermission())
                            .build()
                    )
                    .setEphemeral(true)
                    .queue();
            return;
        }

        event.deferReply(true).queue();

        srv.execute(() -> {
            try {
                CommandOutputCapture capture = new CommandOutputCapture();
                srv.getCommands().performPrefixedCommand(CommandOutputCapture.createSourceStack(srv, capture), command);
                String output = capture.getOutput();

                String response = DiscordLocaleProvider.Discord.SlashCommands.Cmd.success(command);
                if (!output.isEmpty()) {
                    if (output.length() > 1800) {
                        output = output.substring(0, 1800) + "\n. . .";
                    }
                    response += "\n```\n" + output + "\n```";
                }

                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                .setColor(DISCORD_GREEN_COLOR)
                                .setDescription(response)
                                .build()
                        ).queue();
            } catch (Exception e) {
                LOGGER.error("Error executing Discord /cmd: {}", command, e);
                event.getHook().editOriginalEmbeds(new EmbedBuilder()
                                .setColor(DISCORD_RED_COLOR)
                                .setDescription(DiscordLocaleProvider.Discord.SlashCommands.Cmd.Error.executeError(e.getMessage()))
                                .build())
                        .queue();
            }
        });
    }

    private static String formatDuration(long ms) {
        long seconds = ms / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        sb.append(secs).append("s");
        return sb.toString();
    }

    private List<String> getMinecraftSuggestions(String input) {
        MinecraftServer srv = server;
        if (srv == null)
            return List.of();

        CompletableFuture<List<String>> future = new CompletableFuture<>();

        srv.execute(() -> {
            try {
                CommandDispatcher<CommandSourceStack> dispatcher = srv.getCommands().getDispatcher();
                String rawInput = input.startsWith("/") ? input.substring(1) : input;

                ParseResults<CommandSourceStack> parsed = dispatcher.parse(rawInput, srv.createCommandSourceStack());

                Suggestions suggestions = dispatcher.getCompletionSuggestions(parsed)
                        .get(100, TimeUnit.MILLISECONDS);

                List<String> result = suggestions.getList().stream()
                        .map(s -> {
                            int start = s.getRange().getStart();
                            String prefix = rawInput.substring(0, start);
                            return prefix + s.getText();
                        })
                        .limit(25)
                        .collect(Collectors.toList());

                future.complete(result);
            } catch (Exception e) {
                future.complete(List.of());
            }
        });

        try {
            return future.get(500, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return List.of();
        }
    }
}
