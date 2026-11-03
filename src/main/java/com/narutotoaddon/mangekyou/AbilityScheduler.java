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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/** Efeitos que continuam depois do clique (fogo negro, Susanoo, genjutsus). */
public final class AbilityScheduler {
    private AbilityScheduler() {}

    public record Timed(ResourceKey<Level> dim, long end) {}
    public record Locked(ResourceKey<Level> dim, long end, boolean disabledAi) {}
    public record Controlled(ResourceKey<Level> dim, long end, UUID owner) {}
    /** Loop do Izanami: guarda a pose inicial e teleporta o alvo de volta periodicamente. */
    public record IzanamiLock(ResourceKey<Level> dim, long end, boolean disabledAi,
                              double x, double y, double z, float yRot, float xRot) {}

    static final Map<UUID, Timed> BURNING = new HashMap<>();
    static final Map<UUID, Locked> TSUKUYOMI = new HashMap<>();
    static final Map<UUID, Controlled> CONTROLLED = new HashMap<>();
    static final Map<UUID, IzanamiLock> IZANAMI = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        int tc = server.getTickCount();

        // Novo Amaterasu: fogo preto que segue o alvo
        if (tc % 10 == 0) {
            Iterator<Map.Entry<UUID, Timed>> it = BURNING.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Timed> en = it.next();
                ServerLevel lv = server.getLevel(en.getValue().dim());
                Entity ent = lv == null ? null : lv.getEntity(en.getKey());
                if (ent == null || !ent.isAlive() || lv.getGameTime() >= en.getValue().end()) {
                    it.remove();
                    continue;
                }
                ent.setSecondsOnFire(3);
                BlackFire.place(lv, ent.blockPosition());
                ent.hurt(lv.damageSources().magic(), 1.0F);
                lv.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, ent.getX(), ent.getY() + ent.getBbHeight() / 2,
                        ent.getZ(), 6, 0.3, 0.4, 0.3, 0.01);
            }
        }

        // Tsukuyomi + Kotoamatsukami: a cada 1 s
        if (tc % 20 == 0) {
            Iterator<Map.Entry<UUID, Locked>> it = TSUKUYOMI.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Locked> en = it.next();
                Locked l = en.getValue();
                ServerLevel lv = server.getLevel(l.dim());
                Entity ent = lv == null ? null : lv.getEntity(en.getKey());
                boolean finished = ent == null || !ent.isAlive() || lv.getGameTime() >= l.end();
                if (ent instanceof LivingEntity le) {
                    if (finished) {
                        if (l.disabledAi() && le instanceof Mob m) m.setNoAi(false);
                    } else if (le.getHealth() > 4.0F) {
                        // "tortura": dano que nunca mata sozinho
                        le.hurt(lv.damageSources().magic(), 1.5F);
                    }
                }
                if (finished) it.remove();
            }

            Iterator<Map.Entry<UUID, Controlled>> it2 = CONTROLLED.entrySet().iterator();
            while (it2.hasNext()) {
                Map.Entry<UUID, Controlled> en = it2.next();
                Controlled c = en.getValue();
                ServerLevel lv = server.getLevel(c.dim());
                Entity ent = lv == null ? null : lv.getEntity(en.getKey());
                if (!(ent instanceof Mob mob) || !mob.isAlive() || lv.getGameTime() >= c.end()) {
                    it2.remove();
                    continue;
                }
                ServerPlayer owner = server.getPlayerList().getPlayer(c.owner());
                if (owner != null && owner.level() == mob.level()) {
                    if (mob.getTarget() == owner) mob.setTarget(null);
                    if (mob.distanceToSqr(owner) > 36.0D) {
                        mob.getNavigation().moveTo(owner, 1.1D);
                    }
                }
            }

            // Izanami: loop de posicao a cada 1 s
            Iterator<Map.Entry<UUID, IzanamiLock>> it3 = IZANAMI.entrySet().iterator();
            while (it3.hasNext()) {
                Map.Entry<UUID, IzanamiLock> en = it3.next();
                IzanamiLock lock = en.getValue();
                ServerLevel lv = server.getLevel(lock.dim());
                Entity ent = lv == null ? null : lv.getEntity(en.getKey());
                boolean finished = ent == null || !ent.isAlive() || lv.getGameTime() >= lock.end();
                if (ent instanceof LivingEntity le) {
                    if (finished) {
                        if (lock.disabledAi() && le instanceof Mob m) m.setNoAi(false);
                    } else {
                        // teleporta de volta para a pose inicial (loop)
                        if (le instanceof ServerPlayer sp) {
                            sp.connection.teleport(lock.x(), lock.y(), lock.z(), lock.yRot(), lock.xRot());
                        } else {
                            le.moveTo(lock.x(), lock.y(), lock.z(), lock.yRot(), lock.xRot());
                            le.setYHeadRot(lock.yRot());
                            if (le instanceof Mob m) m.getNavigation().stop();
                        }
                        le.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                        le.fallDistance = 0.0F;
                        le.hurtMarked = true;
                        lv.sendParticles(ParticleTypes.REVERSE_PORTAL, le.getX(), le.getY() + 1.0, le.getZ(),
                                8, 0.3, 0.4, 0.3, 0.02);
                    }
                }
                if (finished) it3.remove();
            }
        }
    }

    /** Mob controlado nunca escolhe o dono como alvo. */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent e) {
        Controlled c = CONTROLLED.get(e.getEntity().getUUID());
        if (c != null && e.getNewTarget() != null && e.getNewTarget().getUUID().equals(c.owner())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onAttack(LivingAttackEvent e) {
        Entity attacker = e.getSource().getEntity();
        if (attacker == null) return;
        Controlled c = CONTROLLED.get(attacker.getUUID());
        if (c != null && e.getEntity().getUUID().equals(c.owner())) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent e) {
        BURNING.clear();
        TSUKUYOMI.clear();
        CONTROLLED.clear();
        IZANAMI.clear();
        TimeStopHandler.clear();
        RewindHandler.clear();
    }
}
