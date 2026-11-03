package com.narutotoaddon.mangekyou;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Executa as habilidades dos olhos implantados (lado servidor). O Susanoo e o do proprio mod. */
public final class AbilityHandler {
    private AbilityHandler() {}

    private static String cdKey(Ability a) {
        return "mkaddon_cd_" + a.id;
    }

    public static void use(ServerPlayer p, Ability a) {
        Implant imp = Implants.get(p);
        if (imp == null || !imp.allows(a)) {
            msg(p, "wrong_implant");
            return;
        }
        if (!Implants.mangekyouActive(p)) {
            msg(p, "need_mangekyou");
            return;
        }
        // Shift + Novo Amaterasu: apaga todos os fogos pretos (sem recarga)
        if (a == Ability.NOVO_AMATERASU && p.isShiftKeyDown()) {
            BlackFire.extinguishAll(p);
            return;
        }
        long now = p.level().getGameTime();
        long cdEnd = p.getPersistentData().getLong(cdKey(a));
        if (now < cdEnd && !p.getAbilities().instabuild) {
            long secs = (cdEnd - now + 19) / 20;
            p.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".cooldown", secs), true);
            return;
        }
        boolean ok = switch (a) {
            case NOVO_AMATERASU -> novoAmaterasu(p);
            case KOTOAMATSUKAMI -> kotoamatsukami(p);
            case TSUKUYOMI -> tsukuyomi(p);
            case REWIND -> RewindHandler.rewind(p);
            case TIME_STOP -> TimeStopHandler.start(p);
            case IZANAGI -> izanagi(p);
            case IZANAMI -> izanami(p);
        };
        if (ok) {
            p.getPersistentData().putLong(cdKey(a), now + a.cooldownTicks);
        }
    }

    static void msg(ServerPlayer p, String key) {
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + "." + key));
    }


    // --------------------------------------------------------------- Izanagi
    // Abre uma janela de 5 segundos para reescrever um golpe fatal.
    // Ao terminar (ou ao salvar o usuario), causa cegueira temporaria de 5 segundos.
    private static boolean izanagi(ServerPlayer p) {
        long now = p.level().getGameTime();
        if (AbilityScheduler.IZANAGI.containsKey(p.getUUID())) {
            msg(p, "izanagi_active");
            return false;
        }
        AbilityScheduler.IZANAGI.put(p.getUUID(),
                new AbilityScheduler.Izanagi(p.level().dimension(), now + 100));
        p.level().playSound(null, p.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.65F);
        p.level().levelEvent(2001, p.blockPosition(), 0);
        return true;
    }

    // --------------------------------------------------------------- Izanami
    // Prende o alvo em um ciclo de genjutsu por 10 segundos.
    private static boolean izanami(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        LivingEntity target = Targeting.livingInSight(p, 32.0D);
        if (target == null || target == p
                || (target instanceof net.minecraft.world.entity.player.Player pl && (pl.isCreative() || pl.isSpectator()))) {
            msg(p, "no_target");
            return false;
        }
        int duration = 200; // 10 segundos
        boolean disabledAi = false;
        if (target instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            disabledAi = true;
        }
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, duration, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 255));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 2));
        AbilityScheduler.IZANAMI.put(target.getUUID(),
                new AbilityScheduler.Looped(lv.dimension(), lv.getGameTime() + duration, disabledAi));
        // O uso do Izanami deixa o usuario temporariamente cego, nunca permanentemente.
        p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        lv.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 1.0F, 0.45F);
        lv.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1.0, target.getZ(),
                45, 0.5, 0.8, 0.5, 0.08);
        return true;
    }

    // ---------------------------------------------------------- Novo Amaterasu
    private static boolean novoAmaterasu(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        LivingEntity target = Targeting.livingInSight(p, 48.0D);
        if (target != null) {
            AbilityScheduler.BURNING.put(target.getUUID(),
                    new AbilityScheduler.Timed(lv.dimension(), lv.getGameTime() + 400)); // 20 s seguindo o alvo
            target.setSecondsOnFire(10);
            BlackFire.placeAround(lv, target.blockPosition(), 1);
            lv.playSound(null, target.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.5F);
            return true;
        }
        HitResult hr = p.pick(48.0D, 1.0F, false);
        if (hr instanceof BlockHitResult bhr && hr.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bhr.getBlockPos().relative(bhr.getDirection());
            if (BlackFire.placeAround(lv, pos, 2) > 0) {
                lv.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.5F);
                return true;
            }
        }
        msg(p, "no_target");
        return false;
    }

    // --------------------------------------------------------- Kotoamatsukami
    private static boolean kotoamatsukami(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        LivingEntity target = Targeting.livingInSight(p, 32.0D);
        if (target == null) {
            msg(p, "no_target");
            return false;
        }
        if (target instanceof net.minecraft.world.entity.player.Player pl && (pl.isCreative() || pl.isSpectator())) {
            msg(p, "invalid_target");
            return false;
        }
        if (target instanceof ServerPlayer other) {
            other.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
            other.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
            msg(other, "koto_player");
        } else if (target instanceof Mob mob) {
            mob.setTarget(null);
            AbilityScheduler.CONTROLLED.put(mob.getUUID(),
                    new AbilityScheduler.Controlled(lv.dimension(), lv.getGameTime() + 3000, p.getUUID())); // 2,5 min
        } else {
            msg(p, "invalid_target");
            return false;
        }
        lv.playSound(null, target.blockPosition(), SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 1.2F);
        lv.sendParticles(ParticleTypes.ENCHANT, target.getX(), target.getY() + 1.5, target.getZ(),
                40, 0.5, 0.5, 0.5, 0.5);
        return true;
    }

    // -------------------------------------------------------------- Tsukuyomi
    private static boolean tsukuyomi(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        LivingEntity target = Targeting.livingInSight(p, 32.0D);
        if (target == null) {
            msg(p, "no_target");
            return false;
        }
        if (target instanceof net.minecraft.world.entity.player.Player pl && (pl.isCreative() || pl.isSpectator())) {
            msg(p, "invalid_target");
            return false;
        }
        int dur = 200; // 10 s
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dur, 0));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, dur, 0));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, dur, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 255));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 4));
        boolean disabledAi = false;
        if (target instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            disabledAi = true;
        }
        AbilityScheduler.TSUKUYOMI.put(target.getUUID(),
                new AbilityScheduler.Locked(lv.dimension(), lv.getGameTime() + dur, disabledAi));
        lv.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 1.0F, 0.6F);
        lv.sendParticles(ParticleTypes.SOUL, target.getX(), target.getY() + 1.2, target.getZ(), 25, 0.5, 0.8, 0.5, 0.02);
        lv.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 1, target.getZ(),
                30, 0.4, 0.6, 0.4, 0.05);
        return true;
    }
}
