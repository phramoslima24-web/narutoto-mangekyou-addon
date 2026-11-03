package com.narutotoaddon.mangekyouizanagi;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
 */
public final class TimeStopHandler {
    private TimeStopHandler() {}

    /** Duracao do tempo parado: 10 segundos. */
    static final int DURATION_TICKS = 200;

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
        for (ServerPlayer other : lv.players()) {
            other.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".time_stopped"), true);
        }
        return true;
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
            Entity ownerVehicle = owner.getVehicle();
            for (Entity ent : lv.getAllEntities()) {
                if (ent == owner || ent == ownerVehicle || ent.hasPassenger(owner)) continue;
                if (ent instanceof ServerPlayer sp) {
                    if (refreshEffects) {
                        sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 255, false, false, false));
                        sp.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 128, false, false, false));
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
