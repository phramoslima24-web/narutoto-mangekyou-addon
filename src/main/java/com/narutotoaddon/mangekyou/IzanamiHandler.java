package com.narutotoaddon.mangekyou;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Izanami: prende o alvo num loop ETERNO. Ele fica imobilizado no ponto onde estava (cada vez que sai,
 * e puxado de volta), cego/enjoado/sem forca, nao ataca e toma dano continuo ate morrer.
 * O olho usado fica cego. O estado e gravado no proprio alvo, entao sobrevive a reiniciar o servidor.
 */
public final class IzanamiHandler {
    private IzanamiHandler() {}

    private static final String TAG = "mkaddon_izanami";
    private static final int EFFECT_TICKS = 120;

    private record Loop(ResourceKey<Level> dim, double x, double y, double z, float yRot, float xRot,
                        boolean disabledAi) {}

    private static final Map<UUID, Loop> LOOPS = new HashMap<>();

    public static boolean cast(ServerPlayer p, Ability a) {
        ServerLevel lv = (ServerLevel) p.level();
        LivingEntity target = Targeting.livingInSight(p, 32.0D);
        if (target == null) {
            AbilityHandler.msg(p, "no_target");
            return false;
        }
        if ((target instanceof Player pl && (pl.isCreative() || pl.isSpectator())) || LOOPS.containsKey(target.getUUID())) {
            AbilityHandler.msg(p, "invalid_target");
            return false;
        }
        boolean disabledAi = false;
        if (target instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            disabledAi = true;
        }
        Loop l = new Loop(lv.dimension(), target.getX(), target.getY(), target.getZ(),
                target.getYRot(), target.getXRot(), disabledAi);
        LOOPS.put(target.getUUID(), l);
        save(target, l);
        applyEffects(target);

        lv.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 1.0F, 0.4F);
        lv.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1, target.getZ(),
                60, 0.5, 0.8, 0.5, 0.5);
        Implants.blind(p, a.eye); // o preco: a visao do olho usado
        return true;
    }

    private static void save(LivingEntity e, Loop l) {
        CompoundTag t = e.getPersistentData();
        t.putBoolean(TAG, true);
        t.putDouble(TAG + "_x", l.x());
        t.putDouble(TAG + "_y", l.y());
        t.putDouble(TAG + "_z", l.z());
        t.putFloat(TAG + "_yaw", l.yRot());
        t.putFloat(TAG + "_pitch", l.xRot());
        t.putBoolean(TAG + "_ai", l.disabledAi());
    }

    private static void applyEffects(LivingEntity le) {
        le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EFFECT_TICKS, 255));
        le.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, EFFECT_TICKS, 0));
        le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, EFFECT_TICKS, 0));
        le.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, EFFECT_TICKS, 255));
        le.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, EFFECT_TICKS, 255));
    }

    /** Solta o alvo (usado quando o Izanagi liberta quem estava preso, ou quando ele morre). */
    public static void free(LivingEntity le) {
        Loop l = LOOPS.remove(le.getUUID());
        CompoundTag t = le.getPersistentData();
        if (l != null && l.disabledAi() && le instanceof Mob m) m.setNoAi(false);
        for (String k : new String[] {"", "_x", "_y", "_z", "_yaw", "_pitch", "_ai"}) t.remove(TAG + k);
        if (l != null) {
            le.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
            le.removeEffect(MobEffects.BLINDNESS);
            le.removeEffect(MobEffects.CONFUSION);
            le.removeEffect(MobEffects.WEAKNESS);
            le.removeEffect(MobEffects.DIG_SLOWDOWN);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || LOOPS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        int tc = server.getTickCount();
        if (tc % 2 != 0) return;
        Iterator<Map.Entry<UUID, Loop>> it = LOOPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Loop> en = it.next();
            Loop l = en.getValue();
            ServerLevel lv = server.getLevel(l.dim());
            Entity ent = lv == null ? null : lv.getEntity(en.getKey());
            // alvo descarregado/offline: mantem o loop (e restaurado ao voltar)
            if (!(ent instanceof LivingEntity le) || !le.isAlive()) continue;

            if (le.position().distanceToSqr(new Vec3(l.x(), l.y(), l.z())) > 0.04D) {
                if (le instanceof ServerPlayer sp) {
                    sp.connection.teleport(l.x(), l.y(), l.z(), l.yRot(), l.xRot());
                } else {
                    le.moveTo(l.x(), l.y(), l.z(), l.yRot(), l.xRot());
                }
                le.setDeltaMovement(Vec3.ZERO);
                le.hurtMarked = true;
            }
            if (tc % 20 == 0) {
                applyEffects(le);
                le.invulnerableTime = 0;
                le.hurt(lv.damageSources().magic(), 1.0F); // dano continuo ate morrer
                lv.sendParticles(ParticleTypes.REVERSE_PORTAL, le.getX(), le.getY() + 1, le.getZ(), 10, 0.3, 0.5, 0.3, 0.1);
            }
        }
    }

    /** Quem esta preso no loop nao consegue atacar. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        Entity attacker = e.getSource().getEntity();
        if (attacker != null && LOOPS.containsKey(attacker.getUUID())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity le = e.getEntity();
        if (LOOPS.containsKey(le.getUUID())) free(le);
    }

    /** Depois de reiniciar o servidor / recarregar o chunk, o alvo continua preso. */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (!(e.getLevel() instanceof ServerLevel lv) || !(e.getEntity() instanceof LivingEntity le)) return;
        CompoundTag t = le.getPersistentData();
        if (!t.getBoolean(TAG) || LOOPS.containsKey(le.getUUID())) return;
        boolean ai = t.getBoolean(TAG + "_ai");
        if (ai && le instanceof Mob m) m.setNoAi(true);
        LOOPS.put(le.getUUID(), new Loop(lv.dimension(), t.getDouble(TAG + "_x"), t.getDouble(TAG + "_y"),
                t.getDouble(TAG + "_z"), t.getFloat(TAG + "_yaw"), t.getFloat(TAG + "_pitch"), ai));
    }

    public static void clear() {
        LOOPS.clear();
    }
}
