package com.narutotoaddon.mangekyou;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Olho esquerdo do Madara: volta 2 segundos no tempo para o jogador e para os seres vivos proximos
 * (posicao, rotacao e vida). Quem ja morreu nao e revivido.
 * O historico so e gravado enquanto o jogador carrega o Mangekyou do Madara no inventario.
 */
public final class RewindHandler {
    private RewindHandler() {}

    private static final int HISTORY_TICKS = 40; // 2 s
    private static final double RECORD_RADIUS = 32.0D;

    private record Snap(ResourceKey<Level> dim, double x, double y, double z, float yRot, float xRot, float health) {}

    private static final Map<UUID, ArrayDeque<Snap>> HISTORY = new HashMap<>();
    private static final Map<UUID, Long> SEEN = new HashMap<>();

    static void clear() {
        HISTORY.clear();
        SEEN.clear();
    }

    private static boolean holdsMadaraEye(ServerPlayer p) {
        for (ItemStack s : p.getInventory().items) {
            if (s.getItem() instanceof MangekyouItem m && m.kind == MangekyouItem.Kind.MADARA) return true;
        }
        ItemStack off = p.getOffhandItem();
        return off.getItem() instanceof MangekyouItem m && m.kind == MangekyouItem.Kind.MADARA;
    }

    private static void record(LivingEntity le, long now, Set<UUID> done) {
        if (!le.isAlive() || !done.add(le.getUUID())) return;
        ArrayDeque<Snap> dq = HISTORY.computeIfAbsent(le.getUUID(), k -> new ArrayDeque<>());
        dq.addLast(new Snap(le.level().dimension(), le.getX(), le.getY(), le.getZ(),
                le.getYRot(), le.getXRot(), le.getHealth()));
        while (dq.size() > HISTORY_TICKS) dq.removeFirst();
        SEEN.put(le.getUUID(), now);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Set<UUID> done = new HashSet<>();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!holdsMadaraEye(p)) continue;
            ServerLevel lv = (ServerLevel) p.level();
            long now = lv.getGameTime();
            record(p, now, done);
            for (LivingEntity le : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RECORD_RADIUS))) {
                record(le, now, done);
            }
        }

        if (server.getTickCount() % 100 == 0) {
            long now = server.overworld().getGameTime();
            SEEN.entrySet().removeIf(en -> {
                boolean old = now - en.getValue() > 100;
                if (old) HISTORY.remove(en.getKey());
                return old;
            });
        }
    }

    static boolean rewind(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        ArrayDeque<Snap> own = HISTORY.get(p.getUUID());
        if (own == null || own.size() < 10) {
            AbilityHandler.msg(p, "no_history");
            return false;
        }
        for (LivingEntity le : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RECORD_RADIUS))) {
            ArrayDeque<Snap> dq = HISTORY.get(le.getUUID());
            if (dq == null || dq.isEmpty()) continue;
            Snap s = dq.peekFirst();
            if (!s.dim().equals(lv.dimension())) continue;
            restore(le, s);
            dq.clear();
        }
        lv.playSound(null, p.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.5F, 0.6F);
        lv.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(),
                60, 0.6, 0.8, 0.6, 0.1);
        return true;
    }

    private static void restore(LivingEntity le, Snap s) {
        if (le instanceof ServerPlayer sp) {
            sp.connection.teleport(s.x(), s.y(), s.z(), s.yRot(), s.xRot());
        } else {
            le.moveTo(s.x(), s.y(), s.z(), s.yRot(), s.xRot());
            le.setYHeadRot(s.yRot());
            if (le instanceof Mob m) m.getNavigation().stop();
        }
        le.setDeltaMovement(Vec3.ZERO);
        le.fallDistance = 0.0F;
        le.hurtMarked = true;
        if (s.health() > 0.0F) {
            le.setHealth(Math.min(le.getMaxHealth(), s.health()));
        }
    }
}
