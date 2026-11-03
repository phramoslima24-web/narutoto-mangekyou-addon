package com.narutotoaddon.mangekyou;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Izanagi: ao usar, o olho fica "armado" por um tempo. Se o usuario for receber um golpe fatal nesse periodo,
 * a morte e anulada (vida cheia, efeitos limpos, invulneravel por 3 s) e o usuario fica cego por 5 s.
 */
public final class IzanagiHandler {
    private IzanagiHandler() {}

    private static final String TAG_ARMED_UNTIL = "mkaddon_izanagi_until";
    /** Tempo que o Izanagi fica armado: 60 s. */
    private static final int ARM_TICKS = 1200;

    /** Chamado pelo AbilityHandler ao usar o jutsu. */
    static boolean arm(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        long now = lv.getGameTime();
        if (now < p.getPersistentData().getLong(TAG_ARMED_UNTIL)) {
            AbilityHandler.msg(p, "izanagi_already");
            return false;
        }
        p.getPersistentData().putLong(TAG_ARMED_UNTIL, now + ARM_TICKS);
        lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.5F);
        lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.8, 0.4, 0.03);
        AbilityHandler.msg(p, "izanagi_armed");
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        // /kill e o vazio ignoram invulnerabilidade: nao salvamos (evita loop de morte)
        if (e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (Implants.get(p) == null) return;
        ServerLevel lv = (ServerLevel) p.level();
        long until = p.getPersistentData().getLong(TAG_ARMED_UNTIL);
        if (until <= 0 || lv.getGameTime() >= until) return;

        // gasta o Izanagi e anula a morte
        p.getPersistentData().putLong(TAG_ARMED_UNTIL, 0L);
        e.setCanceled(true);

        p.removeAllEffects();
        p.setHealth(p.getMaxHealth());
        p.clearFire();
        p.fallDistance = 0.0F;
        p.invulnerableTime = 60;
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4)); // 3 s sem dano
        AbilityScheduler.BURNING.remove(p.getUUID());
        AbilityScheduler.TSUKUYOMI.remove(p.getUUID());

        lv.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        lv.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1.0, p.getZ(), 60, 0.5, 0.8, 0.5, 0.4);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".izanagi_used"));
        EyeCost.apply(p); // cego por 5 s (nao e permanente)
    }
}
