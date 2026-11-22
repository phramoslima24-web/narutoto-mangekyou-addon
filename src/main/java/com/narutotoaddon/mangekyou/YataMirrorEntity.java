package com.narutotoaddon.mangekyou;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Espelho de Yata: escudo dourado na frente do Susanoo do Itachi. Projeteis que se aproximam sao refletidos
 * (voltam com o dono trocado); entidades de chakra do Narutoto sao absorvidas; golpes corpo a corpo e dano
 * espiritual/elemental sao bloqueados por {@link YataHandler}. O escudo tem uma reserva de absorcao e quebra.
 */
public class YataMirrorEntity extends Entity {
    public static final float MAX_BUDGET = 700.0F;
    public static final int DURATION = 200; // 10 s

    /** Espelho ativo por dono (servidor). */
    public static final Map<UUID, YataMirrorEntity> ACTIVE = new ConcurrentHashMap<>();

    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(YataMirrorEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> STAGE =
            SynchedEntityData.defineId(YataMirrorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HIT =
            SynchedEntityData.defineId(YataMirrorEntity.class, EntityDataSerializers.INT);

    private float budget = MAX_BUDGET;
    private int left = DURATION;

    public YataMirrorEntity(EntityType<? extends YataMirrorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public static YataMirrorEntity create(ServerLevel lv, Player owner, int stage) {
        YataMirrorEntity m = new YataMirrorEntity(ModEntities.YATA_MIRROR.get(), lv);
        m.entityData.set(OWNER, Optional.of(owner.getUUID()));
        m.entityData.set(STAGE, stage);
        m.setPos(owner.getX(), owner.getY(), owner.getZ());
        lv.addFreshEntity(m);
        YataMirrorEntity old = ACTIVE.put(owner.getUUID(), m);
        if (old != null && old != m) old.discard();
        return m;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(STAGE, 3);
        this.entityData.define(HIT, 0);
    }

    public UUID ownerId() { return this.entityData.get(OWNER).orElse(null); }
    public int stage() { return this.entityData.get(STAGE); }
    /** Ticks desde o ultimo impacto (para o brilho no cliente). */
    public int hitAge() { return this.entityData.get(HIT); }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isPushedByFluid() { return false; }

    /** Esta o ponto na frente do escudo (so ataques vindos da frente sao bloqueados). */
    public boolean inFront(Vec3 p) {
        double yaw = Math.toRadians(getYRot());
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        Vec3 to = new Vec3(p.x - getX(), 0, p.z - getZ());
        if (to.lengthSqr() < 1.0E-4) return true;
        return fwd.dot(to.normalize()) > -0.25D;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        ServerLevel lv = (ServerLevel) this.level();
        UUID oid = ownerId();
        Player owner = oid == null ? null : lv.getPlayerByUUID(oid);
        CharacterSusanoo s = owner == null ? null : SusanooManager.find(owner, SusanooKind.ITACHI);
        if (owner == null || s == null || --left <= 0 || budget <= 0) {
            end(budget <= 0);
            return;
        }
        int st = s.getStage();
        float yaw = s.getYRot();
        double r = Math.toRadians(yaw);
        float reach = SusanooGeometry.reach(st, false);
        this.setPos(s.getX() - Math.sin(r) * reach, s.getY() + SusanooGeometry.chestHeight(st, false), s.getZ() + Math.cos(r) * reach);
        this.setYRot(yaw);
        this.yRotO = yaw;
        if (this.entityData.get(STAGE) != st) this.entityData.set(STAGE, st);
        int h = this.entityData.get(HIT);
        if (h < 100) this.entityData.set(HIT, h + 1);

        double rad = SusanooGeometry.mirrorRadius(st);
        AABB zone = new AABB(getX() - rad, getY() - rad, getZ() - rad, getX() + rad, getY() + rad, getZ() + rad);
        for (Entity e : lv.getEntities(this, zone, x -> x != owner && !(x instanceof YataMirrorEntity))) {
            Vec3 to = position().subtract(e.position());
            if (e instanceof Projectile pr) {
                if (pr.getOwner() == owner) continue;
                if (pr instanceof SusanooProjectile sp && sp.getOwner() == owner) continue;
                if (lv.getGameTime() - pr.getPersistentData().getLong("mkaddon_yata") < 40) continue;
                Vec3 vel = pr.getDeltaMovement();
                if (vel.lengthSqr() < 1.0E-4 || vel.dot(to) <= 0 || !inFront(pr.position())) continue;
                reflect(lv, pr, owner);
            } else if (SusanooCombat.isChakraEntity(e) && !(e instanceof LivingEntity le && SusanooCombat.isAlly(owner, le))
                    && inFront(e.position())) {
                // ataque de chakra de outra tecnica: absorvido
                budget -= 25;
                hitFx(lv, e.position());
                e.discard();
            }
        }
    }

    private void reflect(ServerLevel lv, Projectile pr, Player owner) {
        pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.0D));
        pr.hurtMarked = true;
        pr.setOwner(owner);
        pr.getPersistentData().putLong("mkaddon_yata", lv.getGameTime());
        ProjectileUtil.rotateTowardsMovement(pr, 1.0F);
        budget -= 6;
        hitFx(lv, pr.position());
    }

    /** Chamado pelo YataHandler ao bloquear dano; devolve false quando o escudo ja quebrou. */
    public void absorb(float amount, Vec3 at) {
        budget -= Math.max(1.0F, amount);
        hitFx((ServerLevel) this.level(), at);
    }

    private void hitFx(ServerLevel lv, Vec3 at) {
        this.entityData.set(HIT, 0);
        SusanooKind k = SusanooKind.ITACHI;
        lv.sendParticles(k.glowDust(1.8F), at.x, at.y, at.z, 22, 0.5, 0.5, 0.5, 0.12);
        lv.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 6, 0.3, 0.3, 0.3, 0.12);
        SusanooCombat.sound(lv, at, SoundEvents.ANVIL_PLACE, 0.8F, 1.8F);
        SusanooCombat.sound(lv, at, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F, 1.2F);
    }

    private void end(boolean broken) {
        UUID oid = ownerId();
        if (oid != null) ACTIVE.remove(oid, this);
        if (!this.level().isClientSide) {
            ServerLevel lv = (ServerLevel) this.level();
            lv.sendParticles(SusanooKind.ITACHI.glowDust(2.0F), getX(), getY(), getZ(), broken ? 80 : 30, 3, 3, 3, 0.1);
            SusanooCombat.sound(lv, position(), broken ? SoundEvents.GLASS_BREAK : SoundEvents.BEACON_DEACTIVATE, 2.0F, broken ? 0.8F : 1.0F);
            if (broken && oid != null) {
                Player o = lv.getPlayerByUUID(oid);
                if (o != null) o.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                        "msg." + MangekyouAddon.MODID + ".mirror_broken"), true);
            }
        }
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        UUID oid = ownerId();
        if (oid != null) ACTIVE.remove(oid, this);
        super.remove(reason);
    }

    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
}
