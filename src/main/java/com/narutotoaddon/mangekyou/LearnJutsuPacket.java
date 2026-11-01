package com.narutotoaddon.mangekyou;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class LearnJutsuPacket {
    private final String abilityId;

    public LearnJutsuPacket(String abilityId) {
        this.abilityId = abilityId;
    }

    public static void encode(LearnJutsuPacket m, FriendlyByteBuf buf) {
        buf.writeUtf(m.abilityId);
    }

    public static LearnJutsuPacket decode(FriendlyByteBuf buf) {
        return new LearnJutsuPacket(buf.readUtf());
    }

    public static void handle(LearnJutsuPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp != null) Implants.learn(sp, m.abilityId);
        });
        c.setPacketHandled(true);
    }
}
