package com.narutotoaddon.mangekyou;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SyncImplantPacket {
    private final UUID player;
    private final String id;
    private final int blindMask;
    /** Implante dono da cegueira (define qual textura cega aparece e qual Mangekyou cura). */
    private final String blindOwner;

    public SyncImplantPacket(UUID player, String id, int blindMask, String blindOwner) {
        this.player = player;
        this.id = id;
        this.blindMask = blindMask;
        this.blindOwner = blindOwner == null ? "" : blindOwner;
    }

    public static void encode(SyncImplantPacket m, FriendlyByteBuf buf) {
        buf.writeUUID(m.player);
        buf.writeUtf(m.id);
        buf.writeByte(m.blindMask);
        buf.writeUtf(m.blindOwner);
    }

    public static SyncImplantPacket decode(FriendlyByteBuf buf) {
        return new SyncImplantPacket(buf.readUUID(), buf.readUtf(), buf.readByte(), buf.readUtf());
    }

    public static void handle(SyncImplantPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientState.set(m.player, m.id, m.blindMask, m.blindOwner));
        ctx.get().setPacketHandled(true);
    }
}
