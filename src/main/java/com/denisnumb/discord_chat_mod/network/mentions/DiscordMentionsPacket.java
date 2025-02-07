package com.denisnumb.discord_chat_mod.network.mentions;

import com.denisnumb.discord_chat_mod.discord.ChannelMembersProvider;
import com.denisnumb.discord_chat_mod.discord.model.DiscordMemberData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class DiscordMentionsPacket {
    private final List<DiscordMemberData> memberData;
    private final Gson gson = new Gson();

    public DiscordMentionsPacket(List<DiscordMemberData> usernames) {
        this.memberData = usernames;
    }

    public DiscordMentionsPacket(FriendlyByteBuf buffer) {
        this.memberData = gson.fromJson(buffer.readUtf(), new TypeToken<List<DiscordMemberData>>(){}.getType());
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeUtf(gson.toJson(memberData));
    }

    public static void handle(DiscordMentionsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (packet.memberData != null)
                ChannelMembersProvider.clientMemberData = packet.memberData;
        });
        context.setPacketHandled(true);
    }
}
