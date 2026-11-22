package com.narutotoaddon.mangekyou;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Habilidades exclusivas dos Susanoos (servidor). Cooldown e chakra ficam no AbilityHandler. */
public final class SusanooAbilities {
    private SusanooAbilities() {}

    private static void msg(ServerPlayer p, String key, Object... a) {
        p.displayClientMessage(Component.translatable("msg." + MangekyouAddon.MODID + "." + key, a), true);
    }

    /** Susanoo do personagem certo, ativo e do proprio jogador; senao avisa. */
    private static CharacterSusanoo require(ServerPlayer p, SusanooKind k) {
        if (SusanooKind.of(p) != k) {
            msg(p, "wrong_implant");
            return null;
        }
        CharacterSusanoo s = SusanooManager.find(p, k);
        if (s == null) msg(p, "need_susanoo");
        return s;
    }

    private static ServerPlayer online(ServerLevel lv, UUID id) {
        return lv.getServer().getPlayerList().getPlayer(id);
    }

    // ------------------------------------------------------------- utilidades
    static Vec3 forward(CharacterSusanoo s) {
        double y = Math.toRadians(s.getYRot());
        return new Vec3(-Math.sin(y), 0, Math.cos(y));
    }

    static Vec3 muzzle(CharacterSusanoo s) {
        int st = s.getStage();
        return s.position().add(forward(s).scale(SusanooGeometry.reach(st, s.isClone())))
                .add(0, SusanooGeometry.chestHeight(st, s.isClone()), 0);
    }

    /** Ponto que o jogador esta mirando (bloco ou criatura ate 200 blocos). */
    static Vec3 aimPoint(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        Vec3 end = eye.add(look.scale(200.0D));
        HitResult hr = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 target = hr.getType() == HitResult.Type.MISS ? end : hr.getLocation();
        LivingEntity le = Targeting.livingInSight(p, Math.min(200.0D, eye.distanceTo(target)));
        return le != null ? le.position().add(0, le.getBbHeight() * 0.5D, 0) : target;
    }

    static Vec3 rotY(Vec3 v, double deg) {
        double r = Math.toRadians(deg), c = Math.cos(r), s = Math.sin(r);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    private static void chargeFx(ServerLevel lv, CharacterSusanoo s, SusanooKind k, int ticks) {
        for (int i = 0; i < ticks; i += 2) {
            final int t = i;
            SusanooTasks.later(i, () -> {
                if (s.isRemoved()) return;
                Vec3 m = muzzle(s);
                float r = 4.0F - 3.0F * t / Math.max(1, ticks);
                lv.sendParticles(k.glowDust(1.6F), m.x, m.y, m.z, 14, r, r, r, 0.0D);
                lv.sendParticles(k.dust(2.0F), m.x, m.y, m.z, 6, r * 0.5, r * 0.5, r * 0.5, 0.02D);
                if (t % 6 == 0) SusanooCombat.sound(lv, m, SoundEvents.BEACON_AMBIENT, 1.4F, 0.6F + t * 0.03F);
            });
        }
    }

    // --------------------------------------------------- Madara: Yasaka Magatama
    public static boolean yasakaMagatama(ServerPlayer p) {
        CharacterSusanoo s = require(p, SusanooKind.MADARA);
        if (s == null) return false;
        ServerLevel lv = p.serverLevel();
        s.act("charge", 20);
        chargeFx(lv, s, SusanooKind.MADARA, 18);
        UUID id = p.getUUID();
        SusanooTasks.later(20, () -> {
            ServerPlayer pl = online(lv, id);
            if (pl != null && pl.isAlive()) fireMagatamas(pl);
        });
        return true;
    }

    private static void fireMagatamas(ServerPlayer p) {
        ServerLevel lv = p.serverLevel();
        CharacterSusanoo s = SusanooManager.find(p, SusanooKind.MADARA);
        if (s == null) return;
        s.act("cast", 16);
        Vec3 aim = aimPoint(p);
        double[] fan = {-9.0, 0.0, 9.0};
        Vec3 from = muzzle(s);
        for (double a : fan) {
            Vec3 dir = rotY(aim.subtract(from).normalize(), a);
            SusanooProjectile.fire(lv, p, SusanooKind.MADARA, SusanooProjectile.MAGATAMA, from, dir, 2.4D,
                    45.0F, 9.0F, 6.0F, false, 120, 1);
        }
        SusanooCombat.modSound(lv, from, "susanoo_attack_perfect", SoundEvents.WITHER_SHOOT, 3.0F, 0.7F);
        SusanooCombat.sound(lv, from, SoundEvents.GENERIC_EXPLODE, 1.5F, 1.6F);
        lv.sendParticles(SusanooKind.MADARA.glowDust(2.5F), from.x, from.y, from.z, 40, 1.5, 1.5, 1.5, 0.2);
        // os Susanoos dos clones tambem disparam (um Magatama menor cada, do proprio corpo)
        for (CharacterSusanoo c : SusanooManager.mine(p, true)) {
            Vec3 cf = muzzle(c);
            c.act("cast", 16);
            SusanooProjectile.fire(lv, p, SusanooKind.MADARA, SusanooProjectile.MAGATAMA, cf,
                    aim.subtract(cf).normalize(), 2.2D, 25.0F, 6.0F, 4.0F, false, 120, 1);
        }
    }

    // ------------------------------------- Madara: Susanoos nos clones (liga/desliga)
    public static boolean cloneSusanoos(ServerPlayer p) {
        CharacterSusanoo s = require(p, SusanooKind.MADARA);
        if (s == null) return false;
        if (p.getPersistentData().getBoolean(SusanooManager.CLONE_FLAG)) {
            SusanooManager.clearClones(p);
            msg(p, "clone_susanoo_off");
            return true;
        }
        if (SusanooManager.mine(p, true).isEmpty()) {
            boolean any = !p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, p.getBoundingBox().inflate(128.0D),
                    c -> c.isAlive() && c instanceof net.minecraft.world.entity.TamableAnimal t && t.isOwnedBy(p)
                            && net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(c.getType()) != null
                            && net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(c.getType()).getNamespace().equals("narutoto")
                            && (net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(c.getType()).getPath().equals("clone_new")
                            || net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(c.getType()).getPath().equals("sage_clone"))).isEmpty();
            if (!any) {
                msg(p, "no_clones");
                return false;
            }
        }
        p.getPersistentData().putBoolean(SusanooManager.CLONE_FLAG, true);
        SusanooManager.syncClones(p);
        SusanooCombat.sound(p.serverLevel(), p.position(), SoundEvents.BEACON_ACTIVATE, 2.0F, 0.6F);
        msg(p, "clone_susanoo_on");
        return true;
    }

    // ------------------------------------------------- Itachi: Espelho de Yata
    public static boolean yataMirror(ServerPlayer p) {
        CharacterSusanoo s = require(p, SusanooKind.ITACHI);
        if (s == null) return false;
        ServerLevel lv = p.serverLevel();
        YataMirrorEntity.create(lv, p, s.getStage());
        s.act("mirror", YataMirrorEntity.DURATION);
        Vec3 m = muzzle(s);
        lv.sendParticles(SusanooKind.ITACHI.glowDust(2.5F), m.x, m.y, m.z, 80, 3, 3, 3, 0.15);
        lv.sendParticles(ParticleTypes.END_ROD, m.x, m.y, m.z, 25, 2, 2, 2, 0.2);
        SusanooCombat.sound(lv, m, SoundEvents.BEACON_POWER_SELECT, 2.5F, 1.4F);
        SusanooCombat.sound(lv, m, SoundEvents.AMETHYST_CLUSTER_PLACE, 2.0F, 0.8F);
        return true;
    }

    // ---------------------------------------------- Itachi: Joia Yasaka Magatama
    public static boolean yasakaJewel(ServerPlayer p) {
        CharacterSusanoo s = require(p, SusanooKind.ITACHI);
        if (s == null) return false;
        ServerLevel lv = p.serverLevel();
        s.act("charge", 12);
        chargeFx(lv, s, SusanooKind.ITACHI, 10);
        UUID id = p.getUUID();
        SusanooTasks.later(12, () -> {
            ServerPlayer pl = online(lv, id);
            CharacterSusanoo cs = pl == null ? null : SusanooManager.find(pl, SusanooKind.ITACHI);
            if (pl == null || cs == null) return;
            cs.act("cast", 14);
            Vec3 from = muzzle(cs);
            Vec3 aim = aimPoint(pl);
            for (double a : new double[] {-11.0, 0.0, 11.0}) {
                Vec3 dir = rotY(aim.subtract(from).normalize(), a);
                SusanooProjectile.fire(lv, pl, SusanooKind.ITACHI, SusanooProjectile.JEWEL, from, dir, 2.2D,
                        26.0F, 3.5F, 3.5F, false, 80, 5);
            }
            SusanooCombat.sound(lv, from, SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0F, 0.8F);
            SusanooCombat.sound(lv, from, SoundEvents.TRIDENT_RIPTIDE_3, 1.5F, 1.3F);
            lv.sendParticles(SusanooKind.ITACHI.glowDust(2.0F), from.x, from.y, from.z, 40, 1.2, 1.2, 1.2, 0.2);
        });
        return true;
    }

    // ----------------------------------- Shisui: Tsukumo (barragem de agulhas)
    public static boolean tsukumo(ServerPlayer p) {
        CharacterSusanoo s = require(p, SusanooKind.SHISUI);
        if (s == null) return false;
        ServerLevel lv = p.serverLevel();
        s.act("chest", 60);
        Vec3 m0 = muzzle(s);
        SusanooCombat.sound(lv, m0, SoundEvents.IRON_DOOR_OPEN, 3.0F, 0.5F);
        UUID id = p.getUUID();
        final int open = 10, shots = 28;
        // 1) o peito abre (animacao + brilho) | 2) rajada de 28 ticks, 3 agulhas por tick
        for (int t = 0; t < open; t += 2) {
            SusanooTasks.later(t, () -> {
                ServerPlayer pl = online(lv, id);
                CharacterSusanoo cs = pl == null ? null : SusanooManager.find(pl, SusanooKind.SHISUI);
                if (cs == null) return;
                Vec3 m = muzzle(cs);
                lv.sendParticles(SusanooKind.SHISUI.glowDust(2.0F), m.x, m.y, m.z, 16, 1.6, 1.6, 1.0, 0.05);
            });
        }
        for (int t = 0; t < shots; t++) {
            final int tt = t;
            SusanooTasks.later(open + t, () -> {
                ServerPlayer pl = online(lv, id);
                CharacterSusanoo cs = pl == null || !pl.isAlive() ? null : SusanooManager.find(pl, SusanooKind.SHISUI);
                if (cs == null) return;
                boolean fire = SusanooManager.fireComboActive(pl); // Liberacao de Fogo + Tsukumo
                Vec3 base = muzzle(cs).subtract(forward(cs).scale(SusanooGeometry.reach(cs.getStage(), false) * 0.4D));
                Vec3 aim = aimPoint(pl);
                double w = Math.max(0.6D, SusanooGeometry.chestHeight(cs.getStage(), false) * 0.18D);
                for (int i = 0; i < 3; i++) {
                    Vec3 from = base.add((lv.random.nextDouble() - 0.5) * 2 * w, (lv.random.nextDouble() - 0.5) * 2 * w,
                            (lv.random.nextDouble() - 0.5) * 2 * w);
                    Vec3 dir = aim.subtract(from).normalize()
                            .add((lv.random.nextDouble() - 0.5) * 0.09, (lv.random.nextDouble() - 0.5) * 0.09,
                                    (lv.random.nextDouble() - 0.5) * 0.09).normalize();
                    SusanooProjectile.fire(lv, pl, SusanooKind.SHISUI, SusanooProjectile.NEEDLE, from, dir, 3.4D,
                            fire ? 11.0F : 7.0F, 0.0F, 1.4F, fire, 50, 1);
                }
                if (tt % 2 == 0) {
                    SusanooCombat.sound(lv, base, SoundEvents.ARROW_SHOOT, 1.6F, 1.7F);
                    SusanooCombat.sound(lv, base, fire ? SoundEvents.BLAZE_SHOOT : SoundEvents.CROSSBOW_SHOOT, 1.2F, 1.2F);
                }
                lv.sendParticles(fire ? ParticleTypes.FLAME : SusanooKind.SHISUI.glowDust(1.4F), base.x, base.y, base.z,
                        6, w, w, w, 0.04);
            });
        }
        return true;
    }

    // --------------------------------- Shisui: broca/lanca (liga/desliga + golpes)
    public static boolean drillLance(ServerPlayer p) {
        CharacterSusanoo s = require(p, SusanooKind.SHISUI);
        if (s == null) return false;
        ServerLevel lv = p.serverLevel();
        if (s.isLanceActive()) {
            s.setLance(false);
            msg(p, "lance_off");
            return true;
        }
        s.setLance(true);
        s.act("lance", 16);
        SusanooLance.create(lv, p, s.getStage());
        Vec3 m = muzzle(s);
        lv.sendParticles(SusanooKind.SHISUI.glowDust(2.2F), m.x, m.y, m.z, 60, 2.5, 2.5, 2.5, 0.12);
        SusanooCombat.sound(lv, m, SoundEvents.TRIDENT_RETURN, 2.5F, 0.6F);
        SusanooCombat.sound(lv, m, SoundEvents.BEACON_ACTIVATE, 1.5F, 1.4F);
        msg(p, "lance_on");
        UUID id = p.getUUID();
        SusanooTasks.later(20 * 45, () -> { // a arma se desfaz sozinha (nao fica abandonada)
            ServerPlayer pl = online(lv, id);
            CharacterSusanoo cs = pl == null ? null : SusanooManager.find(pl, SusanooKind.SHISUI);
            if (cs != null && cs == s) cs.setLance(false);
        });
        return true;
    }

    /** Clique de ataque do cliente. */
    public static void attackAction(ServerPlayer p) {
        CharacterSusanoo s = SusanooManager.find(p, SusanooKind.SHISUI);
        if (s != null && s.isLanceActive()) lanceStab(p, s);
    }

    private static boolean cd(ServerPlayer p, String key, int ticks) {
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong(key)) return true;
        p.getPersistentData().putLong(key, now + ticks);
        return false;
    }

    private static double distToSegment(Vec3 pt, Vec3 a, Vec3 b) {
        Vec3 ab = b.subtract(a);
        double t = Math.max(0, Math.min(1, pt.subtract(a).dot(ab) / Math.max(1.0E-6, ab.lengthSqr())));
        return pt.distanceTo(a.add(ab.scale(t)));
    }

    /** Estocada da broca: atravessa todos os inimigos na linha, dano alto e impacto forte. */
    private static void lanceStab(ServerPlayer p, CharacterSusanoo s) {
        if (cd(p, "mkaddon_lance_cd", 12)) return;
        ServerLevel lv = p.serverLevel();
        s.act("lance", 14);
        UUID id = p.getUUID();
        SusanooLance lance = lv.getEntitiesOfClass(SusanooLance.class, s.getBoundingBox().inflate(100),
                l -> id.equals(l.ownerId())).stream().findFirst().orElse(null);
        if (lance == null) return;
        lance.thrust();
        Vec3 dir = p.getLookAngle();
        double len = lance.lengthBlocks() + 8.0D;
        Vec3 a = lance.position(), b = a.add(dir.scale(len));
        double width = 1.8D + 1.5D * SusanooGeometry.lanceScale(s.getStage());
        AABB box = new AABB(a, b).inflate(width + 1.0D);
        int hit = 0;
        for (LivingEntity le : lv.getEntitiesOfClass(LivingEntity.class, box, e -> !SusanooCombat.isAlly(p, e) && e.isAlive())) {
            Vec3 c = le.position().add(0, le.getBbHeight() * 0.5D, 0);
            if (distToSegment(c, a, b) > width + le.getBbWidth() * 0.5D) continue;
            if (le.hurt(SusanooCombat.source(lv, s, p), 32.0F)) hit++;
            le.invulnerableTime = 0;
            le.setDeltaMovement(le.getDeltaMovement().add(dir.x * 1.6, 0.5, dir.z * 1.6));
            le.hurtMarked = true;
            lv.sendParticles(SusanooKind.SHISUI.glowDust(2.0F), c.x, c.y, c.z, 24, 0.5, 0.6, 0.5, 0.2);
            lv.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 16, 0.5, 0.6, 0.5, 0.4);
        }
        for (double d = 0; d < len; d += 1.5D) {
            Vec3 q = a.add(dir.scale(d));
            lv.sendParticles(SusanooKind.SHISUI.dust(1.6F), q.x, q.y, q.z, 3, 0.25, 0.25, 0.25, 0.02);
        }
        SusanooCombat.modSound(lv, a, "susanoo_attack_perfect", SoundEvents.TRIDENT_THROW, 2.5F, 1.0F);
        SusanooCombat.sound(lv, a, SoundEvents.TRIDENT_HIT, hit > 0 ? 2.0F : 0.8F, 0.8F);
    }

    /** Golpe corpo a corpo do Susanoo montado (arco a frente). */
    public static void slash(ServerPlayer p, CharacterSusanoo s) {
        if (cd(p, "mkaddon_slash_cd", 16)) return;
        ServerLevel lv = p.serverLevel();
        SusanooKind k = s.kind();
        s.act("slash", 16);
        Vec3 f = forward(s);
        float reach = SusanooGeometry.reach(s.getStage(), false) * 1.6F;
        Vec3 c = s.position().add(f.scale(reach * 0.8D)).add(0, SusanooGeometry.chestHeight(s.getStage(), false) * 0.5D, 0);
        AABB box = new AABB(c, c).inflate(reach, reach * 0.9D, reach);
        for (LivingEntity le : lv.getEntitiesOfClass(LivingEntity.class, box, e -> !SusanooCombat.isAlly(p, e) && e.isAlive())) {
            Vec3 to = le.position().subtract(s.position()).multiply(1, 0, 1);
            if (to.lengthSqr() > 1.0E-4 && f.dot(to.normalize()) < 0.2D) continue;
            le.hurt(SusanooCombat.source(lv, s, p), 28.0F);
            le.invulnerableTime = 0;
            le.setDeltaMovement(le.getDeltaMovement().add(f.x * 1.3, 0.45, f.z * 1.3));
            le.hurtMarked = true;
            lv.sendParticles(k.glowDust(1.8F), le.getX(), le.getY() + le.getBbHeight() * 0.5, le.getZ(), 20, 0.5, 0.5, 0.5, 0.15);
        }
        SusanooCombat.ring(lv, c.subtract(0, c.y - s.getY(), 0), k.dust(2.0F), reach * 0.7F, 26);
        SusanooCombat.modSound(lv, c, "susanoo_attack_perfect", SoundEvents.PLAYER_ATTACK_SWEEP, 2.0F, 1.0F);
    }
}
