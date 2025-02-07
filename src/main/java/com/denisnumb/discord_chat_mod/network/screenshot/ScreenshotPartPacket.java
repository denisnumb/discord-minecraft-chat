package com.denisnumb.discord_chat_mod.network.screenshot;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import static com.denisnumb.discord_chat_mod.DiscordChatMod.isDiscordConnected;
import static com.denisnumb.discord_chat_mod.MinecraftUtils.getTranslate;
import static com.denisnumb.discord_chat_mod.ModLanguageKey.SERVER_IS_NOT_CONNECTED_TO_DISCORD;


public class ScreenshotPartPacket {
    public final long imageId;
    public final int partIndex;
    public final int totalParts;
    public final byte[] data;

    public ScreenshotPartPacket(long imageId, int partIndex, int totalParts, byte[] data){
        this.imageId = imageId;
        this.partIndex = partIndex;
        this.totalParts = totalParts;
        this.data = data;
    }

    public ScreenshotPartPacket(FriendlyByteBuf buffer) {
        this.imageId = buffer.readLong();
        this.partIndex = buffer.readInt();
        this.totalParts = buffer.readInt();
        this.data = buffer.readByteArray();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeLong(imageId);
        buffer.writeInt(partIndex);
        buffer.writeInt(totalParts);
        buffer.writeByteArray(data);
    }

    public static void handle(ScreenshotPartPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null){
                if (isDiscordConnected())
                    ScreenshotReceiver.receivePart(packet, player);
                else if (packet.partIndex == 0){
                    player.sendSystemMessage(
                            Component.literal(getTranslate(SERVER_IS_NOT_CONNECTED_TO_DISCORD, "Server is not connected to Discord"))
                                    .withStyle(ChatFormatting.RED)
                    );
                }
            }
        });
        context.setPacketHandled(true);
    }
}
