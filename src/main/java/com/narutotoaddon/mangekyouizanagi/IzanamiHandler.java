package com.narutotoaddon.mangekyouizanagi;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Izanami: prende o alvo num loop. Por 20 s ele fica preso no ponto onde estava (cada vez que se afasta e
 * puxado de volta), cego/enjoado/sem forca e nao consegue atacar. O olho usado fica cego (mesma regra do Izanagi).
 */
public final class IzanamiHandler {
    private IzanamiHandler() {}

    private static final int LOOP_TICKS = 400; // 20 s

    private record Loop(ResourceKey<Level> dim, long end, double x, double y, double z, float yRot, float xRot,
                        boolean disabledAi) {}

    private static final Map<UUID, Loop> LOOPS = new HashMap<>();

    public static boolean cast(ServerPlayer p, Ability a) {
        Implant imp = Implants.get(p);
        ServerLevel lv = (ServerLevel) p.level();
        LivingEntity target = Targeting.livingInSight(p, 32.0D);
        if (target == null) {
            AbilityHandler.msg(p, "no_target");
            return false;
        }
        if (LOOPS.containsKey(target.getUUID())) {
            AbilityHandler.msg(p, "invalid_target");
            return false;
        }
        boolean disabledAi = false;
        if (target instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            disabledAi = true;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LOOP_TICKS, 255));
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, LOOP_TICKS, 0));
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, LOOP_TICKS, 0));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, LOOP_TICKS, 255));
        target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, LOOP_TICKS, 255));
        LOOPS.put(target.getUUID(), new Loop(lv.dimension(), lv.getGameTime() + LOOP_TICKS,
                target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot(), disabledAi));

        lv.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 1.0F, 0.4F);
        lv.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 1, target.getZ(),
                60, 0.5, 0.8, 0.5, 0.5);
        Implants.blind(p, imp, a.eye);
        return true;
    }

    private static void release(MinecraftServer server, UUID id, Loop l) {
        ServerLevel lv = server.getLevel(l.dim());
        Entity ent = lv == null ? null : lv.getEntity(id);
        if (ent instanceof Mob m && l.disabledAi()) m.setNoAi(false);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || LOOPS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 5 != 0) return;
        Iterator<Map.Entry<UUID, Loop>> it = LOOPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Loop> en = it.next();
            Loop l = en.getValue();
            ServerLevel lv = server.getLevel(l.dim());
            Entity ent = lv == null ? null : lv.getEntity(en.getKey());
            if (!(ent instanceof LivingEntity le) || !le.isAlive() || lv.getGameTime() >= l.end()) {
                release(server, en.getKey(), l);
                it.remove();
                continue;
            }
            if (le.position().distanceToSqr(new Vec3(l.x(), l.y(), l.z())) > 0.25D) {
                if (le instanceof ServerPlayer sp) {
                    sp.connection.teleport(l.x(), l.y(), l.z(), l.yRot(), l.xRot());
                } else {
                    le.moveTo(l.x(), l.y(), l.z(), l.yRot(), l.xRot());
                }
                le.setDeltaMovement(Vec3.ZERO);
                le.hurtMarked = true;
                lv.sendParticles(ParticleTypes.REVERSE_PORTAL, le.getX(), le.getY() + 1, le.getZ(), 12, 0.3, 0.5, 0.3, 0.1);
            }
        }
    }

    /** Quem esta preso no loop nao consegue atacar. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        Entity attacker = e.getSource().getEntity();
        if (attacker != null && LOOPS.containsKey(attacker.getUUID())) e.setCanceled(true);
    }

    public static void clear() {
        LOOPS.clear();
    }
}
