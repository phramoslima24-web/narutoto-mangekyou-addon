package com.narutotoaddon.mangekyou;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Cliente -> servidor: clique de ataque com a broca/lanca ou golpe do Susanoo montado. */
public class SusanooActionPacket {
    public static final int ATTACK = 0;
    private final int action;

    public SusanooActionPacket(int action) { this.action = action; }

    public static void encode(SusanooActionPacket m, FriendlyByteBuf b) { b.writeByte(m.action); }

    public static SusanooActionPacket decode(FriendlyByteBuf b) { return new SusanooActionPacket(b.readByte()); }

    public static void handle(SusanooActionPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer p = c.getSender();
            if (p == null || !p.isAlive() || SusanooKind.of(p) == null) return;
            if (m.action == ATTACK) SusanooAbilities.attackAction(p);
        });
        c.setPacketHandled(true);
    }
}
