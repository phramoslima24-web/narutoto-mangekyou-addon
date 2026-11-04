package com.narutotoaddon.mangekyou;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Corpo deixado pelo Izanagi: fica deitado no chao ate o tempo acabar (ou o jogador morrer e voltar para ele). */
public class IzanagiBody extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER =
            SynchedEntityData.defineId(IzanagiBody.class, EntityDataSerializers.OPTIONAL_UUID);

    private long end;

    public IzanagiBody(EntityType<? extends IzanagiBody> type, Level level) {
        super(type, level);
    }

    public void setOwner(UUID id) {
        this.entityData.set(OWNER, Optional.ofNullable(id));
    }

    public Optional<UUID> getOwner() {
        return this.entityData.get(OWNER);
    }

    public void setEnd(long end) {
        this.end = end;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(OWNER, Optional.empty());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        if (t.hasUUID("Owner")) setOwner(t.getUUID("Owner"));
        this.end = t.getLong("End");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        getOwner().ifPresent(id -> t.putUUID("Owner", id));
        t.putLong("End", this.end);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.onGround()) {
            setDeltaMovement(0.0D, getDeltaMovement().y - 0.04D, 0.0D);
            move(MoverType.SELF, getDeltaMovement());
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        if (level().isClientSide()) return;
        if (level().getGameTime() >= end) {
            discard();
            return;
        }
        if (tickCount % 20 == 0 && getOwner().isPresent() && level().getServer() != null) {
            ServerPlayer owner = level().getServer().getPlayerList().getPlayer(getOwner().get());
            // o dono ja usou o corpo (ou o Izanagi foi desfeito): some
            if (owner != null && !IzanagiHandler.ownsBody(owner, getUUID())) discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            discard();
            return true;
        }
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
