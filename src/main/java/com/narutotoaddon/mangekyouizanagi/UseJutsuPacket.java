package com.narutotoaddon.mangekyouizanagi;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Acoes do cliente para o servidor.
 * 0 = soltou a tecla de jutsu do Narutoto (usa so se o jutsu selecionado for do addon)
 * 1 = tecla propria "usar" (usa o jutsu selecionado do addon ou, se nao houver, o escolhido pela tecla "trocar")
 * 2 = tecla propria "trocar jutsu do implante"
 */
public class UseJutsuPacket {
    public static final int MOD_KEY = 0, OWN_USE = 1, OWN_CYCLE = 2;
    private final int type;

    public UseJutsuPacket(int type) {
        this.type = type;
    }

    public static void encode(UseJutsuPacket m, FriendlyByteBuf buf) {
        buf.writeByte(m.type);
    }

    public static UseJutsuPacket decode(FriendlyByteBuf buf) {
        return new UseJutsuPacket(buf.readByte());
    }

    public static void handle(UseJutsuPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null) return;
            switch (m.type) {
                case MOD_KEY -> {
                    // o servidor le o jutsu escolhido por conta propria (nao confia no cliente)
                    Ability a = Ability.byJutsuKey(Bridge.getString(sp, "jutsu_choosed_name", ""));
                    if (a != null) AbilityHandler.use(sp, a);
                }
                case OWN_USE -> Implants.useOwn(sp);
                case OWN_CYCLE -> Implants.cycle(sp);
                default -> { }
            }
        });
        c.setPacketHandled(true);
    }
}
