package com.denisnumb.discord_chat_mod.chat_style;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.MOD_ID;
import static com.denisnumb.discord_chat_mod.chat_style.Parameters.*;

public final class CustomChatTypeRegistry {
    private CustomChatTypeRegistry() {}

    public static ResourceKey<ChatType> CHAT;
    public static ResourceKey<ChatType> SAY_COMMAND;
    public static ResourceKey<ChatType> MSG_COMMAND_INCOMING;
    public static ResourceKey<ChatType> MSG_COMMAND_OUTGOING;
    public static ResourceKey<ChatType> TEAM_MSG_COMMAND_INCOMING;
    public static ResourceKey<ChatType> TEAM_MSG_COMMAND_OUTGOING;
    public static ResourceKey<ChatType> EMOTE_COMMAND;

    public static final String CHAT_PATH = "chat";
    public static final String SAY_COMMAND_PATH = "say_command";
    public static final String EMOTE_COMMAND_PATH = "emote_command";
    public static final String MSG_COMMAND_INCOMING_PATH = "msg_command_incoming";
    public static final String MSG_COMMAND_OUTGOING_PATH = "msg_command_outgoing";
    public static final String TEAM_MSG_COMMAND_INCOMING_PATH = "team_msg_command_incoming";
    public static final String TEAM_MSG_COMMAND_OUTGOING_PATH = "team_msg_command_outgoing";

    public static void registerChatTypes(Registry<ChatType> registry) {
        CHAT = ResourceKey.create(Registries.CHAT_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, CHAT_PATH));
        Registry.register(registry, CHAT, buildChatType(CHAT));

        SAY_COMMAND = ResourceKey.create(Registries.CHAT_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, SAY_COMMAND_PATH));
        Registry.register(registry, SAY_COMMAND, buildChatType(SAY_COMMAND));

        EMOTE_COMMAND = ResourceKey.create(Registries.CHAT_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, EMOTE_COMMAND_PATH));
        Registry.register(registry, EMOTE_COMMAND, buildChatType(EMOTE_COMMAND));

        MSG_COMMAND_INCOMING = ResourceKey.create(Registries.CHAT_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, MSG_COMMAND_INCOMING_PATH));
        Registry.register(registry, MSG_COMMAND_INCOMING, buildChatType(MSG_COMMAND_INCOMING));

        MSG_COMMAND_OUTGOING = ResourceKey.create(Registries.CHAT_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, MSG_COMMAND_OUTGOING_PATH));
        Registry.register(registry, MSG_COMMAND_OUTGOING, buildChatType(MSG_COMMAND_OUTGOING));

        TEAM_MSG_COMMAND_INCOMING = ResourceKey.create(Registries.CHAT_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, TEAM_MSG_COMMAND_INCOMING_PATH));
        Registry.register(registry, TEAM_MSG_COMMAND_INCOMING, buildChatType(TEAM_MSG_COMMAND_INCOMING));

        TEAM_MSG_COMMAND_OUTGOING = ResourceKey.create(Registries.CHAT_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, TEAM_MSG_COMMAND_OUTGOING_PATH));
        Registry.register(registry, TEAM_MSG_COMMAND_OUTGOING, buildChatType(TEAM_MSG_COMMAND_OUTGOING));
    }

    public static String[] getParametersByChatType(ResourceKey<ChatType> chatType) {
        return switch (chatType.identifier().getPath()) {
            case CHAT_PATH, SAY_COMMAND_PATH, EMOTE_COMMAND_PATH -> new String[] { PLAYER, MESSAGE };
            case MSG_COMMAND_INCOMING_PATH -> new String[] { SENDER, MESSAGE};
            case MSG_COMMAND_OUTGOING_PATH -> new String[] { RECEIVER, MESSAGE };
            case TEAM_MSG_COMMAND_INCOMING_PATH, TEAM_MSG_COMMAND_OUTGOING_PATH ->
                    new String[] { TEAM, PLAYER, MESSAGE };
            default -> new String[0];
        };
    }

    public static ChatType.Bound buildBound(
            ResourceKey<ChatType> chatType,
            RegistryAccess registryAccess,
            Component sender,
            @Nullable Component originalMessage
    ) {
        Registry<ChatType> registry = registryAccess.lookupOrThrow(Registries.CHAT_TYPE);

        return new ChatType.Bound(
                registry.wrapAsHolder(registry.getValueOrThrow(chatType)),
                sender,
                originalMessage == null
                        ? Optional.empty()
                        : Optional.of(originalMessage)
        );
    }

    private static ChatType buildChatType(ResourceKey<ChatType> key) {
        String narrateTranslationKey = key.identifier().getPath().equals(EMOTE_COMMAND_PATH)
                ? "chat.type.emote"
                : "chat.type.text.narrate";

        List<ChatTypeDecoration.Parameter> chatParameters = List.of(ChatTypeDecoration.Parameter.CONTENT);
        List<ChatTypeDecoration.Parameter> narrationParameters = List.of(ChatTypeDecoration.Parameter.SENDER, ChatTypeDecoration.Parameter.TARGET);

        return new ChatType(
                new ChatTypeDecoration("%s", chatParameters, Style.EMPTY),
                new ChatTypeDecoration(narrateTranslationKey, narrationParameters, Style.EMPTY)
        );
    }
}
