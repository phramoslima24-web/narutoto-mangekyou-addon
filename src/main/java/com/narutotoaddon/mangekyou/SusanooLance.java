package com.narutotoaddon.mangekyou;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Broca/lanca verde de chakra empunhada pelo Susanoo do Shisui (entidade propria, gira o tempo todo). */
public class SusanooLance extends Entity implements GeoEntity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(SusanooLance.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Integer> STAGE =
            SynchedEntityData.defineId(SusanooLance.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> THRUST =
            SynchedEntityData.defineId(SusanooLance.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int thrustTicks;

    public SusanooLance(EntityType<? extends SusanooLance> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    public static SusanooLance create(ServerLevel lv, Player owner, int stage) {
        SusanooLance l = new SusanooLance(ModEntities.SUSANOO_LANCE.get(), lv);
        l.entityData.set(OWNER, Optional.of(owner.getUUID()));
        l.entityData.set(STAGE, stage);
        l.setPos(owner.getX(), owner.getY(), owner.getZ());
        lv.addFreshEntity(l);
        return l;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER, Optional.empty());
        this.entityData.define(STAGE, 3);
        this.entityData.define(THRUST, false);
    }

    public UUID ownerId() { return this.entityData.get(OWNER).orElse(null); }
    public int stage() { return this.entityData.get(STAGE); }
    public boolean thrusting() { return this.entityData.get(THRUST); }

    public void thrust() {
        this.thrustTicks = 8;
        this.entityData.set(THRUST, true);
    }

    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean isPickable() { return false; }

    /** Posicao da ponta da broca (para o golpe). */
    public double lengthBlocks() { return 24.0D * SusanooGeometry.lanceScale(stage()); }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        ServerLevel lv = (ServerLevel) this.level();
        UUID oid = ownerId();
        Player owner = oid == null ? null : lv.getPlayerByUUID(oid);
        CharacterSusanoo s = owner == null ? null : SusanooManager.find(owner, SusanooKind.SHISUI);
        if (owner == null || s == null || !s.isLanceActive()) {
            this.discard();
            return;
        }
        int st = s.getStage();
        if (this.entityData.get(STAGE) != st) this.entityData.set(STAGE, st);
        double yaw = Math.toRadians(s.getYRot());
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        float sc = SusanooGeometry.lanceScale(st);
        double side = 3.5D * (st == 4 ? 1.0D : SusanooGeometry.modelScale(st) * 0.6D);
        double fwd = SusanooGeometry.reach(st, false) * 0.35D;
        this.setPos(s.getX() + rx * side + fx * fwd, s.getY() + SusanooGeometry.chestHeight(st, false) * 0.85D,
                s.getZ() + rz * side + fz * fwd);
        float pitch = Math.max(-60.0F, Math.min(60.0F, owner.getXRot()));
        this.setYRot(owner.getYRot());
        this.yRotO = owner.getYRot();
        this.setXRot(pitch);
        this.xRotO = pitch;
        if (thrustTicks > 0 && --thrustTicks == 0) this.entityData.set(THRUST, false);
        if (this.tickCount % 3 == 0) {
            double tipD = lengthBlocks() * 0.8D;
            Entity v = this;
            double pr = Math.toRadians(pitch);
            lv.sendParticles(SusanooKind.SHISUI.glowDust(1.2F),
                    getX() + fx * Math.cos(pr) * tipD, getY() - Math.sin(pr) * tipD, getZ() + fz * Math.cos(pr) * tipD,
                    2, 0.3 * sc, 0.3 * sc, 0.3 * sc, 0.02);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<SusanooLance>(this, "main", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<SusanooLance> state) {
        return state.setAndContinue(RawAnimation.begin().thenLoop(thrusting() ? "thrust" : "spin"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    @Override protected void readAdditionalSaveData(CompoundTag t) {}
    @Override protected void addAdditionalSaveData(CompoundTag t) {}
}
