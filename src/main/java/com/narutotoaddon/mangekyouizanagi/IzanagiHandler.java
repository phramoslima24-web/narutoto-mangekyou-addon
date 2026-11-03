package com.narutotoaddon.mangekyouizanagi;

import java.util.ArrayList;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Izanagi: ao usar, o olho escolhido fica "armado" por um tempo. Se uma morte acontecer nesse periodo, ela e
 * anulada (vida cheia, efeitos ruins removidos, volta ao ponto de nascimento se caiu no void) e o olho usado
 * fica CEGO. Se o tempo acabar sem morrer, nada e cobrado. A unica cura da cegueira e implantar o mesmo
 * Mangekyou de novo (ver {@link Implants#apply}).
 */
public final class IzanagiHandler {
    private IzanagiHandler() {}

    /** Quanto tempo o Izanagi fica armado esperando uma morte (2 minutos). */
    public static final int ARMED_TICKS = 2400;

    private static final String TAG_EYE = "mkiz_izanagi_eye";
    private static final String TAG_END = "mkiz_izanagi_end";

    public static boolean isArmed(ServerPlayer p) {
        int eye = p.getPersistentData().getInt(TAG_EYE);
        return eye != 0 && p.level().getGameTime() < p.getPersistentData().getLong(TAG_END);
    }

    private static void disarm(ServerPlayer p) {
        p.getPersistentData().remove(TAG_EYE);
        p.getPersistentData().remove(TAG_END);
    }

    /** Usa o Izanagi no olho indicado (1 = esquerdo, 2 = direito). */
    public static boolean arm(ServerPlayer p, int eyeBit) {
        if (isArmed(p)) {
            Implants.msg(p, "izanagi_already");
            return false;
        }
        p.getPersistentData().putInt(TAG_EYE, eyeBit);
        p.getPersistentData().putLong(TAG_END, p.level().getGameTime() + ARMED_TICKS);
        ServerLevel lv = (ServerLevel) p.level();
        lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".izanagi_armed", ARMED_TICKS / 20));
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !isArmed(p)) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypes.GENERIC_KILL)) return; // /kill passa por cima de tudo

        int eyeBit = p.getPersistentData().getInt(TAG_EYE);
        Implant imp = Implants.get(p);
        if (imp == null) {
            disarm(p);
            return;
        }
        e.setCanceled(true);
        disarm(p);

        ServerLevel lv = (ServerLevel) p.level();
        boolean voidDeath = src.is(DamageTypes.FELL_OUT_OF_WORLD) || p.getY() < lv.getMinBuildHeight();

        p.setHealth(p.getMaxHealth());
        p.clearFire();
        p.fallDistance = 0.0F;
        p.setAirSupply(p.getMaxAirSupply());
        for (MobEffectInstance m : new ArrayList<>(p.getActiveEffects())) {
            if (!m.getEffect().isBeneficial()) p.removeEffect(m.getEffect());
        }
        // sobrevive ao que estava matando (lava, fogo, afogamento) tempo suficiente para sair
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        p.invulnerableTime = 40;

        if (voidDeath) {
            ServerLevel target = p.server.getLevel(p.getRespawnDimension());
            if (target == null) target = lv;
            BlockPos pos = p.getRespawnPosition();
            if (pos == null) pos = target.getSharedSpawnPos();
            p.teleportTo(target, pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, p.getYRot(), p.getXRot());
            lv = target;
        }

        lv.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.7F);
        lv.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.05);
        lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.1);

        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".izanagi_used"));
        Implants.blind(p, imp, eyeBit);
    }

    /** Efeito visual enquanto armado + aviso quando o tempo acaba sem morte (sem custo). */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient()) return;
        if (!(e.player instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        if (p.getPersistentData().getInt(TAG_EYE) == 0) return;
        if (isArmed(p)) {
            ((ServerLevel) p.level()).sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(),
                    1, 0.3, 0.6, 0.3, 0.0);
        } else {
            disarm(p);
            Implants.msg(p, "izanagi_expired");
        }
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(IzanagiHandler.class);
    }
}
