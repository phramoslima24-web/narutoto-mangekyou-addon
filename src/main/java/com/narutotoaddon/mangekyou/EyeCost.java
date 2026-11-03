package com.narutotoaddon.mangekyou;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Custo do Izanagi / Izanami: em vez de cegar para sempre, o usuario fica cego so por 5 segundos. */
final class EyeCost {
    private EyeCost() {}

    /** 5 segundos (20 ticks = 1 s). */
    static final int BLIND_TICKS = 100;

    static void apply(ServerPlayer p) {
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLIND_TICKS, 0, false, false, true));
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".eye_cost"));
    }
}
