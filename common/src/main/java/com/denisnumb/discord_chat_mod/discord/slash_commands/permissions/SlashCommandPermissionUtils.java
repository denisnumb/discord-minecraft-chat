package com.denisnumb.discord_chat_mod.discord.slash_commands.permissions;

import com.denisnumb.discord_chat_mod.config.configs.DiscordGuildsConfig;
import com.denisnumb.discord_chat_mod.discord.DiscordChannelRegistry;
import com.denisnumb.discord_chat_mod.discord.model.DiscordGuildContext;
import net.dv8tion.jda.api.entities.Member;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class SlashCommandPermissionUtils {
    private SlashCommandPermissionUtils() {}

    public static SlashCommandPermissions getMemberPermissions(Member member) {
        List<SlashCommandPermissions> effectivePerms = getEffectivePermissions(member);

        Set<String> knownCommands = effectivePerms.stream()
                .flatMap(p -> Stream.concat(p.allow().stream(), p.deny().stream()))
                .filter(cmd -> !cmd.equals("*"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        SlashCommandPermissions.CanRunState starState = SlashCommandPermissions.CanRunState.NOT_SET;
        for (SlashCommandPermissions perms : effectivePerms) {
            if (perms.deny().contains("*")) {
                starState = SlashCommandPermissions.CanRunState.DENY;
                break;
            }
            if (perms.allow().contains("*")) {
                starState = SlashCommandPermissions.CanRunState.ALLOW;
                break;
            }
        }

        Set<String> allow = new LinkedHashSet<>();
        Set<String> deny = new LinkedHashSet<>();

        if (starState == SlashCommandPermissions.CanRunState.ALLOW)
            allow.add("*");
        if (starState == SlashCommandPermissions.CanRunState.DENY)
            deny.add("*");

        for (String cmd : knownCommands) {
            if (hasCommandPermission(member, cmd)) {
                allow.add(cmd);
            } else {
                deny.add(cmd);
            }
        }

        return new SlashCommandPermissions(List.copyOf(allow), List.copyOf(deny));
    }

    public static boolean hasCommandPermission(Member member, String commandName) {
        for (SlashCommandPermissions perms : getEffectivePermissions(member)) {
            SlashCommandPermissions.CanRunState state = perms.canRun(commandName);
            if (state != SlashCommandPermissions.CanRunState.NOT_SET)
                return state == SlashCommandPermissions.CanRunState.ALLOW;
        }

        return false;
    }

    private static List<SlashCommandPermissions> getEffectivePermissions(Member member){
        if (member == null)
            return List.of();

        DiscordGuildContext ctx = DiscordChannelRegistry.getContext(member.getGuild().getId());
        if (ctx == null)
            return List.of();

        Map<String, SlashCommandPermissions> permissionsMap = ctx.cmdPermissions.roleMap();

        List<SlashCommandPermissions> rolePermissionsList = member.getRoles().stream()
                .map(r -> {
                    SlashCommandPermissions byId = permissionsMap.get(r.getId());
                    return byId != null ? byId : permissionsMap.get(r.getName());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        SlashCommandPermissions everyonePermissions = permissionsMap.get(ctx.guild.getId());
        if (everyonePermissions != null)
            rolePermissionsList.add(everyonePermissions);

        return switch (ctx.cmdPermissions.mode()) {
            case DiscordGuildsConfig.SlashCommandPermissionsConfig.Mode.TOP_ROLE
                    -> rolePermissionsList.isEmpty()
                    ? List.of(ctx.cmdPermissions.defaultPermissions())
                    : List.of(rolePermissionsList.getFirst());
            case DiscordGuildsConfig.SlashCommandPermissionsConfig.Mode.MERGE
                    -> rolePermissionsList.isEmpty()
                    ? List.of(ctx.cmdPermissions.defaultPermissions())
                    : rolePermissionsList;
        };
    }
}
