package com.narutotoaddon.mangekyou;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SyncImplantPacket {
    private final String id;

    public SyncImplantPacket(String id) {
        this.id = id;
    }

    public static void encode(SyncImplantPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.id);
    }

    public static SyncImplantPacket decode(FriendlyByteBuf buf) {
        return new SyncImplantPacket(buf.readUtf());
    }

    public static void handle(SyncImplantPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientState.implant = m.id);
        ctx.get().setPacketHandled(true);
    }
}
