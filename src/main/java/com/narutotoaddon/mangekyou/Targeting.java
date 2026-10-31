package com.narutotoaddon.mangekyou;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

final class Targeting {
    private Targeting() {}

    /** Entidade viva na mira do jogador (ignora alvos atras de blocos). */
    static LivingEntity livingInSight(Player p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p.level(), p, eye, end, box,
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable());
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return null;
        }
        HitResult block = p.pick(range, 1.0F, false);
        if (block.getType() == HitResult.Type.BLOCK
                && block.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye)) {
            return null;
        }
        return target;
    }
}
