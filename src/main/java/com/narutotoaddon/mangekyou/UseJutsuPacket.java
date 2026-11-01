package com.narutotoaddon.mangekyou;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/** Enviado quando o jogador solta a tecla de jutsu do Narutoto com um jutsu do addon selecionado. */
public class UseJutsuPacket {
    public UseJutsuPacket() {}

    public static void encode(UseJutsuPacket m, FriendlyByteBuf buf) {}

    public static UseJutsuPacket decode(FriendlyByteBuf buf) {
        return new UseJutsuPacket();
    }

    public static void handle(UseJutsuPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null) return;
            // o servidor le o jutsu escolhido por conta propria (nao confia no cliente)
            Ability a = Ability.byJutsuKey(Bridge.getString(sp, "jutsu_choosed_name", ""));
            if (a != null) AbilityHandler.use(sp, a);
        });
        c.setPacketHandled(true);
    }
}
