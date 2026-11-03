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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
    public record Izanagi(ResourceKey<Level> dim, long end) {}
    public record Looped(ResourceKey<Level> dim, long end, boolean disabledAi) {}

    static final Map<UUID, Izanagi> IZANAGI = new HashMap<>();
    static final Map<UUID, Looped> IZANAMI = new HashMap<>();
    static final Map<UUID, Timed> BURNING = new HashMap<>();
    static final Map<UUID, Locked> TSUKUYOMI = new HashMap<>();
    static final Map<UUID, Controlled> CONTROLLED = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        int tc = server.getTickCount();

        // Izanagi: janela de 5 segundos; ao acabar, aplica cegueira por apenas 5 segundos.
        Iterator<Map.Entry<UUID, Izanagi>> izanagiIt = IZANAGI.entrySet().iterator();
        while (izanagiIt.hasNext()) {
            Map.Entry<UUID, Izanagi> en = izanagiIt.next();
            Izanagi state = en.getValue();
            ServerLevel lv = server.getLevel(state.dim());
            Entity ent = lv == null ? null : lv.getEntity(en.getKey());
            if (!(ent instanceof ServerPlayer player) || !player.isAlive() || lv.getGameTime() >= state.end()) {
                if (ent instanceof ServerPlayer player && player.isAlive()) {
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("Izanagi: sua visão ficará escura por 5 segundos."), true);
                }
                izanagiIt.remove();
            }
        }

        // Izanami: libera a IA quando o ciclo de 10 segundos termina.
        Iterator<Map.Entry<UUID, Looped>> izanamiIt = IZANAMI.entrySet().iterator();
        while (izanamiIt.hasNext()) {
            Map.Entry<UUID, Looped> en = izanamiIt.next();
            Looped state = en.getValue();
            ServerLevel lv = server.getLevel(state.dim());
            Entity ent = lv == null ? null : lv.getEntity(en.getKey());
            if (ent == null || !ent.isAlive() || lv.getGameTime() >= state.end()) {
                if (state.disabledAi() && ent instanceof Mob mob) mob.setNoAi(false);
                izanamiIt.remove();
            }
        }

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
        // Izanagi cancela apenas o golpe que seria fatal durante a janela de 5 segundos.
        if (e.getEntity() instanceof ServerPlayer player) {
            Izanagi state = IZANAGI.get(player.getUUID());
            if (state != null && state.dim().equals(player.level().dimension())
                    && player.getHealth() > 0.0F && e.getAmount() >= player.getHealth()) {
                e.setCanceled(true);
                IZANAGI.remove(player.getUUID());
                player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.5F));
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.TOTEM_USE,
                        net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.8F);
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0,
                            player.getZ(), 50, 0.5, 0.8, 0.5, 0.08);
                }
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Izanagi reescreveu sua realidade! Cegueira: 5 segundos."), true);
                return;
            }
        }
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
        IZANAGI.clear();
        IZANAMI.clear();
        TimeStopHandler.clear();
        RewindHandler.clear();
    }
}
