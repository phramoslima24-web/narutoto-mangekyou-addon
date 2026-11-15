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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Olho esquerdo do Madara: volta EXATAMENTE 5 segundos (100 ticks) no tempo para o jogador e para os seres vivos
 * proximos (posicao, rotacao e vida). Quem ja morreu nao e revivido.
 * O historico so e gravado enquanto o jogador tem o implante do Madara.
 *
 * Melhorias do Madara: ele nunca perde vida ao voltar no tempo, apaga o fogo, ganha resistencia/regeneracao
 * por instantes e os seres vivos ao redor ficam desorientados (lentidao) pelo choque temporal.
 */
public final class RewindHandler {
    private RewindHandler() {}

    /** Quantos ticks para tras o Rewind volta: 5 s * 20 ticks = 100 (exato). */
    public static final int REWIND_TICKS = 100;
    /**
     * O historico guarda 1 foto por tick; para a foto mais antiga estar EXATAMENTE REWIND_TICKS ticks antes da mais
     * nova sao necessarias REWIND_TICKS + 1 fotos.
     */
    private static final int HISTORY_TICKS = REWIND_TICKS + 1;
    /** Historico minimo (0,5 s) para usar o Rewind logo depois de implantar; com o historico cheio volta os 5 s inteiros. */
    private static final int MIN_HISTORY = 10;
    /** Depois disso sem ver a entidade (ticks) o historico dela e descartado: sempre maior que a janela do Rewind. */
    private static final int FORGET_TICKS = REWIND_TICKS + 40;
    private static final double RECORD_RADIUS = 32.0D;
    /** Raio do choque temporal que desorienta os seres vivos ao redor do Madara. */
    private static final double SHOCK_RADIUS = 12.0D;

    private record Snap(ResourceKey<Level> dim, double x, double y, double z, float yRot, float xRot, float health) {}

    private static final Map<UUID, ArrayDeque<Snap>> HISTORY = new HashMap<>();
    private static final Map<UUID, Long> SEEN = new HashMap<>();

    static void clear() {
        HISTORY.clear();
        SEEN.clear();
    }

    private static boolean holdsMadaraEye(ServerPlayer p) {
        return Implants.get(p) == Implant.MADARA;
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
                boolean old = now - en.getValue() > FORGET_TICKS;
                if (old) HISTORY.remove(en.getKey());
                return old;
            });
        }
    }

    static boolean rewind(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        ArrayDeque<Snap> own = HISTORY.get(p.getUUID());
        if (own == null || own.size() < MIN_HISTORY) {
            AbilityHandler.msg(p, "no_history");
            return false;
        }
        for (LivingEntity le : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RECORD_RADIUS))) {
            ArrayDeque<Snap> dq = HISTORY.get(le.getUUID());
            if (dq == null || dq.isEmpty()) continue;
            Snap s = dq.peekFirst();
            if (!s.dim().equals(lv.dimension())) continue;
            restore(le, s, le == p);
            dq.clear();
        }
        // o proprio Madara: fogo apagado, resistencia e regeneracao curtas (o tempo voltou, o corpo se recompoe)
        p.clearFire();
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1));
        // choque temporal: quem esta ao redor fica desorientado por 2 s
        for (LivingEntity le : lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(SHOCK_RADIUS),
                x -> x != p && x.isAlive())) {
            if (le instanceof Player pl && (pl.isCreative() || pl.isSpectator())) continue;
            le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
            le.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 40, 0));
        }
        lv.playSound(null, p.blockPosition(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.5F, 0.6F);
        lv.playSound(null, p.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 1.0F, 1.6F);
        lv.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(),
                120, 0.8, 1.0, 0.8, 0.15);
        for (int ring = 1; ring <= 3; ring++) {
            double r = ring * 2.5D;
            for (int a = 0; a < 360; a += 20) {
                double rad = Math.toRadians(a);
                lv.sendParticles(ParticleTypes.END_ROD, p.getX() + Math.cos(rad) * r, p.getY() + 0.5D,
                        p.getZ() + Math.sin(rad) * r, 1, 0.0, 0.1, 0.0, 0.02);
            }
        }
        return true;
    }

    private static void restore(LivingEntity le, Snap s, boolean owner) {
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
            float back = Math.min(le.getMaxHealth(), s.health());
            // o Madara nunca perde vida ao voltar no tempo (so recupera); os outros voltam a vida de antes
            le.setHealth(owner ? Math.max(le.getHealth(), back) : back);
        }
    }
}
