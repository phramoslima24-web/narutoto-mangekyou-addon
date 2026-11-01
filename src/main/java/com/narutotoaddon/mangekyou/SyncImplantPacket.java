package com.narutotoaddon.mangekyou;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SyncImplantPacket {
    private final UUID player;
    private final String id;

    public SyncImplantPacket(UUID player, String id) {
        this.player = player;
        this.id = id;
    }

    public static void encode(SyncImplantPacket m, FriendlyByteBuf buf) {
        buf.writeUUID(m.player);
        buf.writeUtf(m.id);
    }

    public static SyncImplantPacket decode(FriendlyByteBuf buf) {
        return new SyncImplantPacket(buf.readUUID(), buf.readUtf());
    }

    public static void handle(SyncImplantPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientState.set(m.player, m.id));
        ctx.get().setPacketHandled(true);
    }
}
