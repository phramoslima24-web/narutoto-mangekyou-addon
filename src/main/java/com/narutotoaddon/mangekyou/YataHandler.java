package com.narutotoaddon.mangekyou;

import java.util.UUID;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Espelho de Yata - reconhece o tipo de ataque e adapta a defesa:
 * projetil -> refletido; ataque de chakra (entidade de jutsu) -> absorvido; golpe corpo a corpo -> bloqueado,
 * atacante empurrado e parte do dano devolvida; dano espiritual/elemental (magia, explosao, fogo, raio) -> anulado.
 * Dano nao direcional (queda, afogamento, vazio, /kill) nao e bloqueado.
 */
public final class YataHandler {
    private YataHandler() {}

    private static boolean reflecting;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAttack(LivingAttackEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || reflecting) return;
        UUID oid = victim instanceof Player ? victim.getUUID()
                : victim instanceof CharacterSusanoo cs ? cs.getOwnerUUID() : null;
        if (oid == null) return;
        YataMirrorEntity m = YataMirrorEntity.ACTIVE.get(oid);
        if (m == null || m.isRemoved()) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || src.is(DamageTypes.FALL) || src.is(DamageTypes.DROWN)
                || src.is(DamageTypes.STARVE) || src.is(DamageTypes.IN_WALL) || src.is(DamageTypes.CRAMMING)
                || src.is(DamageTypes.FLY_INTO_WALL) || src.is(DamageTypes.FREEZE)) {
            return;
        }
        Entity direct = src.getDirectEntity();
        Entity attacker = src.getEntity();
        if (attacker != null && oid.equals(attacker.getUUID())) return;
        if (direct instanceof SusanooProjectile sp && oid.equals(sp.getOwner() == null ? null : sp.getOwner().getUUID())) return;

        boolean projectile = direct instanceof Projectile || src.is(DamageTypeTags.IS_PROJECTILE);
        boolean chakra = SusanooCombat.isChakraEntity(direct) || SusanooCombat.isChakraEntity(attacker);
        boolean elemental = src.is(DamageTypeTags.IS_EXPLOSION) || src.is(DamageTypeTags.IS_FIRE)
                || src.is(DamageTypeTags.IS_LIGHTNING) || src.is(DamageTypes.MAGIC) || src.is(DamageTypes.INDIRECT_MAGIC)
                || src.is(DamageTypes.WITHER) || src.is(DamageTypes.DRAGON_BREATH) || src.is(DamageTypes.SONIC_BOOM);
        boolean melee = direct instanceof LivingEntity && !chakra;
        if (!projectile && !chakra && !elemental && !melee) return;

        Vec3 from = direct != null ? direct.position() : attacker != null ? attacker.position() : src.getSourcePosition();
        if (from != null && !m.inFront(from)) return; // so protege a frente do Susanoo
        if (from == null && !elemental) return;

        e.setCanceled(true);
        m.absorb(e.getAmount(), from != null ? from.add(0, 1, 0) : m.position());

        if (projectile && direct instanceof Projectile pr && !(direct instanceof SusanooProjectile)) {
            Player ow = m.level().getPlayerByUUID(oid);
            pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.0D));
            pr.hurtMarked = true;
            if (ow != null) pr.setOwner(ow);
            pr.getPersistentData().putLong("mkaddon_yata", m.level().getGameTime());
        } else if (chakra && direct != null && !(direct instanceof LivingEntity le && le.isAlive() && direct instanceof Player)) {
            direct.discard();
        } else if (melee && attacker instanceof LivingEntity att) {
            Player ow = m.level().getPlayerByUUID(oid);
            Vec3 away = att.position().subtract(m.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : away.normalize();
            att.setDeltaMovement(away.x * 1.6D, 0.45D, away.z * 1.6D);
            att.hurtMarked = true;
            if (ow != null) {
                reflecting = true;
                try {
                    att.hurt(att.level().damageSources().indirectMagic(ow, ow), Math.min(20.0F, e.getAmount() * 0.25F));
                } finally {
                    reflecting = false;
                }
            }
        }
    }
}
