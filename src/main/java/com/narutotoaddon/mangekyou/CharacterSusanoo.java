package com.narutotoaddon.mangekyou;

import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Susanoo exclusivo de um Mangekyou (Madara azul-escuro, Itachi dourado, Shisui verde). Existem tres tipos de
 * entidade independentes (um por personagem), cada um com modelo e texturas proprias.
 *
 * - Estagios 1-3 (costela, esqueleto, completo): acompanham o dono (ou o clone, no caso do Madara).
 * - Estagio 4 (perfeito): gigante montavel, controlado pelo cavaleiro como o do mod original.
 * - Nao e salvo no disco (shouldBeSaved = false) e some sozinho quando o dono / clone / implante muda,
 *   entao nunca sobram Susanoos abandonados.
 */
public class CharacterSusanoo extends TamableAnimal implements GeoEntity {
    public static final int FADE_MAX = 12;

    private static final EntityDataAccessor<Integer> STAGE =
            SynchedEntityData.defineId(CharacterSusanoo.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<String> ACT =
            SynchedEntityData.defineId(CharacterSusanoo.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> CLONE =
            SynchedEntityData.defineId(CharacterSusanoo.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LANCE =
            SynchedEntityData.defineId(CharacterSusanoo.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> MOVING =
            SynchedEntityData.defineId(CharacterSusanoo.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> FADE =
            SynchedEntityData.defineId(CharacterSusanoo.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Clone que este Susanoo acompanha (so no servidor). */
    private UUID anchorUuid;
    private int actTicks;
    private int zeroGrace;
    private boolean vanishing;
    private double lastX, lastZ;

    public CharacterSusanoo(EntityType<CharacterSusanoo> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.setMaxUpStep(20.0F);
        this.noCulling = true;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.ARMOR, 25.0D)
                .add(Attributes.ATTACK_DAMAGE, 0.0D)
                .add(Attributes.FOLLOW_RANGE, 100.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STAGE, 3);
        this.entityData.define(ACT, "");
        this.entityData.define(CLONE, false);
        this.entityData.define(LANCE, false);
        this.entityData.define(MOVING, false);
        this.entityData.define(FADE, 0);
    }

    // ------------------------------------------------------------------ dados
    public SusanooKind kind() {
        EntityType<?> t = this.getType();
        if (t == ModEntities.ITACHI_SUSANOO.get()) return SusanooKind.ITACHI;
        if (t == ModEntities.SHISUI_SUSANOO.get()) return SusanooKind.SHISUI;
        return SusanooKind.MADARA;
    }

    public int getStage() { return this.entityData.get(STAGE); }
    public boolean isClone() { return this.entityData.get(CLONE); }
    public boolean isLanceActive() { return this.entityData.get(LANCE); }
    public boolean isMovingFlag() { return this.entityData.get(MOVING); }
    public int getFade() { return this.entityData.get(FADE); }
    public String getAct() { return this.entityData.get(ACT); }
    public UUID getAnchorUuid() { return anchorUuid; }
    /** O addon nunca substitui a montaria: o Susanoo original do Sasuke segue existindo (invisivel) e este o acompanha. */
    public boolean isPerfectMount() { return false; }

    /** Prepara a entidade logo apos criar (servidor). */
    public void init(Player owner, int stage, boolean clone, UUID anchor) {
        this.setOwnerUUID(owner.getUUID());
        this.setTame(true);
        this.entityData.set(STAGE, stage);
        this.entityData.set(CLONE, clone);
        this.anchorUuid = anchor;
        boolean follow = !isPerfectMount();
        this.setInvulnerable(follow); // so o perfeito (montaria) pode ser ferido, como o original
        this.setNoGravity(follow);
        this.noPhysics = follow;
        this.refreshDimensions();
    }

    public void setLance(boolean on) {
        this.entityData.set(LANCE, on);
        if (!on && "lance_hold".equals(getAct())) this.entityData.set(ACT, "");
    }

    /** Toca uma animacao de habilidade por N ticks (servidor). */
    public void act(String name, int ticks) {
        this.entityData.set(ACT, name);
        this.actTicks = ticks;
    }

    public Player ownerPlayer() {
        UUID u = getOwnerUUID();
        return u == null ? null : this.level().getPlayerByUUID(u);
    }

    // --------------------------------------------------------- tamanho / sela
    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return isPerfectMount() ? EntityDimensions.scalable(5.0F, 45.0F) : EntityDimensions.scalable(0.4F, 0.4F);
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dims) {
        return isPerfectMount() ? 40.0F : dims.height * 0.9F;
    }

    @Override
    public double getPassengersRidingOffset() {
        return isPerfectMount() ? this.getBbHeight() * 0.75D + 6.7D : super.getPassengersRidingOffset();
    }

    @Override
    protected boolean canAddPassenger(Entity p) {
        return isPerfectMount() && this.getPassengers().isEmpty() && p.getUUID().equals(getOwnerUUID());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STAGE.equals(key) || CLONE.equals(key)) this.refreshDimensions();
    }

    @Override public boolean isPickable() { return isPerfectMount() && super.isPickable(); }
    @Override public boolean isPushable() { return false; }
    @Override protected void doPush(Entity e) {}
    @Override protected void pushEntities() {}
    @Override public boolean isPushedByFluid() { return false; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canBeLeashed(Player p) { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public AgeableMob getBreedOffspring(ServerLevel l, AgeableMob o) { return null; }
    @Override protected void registerGoals() {}

    @Override
    protected SoundEvent getHurtSound(DamageSource ds) {
        return isPerfectMount() ? SoundEvents.IRON_GOLEM_HURT : null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (isPerfectMount() && isOwnedBy(player) && !this.level().isClientSide && getPassengers().isEmpty()) {
            player.startRiding(this);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    // ------------------------------------------------------------------ dano
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (this.isInvulnerable() && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (src.is(DamageTypes.IN_WALL) || src.is(DamageTypes.FALL) || src.is(DamageTypes.DROWN)
                || src.is(DamageTypes.CRAMMING) || src.is(DamageTypes.FLY_INTO_WALL) || src.is(DamageTypes.STARVE)) {
            return false;
        }
        Entity att = src.getEntity();
        if (att != null && att.getUUID().equals(getOwnerUUID())) return false; // fogo amigo
        return super.hurt(src, amount);
    }

    @Override
    public void die(DamageSource src) {
        if (!this.level().isClientSide && isPerfectMount()) {
            Player o = ownerPlayer();
            if (o != null && SusanooKind.of(o) == kind()) {
                Bridge.setDouble(o, "if_susanoo", 0.0D);
                Bridge.sync(o);
            }
        }
        super.die(src);
    }

    // ----------------------------------------------------------- movimento
    @Override
    public void travel(Vec3 dir) {
        Entity rider = this.getFirstPassenger();
        if (isPerfectMount() && rider instanceof LivingEntity passenger) {
            this.setYRot(passenger.getYRot());
            this.yRotO = this.getYRot();
            this.setXRot(passenger.getXRot() * 0.5F);
            this.setRot(this.getYRot(), this.getXRot());
            this.yBodyRot = passenger.getYRot();
            this.yHeadRot = passenger.getYRot();
            this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
            super.travel(new Vec3(passenger.xxa, 0.0D, passenger.zza));
        } else if (!this.noPhysics) {
            super.travel(Vec3.ZERO);
        }
    }

    // ------------------------------------------------------------------ tick
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) serverTick();
    }

    private void follow(Entity a) {
        double nx = a.getX(), ny = a.getY(), nz = a.getZ();
        boolean mv = Math.abs(nx - lastX) + Math.abs(nz - lastZ) > 0.015D;
        lastX = nx;
        lastZ = nz;
        this.setPos(nx, ny, nz);
        float yaw = a instanceof LivingEntity l ? l.yBodyRot : a.getYRot();
        this.setYRot(yaw);
        this.yRotO = yaw;
        this.yBodyRot = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRot = yaw;
        this.yHeadRotO = yaw;
        this.setXRot(0.0F);
        this.setDeltaMovement(Vec3.ZERO);
        if (this.entityData.get(MOVING) != mv) this.entityData.set(MOVING, mv);
    }

    /** Some com a animacao de desaparecimento (ou na hora, se o dono nao esta mais no mundo). */
    public void vanish() {
        if (vanishing || this.level().isClientSide) return;
        vanishing = true;
        this.ejectPassengers();
        ServerLevel sl = (ServerLevel) this.level();
        SusanooKind k = kind();
        float h = SusanooGeometry.chestHeight(getStage(), isClone()) * 1.6F;
        sl.sendParticles(k.dust(2.0F), getX(), getY() + h * 0.4D, getZ(), 40, 2.0D, h * 0.3D, 2.0D, 0.05D);
        sl.sendParticles(ParticleTypes.CLOUD, getX(), getY() + h * 0.4D, getZ(), 12, 1.5D, h * 0.25D, 1.5D, 0.03D);
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 2.0F, 0.7F);
        if (getAct().equals("") || !getAct().equals("vanish")) this.entityData.set(ACT, "vanish");
    }

    private void serverTick() {
        ServerLevel sl = (ServerLevel) this.level();
        if (vanishing) {
            int f = this.entityData.get(FADE) + 1;
            this.entityData.set(FADE, f);
            this.setDeltaMovement(Vec3.ZERO);
            if (f >= FADE_MAX) this.discard();
            return;
        }
        SusanooKind k = kind();
        Player owner = ownerPlayer();
        if (owner == null || !owner.isAlive()) {
            this.discard(); // dono offline / em outra dimensao: nao deixa entidade abandonada
            return;
        }
        if (anchorUuid != null) { // clone de sombra OU Susanoo original do Sasuke (que mantem todos os poderes)
            Entity a = sl.getEntity(anchorUuid);
            boolean dead = a == null || a.isRemoved() || !a.isAlive()
                    || (a instanceof LivingEntity le && le.isDeadOrDying());
            if (dead || SusanooKind.of(owner) != k) {
                vanish();
                return;
            }
            follow(a);
        } else {
            if (SusanooKind.of(owner) != k) { // troca de Mangekyou: some e o Manager invoca o certo
                vanish();
                return;
            }
            double st = Bridge.getDouble(owner, "if_susanoo", -1.0D);
            if (st == 0.0D && this.tickCount > 100) {
                if (++zeroGrace > 100) {
                    vanish();
                    return;
                }
            } else {
                zeroGrace = 0;
            }
            follow(owner);
        }
        // animacao de habilidade
        if (actTicks > 0 && --actTicks == 0) this.entityData.set(ACT, "");
        if (actTicks <= 0 && isLanceActive() && getAct().isEmpty()) this.entityData.set(ACT, "lance_hold");
        // particulas de chakra ao redor do corpo
        if (this.tickCount % 4 == 0) {
            int st2 = getStage();
            float w = switch (st2) { case 1 -> 0.8F; case 2 -> 3.0F; case 3 -> 4.0F; default -> 7.0F; };
            float h = SusanooGeometry.chestHeight(st2, isClone()) * 1.5F;
            sl.sendParticles(k.dust(1.5F), getX(), getY() + h * 0.5D, getZ(), 2 + st2 * 2, w, h * 0.45D, w, 0.01D);
            if (this.tickCount % 12 == 0) {
                sl.sendParticles(k.glowDust(1.0F), getX(), getY() + h * 0.5D, getZ(), 3, w, h * 0.4D, w, 0.01D);
            }
        }
    }

    // --------------------------------------------------------------- GeckoLib
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CharacterSusanoo>(this, "main", 4, this::predicate));
    }

    private PlayState predicate(AnimationState<CharacterSusanoo> state) {
        String act = getAct();
        if (!act.isEmpty() && !act.equals("vanish")) {
            RawAnimation ra = act.equals("lance_hold") || act.equals("mirror") || act.equals("chest")
                    ? RawAnimation.begin().thenLoop("ab." + act)
                    : RawAnimation.begin().thenPlay("ab." + act);
            return state.setAndContinue(ra);
        }
        return state.setAndContinue(RawAnimation.begin().thenLoop(isMovingFlag() ? "walk.animation" : "stand.animation"));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
