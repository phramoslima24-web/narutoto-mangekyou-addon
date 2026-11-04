package com.narutotoaddon.mangekyou;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
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
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Rinne Sharingan: Tsukuyomi Infinito, Raizes da Arvore Divina (Shinju), Amenominaka
 * e os poderes oculares combinados (passivo: visao noturna + chance de prever e desviar de golpes).
 */
public final class RinneAbilities {
    private RinneAbilities() {}

    /** Se true, o Tsukuyomi Infinito so funciona a noite, no Overworld, olhando para o ceu aberto (a lua). */
    static final boolean REQUIRE_MOON = true;
    /** Raio do Tsukuyomi Infinito ("planetario" dentro do que o servidor carrega). */
    static final double MUGEN_RADIUS = 64.0D;
    static final int MUGEN_TICKS = 300;      // 15 s
    static final int ROOTS_TICKS = 300;      // 15 s
    static final double REALM_RADIUS = 12.0D;
    static final int REALM_TICKS = 400;      // 20 s
    /** Chance (0 a 1) de prever e desviar de um golpe com o Rinne Sharingan ativo. */
    static final float DODGE_CHANCE = 0.25F;

    /** Casulo de raizes: vitima, quando acaba, dono (recebe a vida drenada) e se a IA foi desligada por ele. */
    public record Cocoon(ResourceKey<Level> dim, long end, UUID owner, boolean disabledAi) {}
    /** Dimensao alternativa ativa em volta de quem lancou o Amenominaka. */
    public record Dominion(ResourceKey<Level> dim, long end, int realm) {}

    static final Map<UUID, Cocoon> COCOONED = new HashMap<>();
    /** Jogadores presos no Tsukuyomi Infinito: o chakra deles e sugado para o dono. */
    static final Map<UUID, Cocoon> DRAINED = new HashMap<>();
    static final Map<UUID, Dominion> DOMINIONS = new HashMap<>();

    private static final int LAVA = 0, ICE = 1, GRAVITY = 2, SAND = 3;
    private static final String[] REALM_KEYS = {"realm_lava", "realm_ice", "realm_gravity", "realm_sand"};

    private static boolean affectable(LivingEntity e, ServerPlayer caster) {
        if (e == caster || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand) return false;
        return !(e instanceof Player pl && pl.isCreative());
    }

    private static ParticleOptions rootParticle() {
        return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MANGROVE_ROOTS.defaultBlockState());
    }

    // ------------------------------------------------------ Tsukuyomi Infinito
    static boolean mugenTsukuyomi(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        if (REQUIRE_MOON) {
            long t = lv.getDayTime() % 24000L;
            boolean night = t >= 13000L && t <= 23000L;
            boolean openSky = lv.canSeeSky(p.blockPosition().above());
            // refletir o olho na lua: noite, ceu aberto e olhando para cima
            if (lv.dimension() != Level.OVERWORLD || !night || !openSky || p.getXRot() > -40.0F) {
                AbilityHandler.msg(p, "need_moon");
                return false;
            }
        }
        List<LivingEntity> victims = lv.getEntitiesOfClass(LivingEntity.class,
                p.getBoundingBox().inflate(MUGEN_RADIUS), e -> affectable(e, p));
        if (victims.isEmpty()) {
            AbilityHandler.msg(p, "no_target");
            return false;
        }
        for (LivingEntity v : victims) {
            entomb(lv, v, MUGEN_TICKS);
            if (v instanceof ServerPlayer) {
                DRAINED.put(v.getUUID(), new Cocoon(lv.dimension(), lv.getGameTime() + MUGEN_TICKS, p.getUUID(), false));
            }
        }
        // o conjurador se alimenta do chakra drenado
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, MUGEN_TICKS, 0));
        lv.playSound(null, p.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 2.0F, 0.5F);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".mugen_done", victims.size()));
        return true;
    }

    /** Prende a vitima no sonho eterno (reaproveita o mapa do Tsukuyomi: tortura que nunca mata sozinha). */
    private static void entomb(ServerLevel lv, LivingEntity t, int dur) {
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dur, 0));
        t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, dur, 0));
        t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, dur, 0));
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 255));
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 4));
        t.addEffect(new MobEffectInstance(MobEffects.HUNGER, dur, 2)); // "sugando o chakra"
        boolean disabledAi = false;
        if (t instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            disabledAi = true;
        }
        AbilityScheduler.Locked prev = AbilityScheduler.TSUKUYOMI.get(t.getUUID());
        if (prev != null && prev.disabledAi()) disabledAi = true; // nao deixa a IA presa se ja estava desligada por nos
        AbilityScheduler.TSUKUYOMI.put(t.getUUID(),
                new AbilityScheduler.Locked(lv.dimension(), lv.getGameTime() + dur, disabledAi));
        lv.sendParticles(ParticleTypes.SOUL, t.getX(), t.getY() + 1, t.getZ(), 8, 0.4, 0.6, 0.4, 0.02);
    }

    // ------------------------------------------- Raizes da Arvore Divina (Shinju)
    static boolean shinjuRoots(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        Set<LivingEntity> victims = new LinkedHashSet<>();
        // as vitimas do Tsukuyomi Infinito primeiro...
        victims.addAll(lv.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(MUGEN_RADIUS),
                x -> affectable(x, p) && AbilityScheduler.TSUKUYOMI.containsKey(x.getUUID())));
        // ...senao, o alvo na mira e quem estiver colado nele
        if (victims.isEmpty()) {
            LivingEntity t = Targeting.livingInSight(p, 32.0D);
            if (t != null && affectable(t, p)) {
                victims.add(t);
                victims.addAll(lv.getEntitiesOfClass(LivingEntity.class, t.getBoundingBox().inflate(4.0D),
                        x -> affectable(x, p)));
            }
        }
        if (victims.isEmpty()) {
            AbilityHandler.msg(p, "no_victims");
            return false;
        }
        for (LivingEntity v : victims) cocoon(lv, p, v);
        lv.playSound(null, p.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 1.5F, 0.5F);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".roots_done", victims.size()));
        return true;
    }

    private static void cocoon(ServerLevel lv, ServerPlayer owner, LivingEntity v) {
        v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ROOTS_TICKS, 255));
        v.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, ROOTS_TICKS, 2));
        v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ROOTS_TICKS, 2));
        boolean disabledAi = false;
        if (v instanceof Mob mob && !mob.isNoAi()) {
            mob.setNoAi(true);
            disabledAi = true;
        }
        Cocoon prev = COCOONED.get(v.getUUID());
        if (prev != null && prev.disabledAi()) disabledAi = true;
        COCOONED.put(v.getUUID(), new Cocoon(lv.dimension(), lv.getGameTime() + ROOTS_TICKS, owner.getUUID(), disabledAi));
        lv.sendParticles(rootParticle(), v.getX(), v.getY() + v.getBbHeight() / 2, v.getZ(),
                40, 0.4, v.getBbHeight() / 2, 0.4, 0.05);
    }

    // ------------------------------------------------------------- Amenominaka
    static boolean amenominaka(ServerPlayer p) {
        ServerLevel lv = (ServerLevel) p.level();
        int realm = p.getRandom().nextInt(REALM_KEYS.length);
        DOMINIONS.put(p.getUUID(), new Dominion(lv.dimension(), lv.getGameTime() + REALM_TICKS, realm));
        applyRealm(lv, p, realm);
        lv.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 1.0F, 0.6F);
        lv.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 80, 1.5, 1.0, 1.5, 0.2);
        p.sendSystemMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".amenominaka",
                Component.translatable("msg." + MangekyouAddon.MODID + "." + REALM_KEYS[realm])));
        return true;
    }

    private static ParticleOptions realmParticle(int realm) {
        return switch (realm) {
            case LAVA -> ParticleTypes.LAVA;
            case ICE -> ParticleTypes.SNOWFLAKE;
            case GRAVITY -> ParticleTypes.ASH;
            default -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState());
        };
    }

    /** Aplicado a cada segundo a quem esta perto do conjurador (o conjurador fica imune ao proprio dominio). */
    private static void applyRealm(ServerLevel lv, ServerPlayer caster, int realm) {
        if (realm == LAVA) {
            caster.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 60, 0, false, false));
        }
        List<LivingEntity> list = lv.getEntitiesOfClass(LivingEntity.class,
                caster.getBoundingBox().inflate(REALM_RADIUS), x -> affectable(x, caster));
        for (LivingEntity t : list) {
            switch (realm) {
                case LAVA -> t.setSecondsOnFire(4);
                case ICE -> {
                    t.setTicksFrozen(t.getTicksRequiredToFreeze() + 40);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                }
                case GRAVITY -> {
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 4));
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1));
                    Vec3 dm = t.getDeltaMovement();
                    t.setDeltaMovement(dm.x, Math.min(dm.y, 0.0D) - 0.6D, dm.z); // puxa para o chao
                    t.hurtMarked = true;
                    t.fallDistance += 3.0F;
                }
                default -> {
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0));
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                }
            }
            lv.sendParticles(realmParticle(realm), t.getX(), t.getY() + 1, t.getZ(), 10, 0.4, 0.6, 0.4, 0.05);
        }
        // ambientacao da dimensao em volta do conjurador
        lv.sendParticles(realmParticle(realm), caster.getX(), caster.getY() + 1, caster.getZ(),
                40, REALM_RADIUS / 2, 1.5, REALM_RADIUS / 2, 0.05);
    }

    // ----------------------------------------------------------------- tick (1 s)
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getTickCount() % 20 != 0) return;

        // casulos de raizes: drenam a vida da vitima (sem matar) para o dono
        Iterator<Map.Entry<UUID, Cocoon>> it = COCOONED.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Cocoon> en = it.next();
            Cocoon c = en.getValue();
            ServerLevel lv = server.getLevel(c.dim());
            Entity ent = lv == null ? null : lv.getEntity(en.getKey());
            boolean finished = ent == null || !ent.isAlive() || lv.getGameTime() >= c.end();
            if (ent instanceof LivingEntity le) {
                if (finished) {
                    if (c.disabledAi() && le instanceof Mob m) m.setNoAi(false);
                } else {
                    if (le.getHealth() > 4.0F) {
                        le.hurt(lv.damageSources().magic(), 1.0F);
                        ServerPlayer owner = server.getPlayerList().getPlayer(c.owner());
                        if (owner != null && owner.isAlive()) owner.heal(1.0F);
                    }
                    lv.sendParticles(rootParticle(), le.getX(), le.getY() + le.getBbHeight() / 2, le.getZ(),
                            12, 0.3, le.getBbHeight() / 2, 0.3, 0.02);
                }
            }
            if (finished) it.remove();
        }

        // Tsukuyomi Infinito: suga 10% do chakra maximo da vitima por segundo e passa para o dono
        Iterator<Map.Entry<UUID, Cocoon>> it3 = DRAINED.entrySet().iterator();
        while (it3.hasNext()) {
            Map.Entry<UUID, Cocoon> en = it3.next();
            Cocoon c = en.getValue();
            ServerLevel lv = server.getLevel(c.dim());
            ServerPlayer victim = server.getPlayerList().getPlayer(en.getKey());
            if (victim == null || lv == null || !victim.isAlive() || lv.getGameTime() >= c.end()) {
                it3.remove();
                continue;
            }
            double have = Bridge.getDouble(victim, "chakra", -1);
            if (have <= 0) continue;
            double max = Bridge.getDouble(victim, "chakra_maximum", have);
            double take = Math.min(have, Math.max(1.0D, max * 0.10D));
            Bridge.setDouble(victim, "chakra", have - take);
            Bridge.sync(victim);
            ServerPlayer owner = server.getPlayerList().getPlayer(c.owner());
            if (owner != null) {
                double oh = Bridge.getDouble(owner, "chakra", -1);
                if (oh >= 0) {
                    double om = Bridge.getDouble(owner, "chakra_maximum", oh + take);
                    Bridge.setDouble(owner, "chakra", Math.min(om, oh + take));
                    Bridge.sync(owner);
                }
            }
        }

        // dimensoes do Amenominaka
        Iterator<Map.Entry<UUID, Dominion>> it2 = DOMINIONS.entrySet().iterator();
        while (it2.hasNext()) {
            Map.Entry<UUID, Dominion> en = it2.next();
            Dominion d = en.getValue();
            ServerPlayer caster = server.getPlayerList().getPlayer(en.getKey());
            ServerLevel lv = server.getLevel(d.dim());
            if (caster == null || lv == null || !caster.isAlive() || caster.level() != lv
                    || lv.getGameTime() >= d.end()) {
                it2.remove();
                continue;
            }
            applyRealm(lv, caster, d.realm());
        }
    }

    // ------------------------------------------------ poderes oculares combinados
    private static boolean rinneActive(ServerPlayer p) {
        return Implants.get(p) == Implant.RINNE && Implants.stage(p) >= 2;
    }

    /** Percepcao visual: visao noturna enquanto o Rinne Sharingan esta ativo. */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side.isClient()) return;
        if (!(e.player instanceof ServerPlayer p) || p.tickCount % 40 != 0) return;
        if (!rinneActive(p)) return;
        p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 300, 0, false, false, false));
    }

    /** Previsao de movimentos: chance de desviar de golpes de outras entidades. */
    @SubscribeEvent
    public static void onAttacked(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (e.getSource().getEntity() == null) return; // so golpes de entidades (nao queda, lava, etc.)
        if (!rinneActive(sp)) return;
        if (sp.getRandom().nextFloat() >= DODGE_CHANCE) return;
        e.setCanceled(true);
        ServerLevel lv = (ServerLevel) sp.level();
        lv.sendParticles(ParticleTypes.CRIT, sp.getX(), sp.getY() + 1, sp.getZ(), 12, 0.3, 0.5, 0.3, 0.2);
        sp.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + ".dodged"), true);
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent e) {
        COCOONED.clear();
        DRAINED.clear();
        DOMINIONS.clear();
    }
}
