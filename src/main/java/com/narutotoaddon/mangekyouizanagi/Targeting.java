package com.narutotoaddon.mangekyouizanagi;

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
            return assist(p, range, eye, look);
        }
        HitResult block = p.pick(range, 1.0F, false);
        if (block.getType() == HitResult.Type.BLOCK
                && block.getLocation().distanceToSqr(eye) < hit.getLocation().distanceToSqr(eye)) {
            return null;
        }
        return target;
    }

    /** Auxilio de mira: se nao acertou ninguem direto, pega o alvo mais proximo da linha de visao (cone estreito). */
    private static LivingEntity assist(Player p, double range, Vec3 eye, Vec3 look) {
        AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(2.0D);
        LivingEntity best = null;
        double bestScore = 0.97D;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, box,
                x -> x != p && x.isAlive() && !x.isSpectator() && x.isPickable())) {
            Vec3 to = e.getEyePosition().subtract(eye);
            double dist = to.length();
            if (dist < 0.5D || dist > range) continue;
            double dot = to.normalize().dot(look);
            if (dot > bestScore && p.hasLineOfSight(e)) {
                bestScore = dot;
                best = e;
            }
        }
        return best;
    }
}
