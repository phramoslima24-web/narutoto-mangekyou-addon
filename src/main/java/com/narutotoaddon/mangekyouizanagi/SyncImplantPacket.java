package com.narutotoaddon.mangekyouizanagi;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SyncImplantPacket {
    private final UUID player;
    private final String id;
    private final int blindMask;

    public SyncImplantPacket(UUID player, String id, int blindMask) {
        this.player = player;
        this.id = id;
        this.blindMask = blindMask;
    }

    public static void encode(SyncImplantPacket m, FriendlyByteBuf buf) {
        buf.writeUUID(m.player);
        buf.writeUtf(m.id);
        buf.writeByte(m.blindMask);
    }

    public static SyncImplantPacket decode(FriendlyByteBuf buf) {
        return new SyncImplantPacket(buf.readUUID(), buf.readUtf(), buf.readByte());
    }

    public static void handle(SyncImplantPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientState.set(m.player, m.id, m.blindMask));
        ctx.get().setPacketHandled(true);
    }
}
