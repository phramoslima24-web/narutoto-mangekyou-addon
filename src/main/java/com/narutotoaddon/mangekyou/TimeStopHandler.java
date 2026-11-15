package com.narutotoaddon.mangekyou;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Olho direito do Madara: para o tempo no mundo atual, exceto o jogador dono.
 * - Mobs/NPCs: o tick deles e cancelado (ficam congelados).
 * - Projeteis, itens, TNT, blocos caindo: velocidade zerada e gravidade desligada.
 * - Outros jogadores: lentidao 255 + sem pulo + sem interagir/atacar/quebrar blocos.
 *
 * Melhorias do Madara: 15 s de duracao (antes 10 s), onda de choque ao parar/voltar o tempo, os seres vivos
 * congelados brilham, o Madara ganha velocidade, forca e resistencia e seus golpes em quem esta congelado
 * causam +75% de dano. A recarga continua a mesma (Ability.TIME_STOP).
 */
public final class TimeStopHandler {
    private TimeStopHandler() {}

    /** Duracao do tempo parado: 15 segundos. */
    static final int DURATION_TICKS = 300;
    /** Dano extra do Madara em quem esta congelado no tempo. */
    private static final float FROZEN_DAMAGE_MULT = 1.75F;
    /** Raio em que os seres vivos congelados brilham (blocos). */
    private static final double GLOW_RADIUS = 48.0D;

    private static final class Stop {
        final UUID owner;
        final long end;
        final Set<UUID> gravityChanged = new HashSet<>();

        Stop(UUID owner, long end) {
            this.owner = owner;
            this.end = end;
        }
    }

    private static final Map<ResourceKey<Level>, Stop> STOPS = new HashMap<>();

    static void clear() {
        STOPS.clear();
    }

    static boolean start(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        if (STOPS.containsKey(lv.dimension())) {
            AbilityHandler.msg(p, "already_stopped");
            return false;
        }
        STOPS.put(lv.dimension(), new Stop(p.getUUID(), lv.getGameTime() + DURATION_TICKS));
        lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 2.0F, 0.5F);
        lv.playSound(null, p.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.8F, 0.5F);
        shockwave(lv, p.getX(), p.getY() + 0.5D, p.getZ(), ParticleTypes.REVERSE_PORTAL);
        for (ServerPlayer other : lv.players()) {
            other.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".time_stopped"), true);
        }
        return true;
    }

    /** Aneis de particulas que se espalham a partir do Madara (ao parar e ao retomar o tempo). */
    private static void shockwave(ServerLevel lv, double x, double y, double z,
                                  net.minecraft.core.particles.ParticleOptions type) {
        for (int ring = 1; ring <= 4; ring++) {
            double r = ring * 3.0D;
            for (int a = 0; a < 360; a += 15) {
                double rad = Math.toRadians(a);
                lv.sendParticles(type, x + Math.cos(rad) * r, y, z + Math.sin(rad) * r, 1, 0.0, 0.15, 0.0, 0.03);
            }
        }
    }

    private static boolean isFrozenFor(Entity e) {
        if (e.level().isClientSide()) return false;
        Stop s = STOPS.get(e.level().dimension());
        return s != null && !s.owner.equals(e.getUUID());
    }

    private static boolean blocked(Player p) {
        return isFrozenFor(p);
    }

    private static void release(MinecraftServer server, ResourceKey<Level> dim, Stop stop) {
        ServerLevel lv = server.getLevel(dim);
        if (lv == null) return;
        for (UUID id : stop.gravityChanged) {
            Entity ent = lv.getEntity(id);
            if (ent != null) ent.setNoGravity(false);
        }
        lv.playSound(null, lv.getSharedSpawnPos(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.8F);
        ServerPlayer owner = server.getPlayerList().getPlayer(stop.owner);
        if (owner != null && owner.level() == lv) {
            shockwave(lv, owner.getX(), owner.getY() + 0.5D, owner.getZ(), ParticleTypes.END_ROD);
        }
        for (ServerPlayer other : lv.players()) {
            other.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".time_resumed"), true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || STOPS.isEmpty()) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Iterator<Map.Entry<ResourceKey<Level>, Stop>> it = STOPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceKey<Level>, Stop> en = it.next();
            Stop stop = en.getValue();
            ServerLevel lv = server.getLevel(en.getKey());
            ServerPlayer owner = server.getPlayerList().getPlayer(stop.owner);
            if (lv == null || owner == null || !owner.isAlive() || lv.getGameTime() >= stop.end) {
                release(server, en.getKey(), stop);
                it.remove();
                continue;
            }
            boolean refreshEffects = server.getTickCount() % 10 == 0;
            if (refreshEffects) {
                // o dono do tempo: mais rapido, mais forte e mais resistente enquanto o tempo esta parado
                owner.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 1, false, false, true));
                owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 30, 1, false, false, true));
                owner.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 0, false, false, true));
            }
            Entity ownerVehicle = owner.getVehicle();
            for (Entity ent : lv.getAllEntities()) {
                if (ent == owner || ent == ownerVehicle || ent.hasPassenger(owner)) continue;
                if (ent instanceof ServerPlayer sp) {
                    if (refreshEffects) {
                        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, false, false));
                        sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 128, false, false, false));
                    }
                } else if (ent instanceof LivingEntity frozen) {
                    // congelados no tempo: brilham, para o Madara enxergar todos (efeito dura enquanto o tick esta parado)
                    if (refreshEffects && frozen.isAlive() && frozen.distanceToSqr(owner) < GLOW_RADIUS * GLOW_RADIUS) {
                        frozen.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, false));
                    }
                } else if (!(ent instanceof LivingEntity)) {
                    ent.setDeltaMovement(Vec3.ZERO);
                    if (!ent.isNoGravity()) {
                        ent.setNoGravity(true);
                        stop.gravityChanged.add(ent.getUUID());
                    }
                    if (ent instanceof PrimedTnt tnt) {
                        tnt.setFuse(tnt.getFuse() + 1);
                    }
                }
            }
        }
    }

    /** Congela o tick de todos os seres vivos (menos jogadores) no mundo parado. */
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent e) {
        LivingEntity le = e.getEntity();
        if (le instanceof Player) return;
        if (isFrozenFor(le)) {
            e.setCanceled(true);
        }
    }

    /** Como o tick esta cancelado, o tempo de invulnerabilidade nao cai: zera quando o Madara bate. */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent e) {
        Entity src = e.getSource().getEntity();
        if (src instanceof Player p && !p.level().isClientSide() && !isFrozenFor(p)
                && STOPS.containsKey(p.level().dimension()) && isFrozenFor(e.getEntity())) {
            e.getEntity().invulnerableTime = 0;
        }
    }

    /** Golpes do Madara em quem esta congelado no tempo causam mais dano. */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent e) {
        Entity src = e.getSource().getEntity();
        if (src instanceof Player p && !p.level().isClientSide() && !isFrozenFor(p)
                && STOPS.containsKey(p.level().dimension()) && isFrozenFor(e.getEntity())) {
            e.setAmount(e.getAmount() * FROZEN_DAMAGE_MULT);
            if (p.level() instanceof ServerLevel lv) {
                LivingEntity t = e.getEntity();
                lv.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + t.getBbHeight() / 2, t.getZ(),
                        12, 0.3, 0.4, 0.3, 0.2);
            }
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (blocked(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock e) {
        if (blocked(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem e) {
        if (blocked(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (blocked(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent e) {
        if (blocked(e.getEntity())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (blocked(e.getPlayer())) e.setCanceled(true);
    }
}
