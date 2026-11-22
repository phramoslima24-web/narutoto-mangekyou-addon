package com.narutotoaddon.mangekyou;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Projetil dos Susanoos: Yasaka Magatama (Madara, explosivo), Joia Yasaka (Itachi, disco perfurante) e
 * agulhas do Tsukumo (Shisui, com variante de fogo). Todo o dano e processado no servidor; cada projetil
 * explode/atinge no maximo uma vez e tem vida maxima, entao nao existem projeteis infinitos.
 */
public class SusanooProjectile extends Projectile {
    public static final int MAGATAMA = 0, JEWEL = 1, NEEDLE = 2;

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(SusanooProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> KIND =
            SynchedEntityData.defineId(SusanooProjectile.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FIRE =
            SynchedEntityData.defineId(SusanooProjectile.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SIZE =
            SynchedEntityData.defineId(SusanooProjectile.class, EntityDataSerializers.FLOAT);

    private float damage = 10.0F;
    private float radius = 4.0F;
    private int life;
    private int maxLife = 80;
    private int pierceLeft = 1;
    private final Set<UUID> hitSet = new HashSet<>();
    private boolean finished;

    public SusanooProjectile(EntityType<? extends SusanooProjectile> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    /** Cria e dispara (servidor). */
    public static SusanooProjectile fire(ServerLevel lv, LivingEntity owner, SusanooKind kind, int variant, Vec3 from,
                                         Vec3 dir, double speed, float damage, float radius, float size, boolean fire,
                                         int maxLife, int pierce) {
        SusanooProjectile p = new SusanooProjectile(ModEntities.SUSANOO_PROJECTILE.get(), lv);
        p.setOwner(owner);
        p.setPos(from.x, from.y, from.z);
        p.entityData.set(VARIANT, variant);
        p.entityData.set(KIND, kind.ordinal());
        p.entityData.set(FIRE, fire);
        p.entityData.set(SIZE, size);
        p.damage = damage;
        p.radius = radius;
        p.maxLife = maxLife;
        p.pierceLeft = Math.max(1, pierce);
        Vec3 d = dir.normalize();
        p.setDeltaMovement(d.scale(speed));
        ProjectileUtil.rotateTowardsMovement(p, 1.0F);
        lv.addFreshEntity(p);
        return p;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(VARIANT, MAGATAMA);
        this.entityData.define(KIND, 0);
        this.entityData.define(FIRE, false);
        this.entityData.define(SIZE, 3.0F);
    }

    public int variant() { return this.entityData.get(VARIANT); }
    public SusanooKind kind() { return SusanooKind.byOrdinal(this.entityData.get(KIND)); }
    public boolean isFire() { return this.entityData.get(FIRE); }
    public float size() { return this.entityData.get(SIZE); }

    @Override
    protected boolean canHitEntity(Entity e) {
        if (!super.canHitEntity(e)) return false;
        if (e instanceof SusanooProjectile || e instanceof YataMirrorEntity || e instanceof SusanooLance) return false;
        Entity o = this.getOwner();
        if (o instanceof LivingEntity lo && SusanooCombat.isAlly(lo, e)) return false;
        return !hitSet.contains(e.getUUID());
    }

    @Override
    public boolean shouldBeSaved() { return false; }

    @Override
    public void tick() {
        super.tick();
        Vec3 mv = this.getDeltaMovement();
        if (!this.level().isClientSide) {
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                handleHit(hit);
                if (this.isRemoved()) return;
            }
            if (++life > maxLife) {
                expire();
                return;
            }
            if (variant() == NEEDLE && isFire() && life % 2 == 0) {
                ((ServerLevel) level()).sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 1, 0.05, 0.05, 0.05, 0.0);
            }
        } else {
            clientTrail();
        }
        this.setPos(getX() + mv.x, getY() + mv.y, getZ() + mv.z);
        ProjectileUtil.rotateTowardsMovement(this, 1.0F);
    }

    private void clientTrail() {
        SusanooKind k = kind();
        int v = variant();
        float s = v == MAGATAMA ? 2.4F : v == JEWEL ? 1.6F : 0.8F;
        for (int i = 0; i < (v == MAGATAMA ? 3 : 1); i++) {
            this.level().addParticle(i == 0 ? k.dust(s) : k.glowDust(s * 0.6F),
                    getX() + (random.nextDouble() - 0.5) * size() * 0.4, getY() + (random.nextDouble() - 0.5) * size() * 0.4,
                    getZ() + (random.nextDouble() - 0.5) * size() * 0.4, 0, 0, 0);
        }
    }

    private LivingEntity ownerLiving() {
        return this.getOwner() instanceof LivingEntity l ? l : null;
    }

    private void handleHit(HitResult hit) {
        if (finished) return;
        ServerLevel lv = (ServerLevel) this.level();
        SusanooKind k = kind();
        Vec3 pos = hit.getLocation();
        int v = variant();
        if (hit instanceof EntityHitResult ehr) {
            Entity t = ehr.getEntity();
            hitSet.add(t.getUUID());
            if (v == MAGATAMA) {
                finishMagatama(lv, k, pos);
                return;
            }
            if (t instanceof LivingEntity le) {
                le.hurt(SusanooCombat.source(lv, this, ownerLiving()), damage);
                if (isFire()) le.setSecondsOnFire(8);
                le.invulnerableTime = 0; // permite varias agulhas seguidas
                Vec3 push = getDeltaMovement().normalize().scale(v == JEWEL ? 0.6 : 0.25);
                le.setDeltaMovement(le.getDeltaMovement().add(push.x, 0.1, push.z));
                le.hurtMarked = true;
            }
            impactFx(lv, k, pos, v);
            if (--pierceLeft <= 0) endNow(lv, k, pos);
        } else {
            if (v == MAGATAMA) {
                finishMagatama(lv, k, pos);
            } else {
                impactFx(lv, k, pos, v);
                endNow(lv, k, pos);
            }
        }
    }

    private void finishMagatama(ServerLevel lv, SusanooKind k, Vec3 pos) {
        finished = true;
        SusanooCombat.magatamaExplosion(lv, this, ownerLiving(), pos, k, radius, damage, isFire());
        this.discard();
    }

    private void impactFx(ServerLevel lv, SusanooKind k, Vec3 pos, int v) {
        if (v == JEWEL) {
            lv.sendParticles(k.glowDust(2.0F), pos.x, pos.y, pos.z, 30, 0.6, 0.6, 0.6, 0.15);
            lv.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y, pos.z, 10, 0.4, 0.4, 0.4, 0.15);
            SusanooCombat.sound(lv, pos, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.4F);
        } else {
            lv.sendParticles(isFire() ? ParticleTypes.FLAME : k.glowDust(1.0F), pos.x, pos.y, pos.z, 8, 0.15, 0.15, 0.15, 0.05);
            lv.sendParticles(k.dust(1.2F), pos.x, pos.y, pos.z, 6, 0.2, 0.2, 0.2, 0.1);
            SusanooCombat.sound(lv, pos, SoundEvents.TRIDENT_HIT, 0.6F, 1.5F);
        }
    }

    /** Fim do disco/agulha: o disco da Joia ainda faz um estouro dourado em area pequena. */
    private void endNow(ServerLevel lv, SusanooKind k, Vec3 pos) {
        finished = true;
        if (variant() == JEWEL && radius > 0) {
            SusanooCombat.blast(lv, this, ownerLiving(), pos, radius, damage * 0.6F, 0.8F, false);
            SusanooCombat.ring(lv, pos, k.glowDust(2.0F), radius, 24);
        }
        this.discard();
    }

    private void expire() {
        ServerLevel lv = (ServerLevel) this.level();
        if (variant() == MAGATAMA) {
            finishMagatama(lv, kind(), position()); // nao some "do nada": explode no fim da vida
        } else {
            lv.sendParticles(kind().dust(1.0F), getX(), getY(), getZ(), 5, 0.1, 0.1, 0.1, 0.02);
            this.discard();
        }
    }

    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
}
