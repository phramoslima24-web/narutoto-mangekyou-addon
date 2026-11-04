package com.narutotoaddon.mangekyou;

import java.util.ArrayList;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Izanagi: ao usar, deixa um CORPO no chao e fica armado por 5 minutos. Se o jogador morrer nesse tempo,
 * a morte e anulada: ele revive e e teleportado para o corpo (mesmo em outra dimensao), em troca da visao
 * do olho usado. Se os 5 minutos passarem sem morrer, o corpo some e nada e cobrado.
 */
public final class IzanagiHandler {
    private IzanagiHandler() {}

    /** 5 minutos. */
    public static final int ARMED_TICKS = 6000;
    private static final String TAG = "mkaddon_izanagi";

    private static CompoundTag data(ServerPlayer p) {
        return p.getPersistentData().getCompound(TAG);
    }

    public static boolean isArmed(ServerPlayer p) {
        CompoundTag t = data(p);
        return t.getInt("eye") != 0 && p.level().getGameTime() < t.getLong("end");
    }

    /** O corpo com esse UUID e o corpo vigente do jogador? */
    static boolean ownsBody(ServerPlayer p, UUID body) {
        CompoundTag t = data(p);
        return isArmed(p) && t.hasUUID("body") && t.getUUID("body").equals(body);
    }

    /** Usa o Izanagi no olho indicado (1 = esquerdo, 2 = direito). */
    public static boolean arm(ServerPlayer p, int eyeBit) {
        if (isArmed(p)) {
            Implants.msg(p, "izanagi_already");
            return false;
        }
        ServerLevel lv = (ServerLevel) p.level();
        long end = lv.getGameTime() + ARMED_TICKS;

        IzanagiBody body = ModEntities.IZANAGI_BODY.get().create(lv);
        if (body == null) return false;
        body.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0.0F);
        body.setOwner(p.getUUID());
        body.setEnd(end);
        lv.addFreshEntity(body);

        CompoundTag t = new CompoundTag();
        t.putInt("eye", eyeBit);
        t.putLong("end", end);
        t.putString("dim", lv.dimension().location().toString());
        t.putDouble("x", p.getX());
        t.putDouble("y", p.getY());
        t.putDouble("z", p.getZ());
        t.putFloat("yaw", p.getYRot());
        t.putUUID("body", body.getUUID());
        p.getPersistentData().put(TAG, t);

        lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".izanagi_armed", ARMED_TICKS / 20 / 60));
        return true;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !isArmed(p)) return;
        if (e.getSource().is(DamageTypes.GENERIC_KILL)) return; // /kill passa por cima de tudo

        CompoundTag t = data(p).copy();
        p.getPersistentData().remove(TAG);
        e.setCanceled(true);

        ServerLevel from = (ServerLevel) p.level();
        ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(t.getString("dim")));
        ServerLevel dest = p.server.getLevel(dimKey);
        if (dest == null) dest = from;

        p.setHealth(p.getMaxHealth());
        p.clearFire();
        p.fallDistance = 0.0F;
        p.setAirSupply(p.getMaxAirSupply());
        for (MobEffectInstance m : new ArrayList<>(p.getActiveEffects())) {
            if (!m.getEffect().isBeneficial()) p.removeEffect(m.getEffect());
        }
        // se estava preso no Izanami, escapa (senao seria puxado de volta e morreria de novo)
        IzanamiHandler.free(p);
        AbilityScheduler.BURNING.remove(p.getUUID());
        AbilityScheduler.TSUKUYOMI.remove(p.getUUID());

        from.sendParticles(ParticleTypes.SOUL, p.getX(), p.getY() + 1.0, p.getZ(), 30, 0.4, 0.7, 0.4, 0.05);
        p.teleportTo(dest, t.getDouble("x"), t.getDouble("y"), t.getDouble("z"), t.getFloat("yaw"), 0.0F);
        p.setDeltaMovement(0, 0, 0);
        p.hurtMarked = true;

        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
        p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        p.invulnerableTime = 40;

        // o corpo some (se o chunk nao estava carregado, o proprio corpo se remove depois)
        Entity body = t.hasUUID("body") ? dest.getEntity(t.getUUID("body")) : null;
        if (body != null) body.discard();

        dest.playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.7F);
        dest.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.1);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".izanagi_used"));
        Implants.blind(p, t.getInt("eye")); // o preco: a visao do olho usado
    }

    /** Particulas enquanto armado + aviso quando o tempo acaba sem morte (sem custo). */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient()) return;
        if (!(e.player instanceof ServerPlayer p) || p.tickCount % 10 != 0) return;
        if (!p.getPersistentData().contains(TAG)) return;
        if (isArmed(p)) {
            ((ServerLevel) p.level()).sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(),
                    1, 0.3, 0.6, 0.3, 0.0);
        } else {
            p.getPersistentData().remove(TAG);
            Implants.msg(p, "izanagi_expired");
        }
    }
}
