package com.narutotoaddon.mangekyou;

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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Izanami: prende o alvo num loop de 10 s. A cada 2 s ele volta ao ponto onde o loop comecou.
 * O usuario fica cego por 5 s (nao e permanente).
 */
public final class IzanamiHandler {
    private IzanamiHandler() {}

    private static final int LOOP_TICKS = 200;     // duracao total: 10 s
    private static final int RESET_EVERY = 40;     // volta ao ponto inicial a cada 2 s

    private record Loop(ResourceKey<Level> dim, long start, long end, boolean disabledAi,
                        double x, double y, double z) {}

    private static final Map<UUID, Loop> LOOPS = new HashMap<>();

    /** Chamado pelo AbilityHandler ao usar o jutsu. */
    static boolean start(ServerPlayer p) {
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
        target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, LOOP_TICKS, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, LOOP_TICKS, 3));
        long now = lv.getGameTime();
        LOOPS.put(target.getUUID(), new Loop(lv.dimension(), now, now + LOOP_TICKS, disabledAi,
                target.getX(), target.getY(), target.getZ()));

        lv.playSound(null, target.blockPosition(), SoundEvents.ENDERMAN_STARE, SoundSource.PLAYERS, 1.0F, 0.4F);
        lv.sendParticles(ParticleTypes.REVERSE_PORTAL, target.getX(), target.getY() + 1.0, target.getZ(),
                40, 0.4, 0.8, 0.4, 0.1);
        EyeCost.apply(p); // cego por 5 s (nao e permanente)
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || LOOPS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, Loop>> it = LOOPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Loop> en = it.next();
            Loop l = en.getValue();
            ServerLevel lv = server.getLevel(l.dim());
            Entity ent = lv == null ? null : lv.getEntity(en.getKey());
            long now = lv == null ? 0L : lv.getGameTime();
            boolean finished = ent == null || !ent.isAlive() || now >= l.end();
            if (ent instanceof LivingEntity le) {
                if (finished) {
                    if (l.disabledAi() && le instanceof Mob m) m.setNoAi(false);
                } else if ((now - l.start()) > 0 && (now - l.start()) % RESET_EVERY == 0) {
                    if (le.distanceToSqr(l.x(), l.y(), l.z()) > 0.25D) loopBack(lv, le, l);
                }
            }
            if (finished) it.remove();
        }
    }

    private static void loopBack(ServerLevel lv, LivingEntity le, Loop l) {
        lv.sendParticles(ParticleTypes.REVERSE_PORTAL, le.getX(), le.getY() + 1.0, le.getZ(), 20, 0.3, 0.6, 0.3, 0.05);
        if (le instanceof ServerPlayer sp) {
            sp.connection.teleport(l.x(), l.y(), l.z(), sp.getYRot(), sp.getXRot());
        } else {
            le.moveTo(l.x(), l.y(), l.z(), le.getYRot(), le.getXRot());
        }
        le.setDeltaMovement(Vec3.ZERO);
        le.fallDistance = 0.0F;
        lv.playSound(null, le.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 0.5F);
    }

    /** Ao fechar o servidor, devolve a IA dos mobs presos (senao ficariam sem IA para sempre). */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent e) {
        MinecraftServer server = e.getServer();
        for (Map.Entry<UUID, Loop> en : LOOPS.entrySet()) {
            Loop l = en.getValue();
            ServerLevel lv = server.getLevel(l.dim());
            Entity ent = lv == null ? null : lv.getEntity(en.getKey());
            if (l.disabledAi() && ent instanceof Mob m) m.setNoAi(false);
        }
        LOOPS.clear();
    }
}
