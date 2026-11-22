package com.narutotoaddon.mangekyou;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Cliente -> servidor: escolhe qual dos olhos possuidos fica ativo (aparencia e Susanoo). O servidor valida. */
public class SelectEyePacket {
    private final String implantId;

    public SelectEyePacket(String implantId) {
        this.implantId = implantId;
    }

    public static void encode(SelectEyePacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.implantId);
    }

    public static SelectEyePacket decode(FriendlyByteBuf buf) {
        return new SelectEyePacket(buf.readUtf());
    }

    public static void handle(SelectEyePacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp != null) Implants.setActive(sp, m.implantId);
        });
        c.setPacketHandled(true);
    }
}
