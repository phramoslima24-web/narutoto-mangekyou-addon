package com.narutotoaddon.mangekyou;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.joml.Vector3f;

/**
 * Obito Uchiha: passiva das celulas de Hashirama, os tres modos (Tobi, Obito da Guerra, Jinchuuriki do Dez Caudas) e as
 * Esferas da Busca da Verdade. Os multiplicadores sao aplicados sobre o sistema REAL do Narutoto sem mexer nos pontos
 * de atributo do jogador: dano (corpo a corpo e jutsus, todo dano causado pelo jogador) e defesa pelo evento de dano,
 * velocidade e vida maxima por modificadores temporarios de atributo (somem sozinhos ao desligar o modo).
 */
public final class ObitoHandler {
    private ObitoHandler() {}

    /** Modo salvo no jogador: 0 = nenhum, 1 = Tobi, 2 = Obito da Guerra, 3 = Jinchuuriki do Dez Caudas. */
    public static final String TAG_MODE = "mkaddon_obito_mode";
    private static final String TAG_FLY = "mkaddon_obito_fly";

    private static final UUID SPEED_ID = UUID.fromString("5c0f1b6e-3a8d-4c21-9b0e-0b17e0000001");
    private static final UUID HEALTH_ID = UUID.fromString("5c0f1b6e-3a8d-4c21-9b0e-0b17e0000002");

    /** Multiplicadores (dano, defesa, velocidade), custo e ganho de chakra por segundo, vida extra. */
    public enum Mode {
        NONE(1.0F, 1.0F, 1.0F, 0, 0, 0, null),
        TOBI(1.2F, 1.1F, 1.5F, 2, 0, 0, Ability.MODE_TOBI),
        WAR(1.6F, 1.5F, 1.6F, 4, 3, 4, Ability.MODE_WAR),
        JINCHUURIKI(2.5F, 2.2F, 2.0F, 12, 8, 10, Ability.MODE_JINCHUURIKI);

        public final float damage, defense, speed;
        public final int drainPerSec, regenPerSec, extraHealth;
        public final Ability ability;

        Mode(float damage, float defense, float speed, int drain, int regen, int extraHealth, Ability ability) {
            this.damage = damage;
            this.defense = defense;
            this.speed = speed;
            this.drainPerSec = drain;
            this.regenPerSec = regen;
            this.extraHealth = extraHealth;
            this.ability = ability;
        }

        public static Mode of(int i) {
            Mode[] v = values();
            return i >= 0 && i < v.length ? v[i] : NONE;
        }
    }

    public static Mode mode(net.minecraft.world.entity.Entity e) {
        return Mode.of(e.getPersistentData().getInt(TAG_MODE));
    }

    // ------------------------------------------------------------------ Modos
    /** Liga/desliga um modo. Retorna true so quando LIGOU (cobra chakra e recarga); desligar e gratis. */
    public static boolean toggleMode(ServerPlayer p, Ability a) {
        Mode target = a == Ability.MODE_TOBI ? Mode.TOBI : a == Ability.MODE_WAR ? Mode.WAR : Mode.JINCHUURIKI;
        if (mode(p) == target) {
            setMode(p, Mode.NONE);
            return false;
        }
        double stage = Implants.stage(p);
        if (target == Mode.WAR && stage < 3.0D && !p.getAbilities().instabuild) {
            Implants.msg(p, "mode_needs_eternal");
            return false;
        }
        if (target == Mode.JINCHUURIKI && stage < 4.0D && !p.getAbilities().instabuild) {
            Implants.msg(p, "mode_needs_rinnegan");
            return false;
        }
        setMode(p, target);
        return true;
    }

    private static void setMode(ServerPlayer p, Mode m) {
        p.getPersistentData().putInt(TAG_MODE, m.ordinal());
        apply(p, m);
        if (m == Mode.NONE) {
            p.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".mode_off"), true);
        } else {
            p.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".mode_on",
                    Component.translatable(m.ability.jutsuKey())), true);
            ServerLevel lv = (ServerLevel) p.level();
            lv.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
            lv.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 40, 0.5, 0.8, 0.5, 0.3);
        }
    }

    /** Aplica (ou remove) velocidade, vida extra e voo do modo. Os modificadores sao temporarios. */
    private static void apply(ServerPlayer p, Mode m) {
        AttributeInstance sp = p.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) {
            sp.removeModifier(SPEED_ID);
            if (m != Mode.NONE) {
                sp.addTransientModifier(new AttributeModifier(SPEED_ID, "mkaddon_obito_speed", m.speed - 1.0D,
                        AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
        AttributeInstance hp = p.getAttribute(Attributes.MAX_HEALTH);
        if (hp != null) {
            hp.removeModifier(HEALTH_ID);
            if (m.extraHealth > 0) {
                hp.addTransientModifier(new AttributeModifier(HEALTH_ID, "mkaddon_obito_health", m.extraHealth,
                        AttributeModifier.Operation.ADDITION));
            }
        }
        CompoundTag d = p.getPersistentData();
        if (m == Mode.JINCHUURIKI) {
            // Jinchuuriki do Dez Caudas: capacidade de voo
            if (!p.getAbilities().mayfly) {
                p.getAbilities().mayfly = true;
                d.putBoolean(TAG_FLY, true);
                p.onUpdateAbilities();
            }
        } else if (d.getBoolean(TAG_FLY)) {
            d.remove(TAG_FLY);
            if (!p.isCreative() && !p.isSpectator()) {
                p.getAbilities().mayfly = false;
                p.getAbilities().flying = false;
                p.onUpdateAbilities();
            }
        }
    }

    private static boolean hasModifiers(ServerPlayer p) {
        AttributeInstance sp = p.getAttribute(Attributes.MOVEMENT_SPEED);
        return sp != null && sp.getModifier(SPEED_ID) != null;
    }

    // ------------------------------------------------------- Tick (1x por segundo)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient() || !(e.player instanceof ServerPlayer p)) return;
        if (p.tickCount % 20 != 0 || !Implants.has(p, Implant.OBITO)) return;
        boolean hashirama = Bridge.getBool(p, "if_hashirama_cells", false);

        // Celulas de Hashirama: recuperacao moderada (nao deixa o jogador invencivel)
        if (p.getHealth() < p.getMaxHealth() * 0.75F && p.getHealth() > 0) {
            p.heal(hashirama ? 1.0F : 0.5F);
        }

        Mode m = mode(p);
        if (m == Mode.NONE) {
            if (hasModifiers(p) || p.getPersistentData().getBoolean(TAG_FLY)) apply(p, Mode.NONE);
            return;
        }
        // o modo cai se a evolucao exigida foi perdida (ex.: Rinnegan desfeito)
        double stage = Implants.stage(p);
        if (!p.getAbilities().instabuild
                && ((m == Mode.WAR && stage < 3.0D) || (m == Mode.JINCHUURIKI && stage < 4.0D) || stage < 2.0D)) {
            setMode(p, Mode.NONE);
            return;
        }
        if (!hasModifiers(p)) apply(p, m); // modificadores temporarios: reaplica apos login/morte/dimensao
        if (!p.getAbilities().instabuild) {
            double chakra = Bridge.getDouble(p, "chakra", -1);
            if (chakra >= 0) {
                double max = Bridge.getDouble(p, "chakra_maximum", chakra);
                double next = Math.min(max, chakra + m.regenPerSec) - m.drainPerSec;
                if (next < 0) {
                    setMode(p, Mode.NONE);
                    Implants.msg(p, "mode_no_chakra");
                    return;
                }
                if (next != chakra) {
                    Bridge.setDouble(p, "chakra", next);
                    Bridge.sync(p);
                }
            }
        }
        ServerLevel lv = (ServerLevel) p.level();
        if (m == Mode.JINCHUURIKI) {
            lv.sendParticles(new DustParticleOptions(new Vector3f(0.05F, 0.05F, 0.05F), 1.2F), p.getX(), p.getY() + 1.0,
                    p.getZ(), 12, 0.6, 0.9, 0.6, 0.0);
        } else if (m == Mode.WAR) {
            lv.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(), 4, 0.4, 0.8, 0.4, 0.01);
        } else {
            lv.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 6, 0.4, 0.8, 0.4, 0.2);
        }
    }

    // ------------------------------------------------- Dano e defesa dos modos
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent e) {
        float amount = e.getAmount();
        // defesa: celulas de Hashirama (passiva) + modo
        if (e.getEntity() instanceof ServerPlayer victim && Implants.has(victim, Implant.OBITO)
                && !e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            boolean hashirama = Bridge.getBool(victim, "if_hashirama_cells", false);
            amount *= hashirama ? 0.88F : 0.92F;
            amount /= mode(victim).defense;
        }
        // dano: tudo que o jogador causa (corpo a corpo e jutsus do Narutoto, que tem o jogador como autor)
        if (e.getSource().getEntity() instanceof ServerPlayer attacker && attacker != e.getEntity()
                && Implants.has(attacker, Implant.OBITO)) {
            amount *= mode(attacker).damage;
        }
        e.setAmount(amount);
    }

    // ------------------------------------------- Esferas da Busca da Verdade
    private static final class Volley {
        final UUID owner;
        final ResourceKey<Level> dim;
        final int count;
        Vec3 aim;
        LivingEntity target;
        int age;

        Volley(ServerPlayer p, int count) {
            this.owner = p.getUUID();
            this.dim = p.level().dimension();
            this.count = count;
        }
    }

    private static final List<Volley> VOLLEYS = new ArrayList<>();
    private static final int ORBIT_TICKS = 40, FLIGHT_TICKS = 12;

    /** So no Jinchuuriki do Dez Caudas: 9 esferas orbitam o Obito e depois voam no alvo. */
    public static boolean truthSeekingBalls(ServerPlayer p) {
        if (mode(p) != Mode.JINCHUURIKI && !p.getAbilities().instabuild) {
            Implants.msg(p, "balls_need_jinchuuriki");
            return false;
        }
        VOLLEYS.add(new Volley(p, 9));
        ((ServerLevel) p.level()).playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS,
                1.0F, 0.5F);
        return true;
    }

    private static Vec3 orbPos(Player p, int i, int count, int age) {
        double ang = (Math.PI * 2.0D / count) * i + age * 0.25D;
        return new Vec3(p.getX() + Math.cos(ang) * 2.2D, p.getY() + 1.6D + Math.sin(age * 0.15D + i) * 0.25D,
                p.getZ() + Math.sin(ang) * 2.2D);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || VOLLEYS.isEmpty() || net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() == null) return;
        var server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        Iterator<Volley> it = VOLLEYS.iterator();
        while (it.hasNext()) {
            Volley v = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(v.owner);
            ServerLevel lv = p == null ? null : server.getLevel(v.dim);
            if (p == null || lv == null || !p.isAlive() || p.level() != lv) {
                it.remove();
                continue;
            }
            v.age++;
            DustParticleOptions dark = new DustParticleOptions(new Vector3f(0.02F, 0.02F, 0.02F), 1.6F);
            if (v.age <= ORBIT_TICKS) {
                for (int i = 0; i < v.count; i++) {
                    Vec3 o = orbPos(p, i, v.count, v.age);
                    lv.sendParticles(dark, o.x, o.y, o.z, 2, 0.05, 0.05, 0.05, 0.0);
                }
                if (v.age == ORBIT_TICKS) {
                    v.target = Targeting.livingInSight(p, 48.0D);
                    v.aim = v.target != null ? v.target.position().add(0, v.target.getBbHeight() * 0.5D, 0)
                            : p.getEyePosition().add(p.getLookAngle().scale(30.0D));
                }
                continue;
            }
            int f = v.age - ORBIT_TICKS;
            double t = Math.min(1.0D, f / (double) FLIGHT_TICKS);
            if (v.target != null && v.target.isAlive()) v.aim = v.target.position().add(0, v.target.getBbHeight() * 0.5D, 0);
            for (int i = 0; i < v.count; i++) {
                Vec3 from = orbPos(p, i, v.count, ORBIT_TICKS);
                Vec3 pos = from.lerp(v.aim, t);
                lv.sendParticles(dark, pos.x, pos.y, pos.z, 2, 0.05, 0.05, 0.05, 0.0);
            }
            if (f >= FLIGHT_TICKS) {
                explode(p, lv, v.aim);
                it.remove();
            }
        }
    }

    private static void explode(ServerPlayer p, ServerLevel lv, Vec3 at) {
        lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        lv.sendParticles(ParticleTypes.SQUID_INK, at.x, at.y, at.z, 60, 1.5, 1.2, 1.5, 0.08);
        lv.playSound(null, net.minecraft.core.BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.6F);
        float dmg = 24.0F + (float) (Bridge.getDouble(p, "attribute_dojutsu", 0) * 2.0D);
        for (LivingEntity le : lv.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(at, at).inflate(4.0D),
                x -> x != p && x.isAlive())) {
            if (le instanceof Player pl && (pl.isCreative() || pl.isSpectator())) continue;
            le.hurt(lv.damageSources().indirectMagic(p, p), dmg);
        }
    }
}
