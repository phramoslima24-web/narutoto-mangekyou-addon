package com.narutotoaddon.mangekyou;

import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** Dano em area, explosoes sem ferir aliados, efeitos e sons usados pelas habilidades dos Susanoos. */
public final class SusanooCombat {
    private SusanooCombat() {}

    /** Cratera nas explosoes do Yasaka Magatama (respeita a regra mobGriefing). */
    public static final boolean DESTROY_BLOCKS = true;
    private static final int MAX_BLOCKS_PER_BLAST = 700;

    private static final Set<String> CHAKRA_WORDS = Set.of("ball", "shoot", "projectile", "dragon", "goryuka", "dummy",
            "arrow", "wave", "beam", "blast", "kunai", "shuriken", "bomb", "flame", "fire", "dan", "bullet", "needle",
            "sphere", "senbon", "rasen", "bijuu", "tailed_beast");

    /** Aliados do dono: ele mesmo, seus Susanoos, clones/pets domados por ele, montaria. */
    public static boolean isAlly(LivingEntity owner, Entity e) {
        if (e == null || owner == null) return true;
        if (e == owner || e.getUUID().equals(owner.getUUID())) return true;
        if (e instanceof CharacterSusanoo cs && owner.getUUID().equals(cs.getOwnerUUID())) return true;
        if (e instanceof TamableAnimal t && owner.getUUID().equals(t.getOwnerUUID())) return true;
        if (e == owner.getVehicle()) return true;
        return e instanceof Player p && (p.isCreative() || p.isSpectator());
    }

    public static DamageSource source(ServerLevel lv, Entity direct, LivingEntity owner) {
        return owner == null ? lv.damageSources().magic() : lv.damageSources().indirectMagic(direct, owner);
    }

    /** Narutoto: entidade de jutsu (nao jogador) cujo nome indica um ataque de chakra. */
    public static boolean isChakraEntity(Entity e) {
        if (e == null || e instanceof Player) return false;
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
        if (id == null || !id.getNamespace().equals("narutoto")) return false;
        String p = id.getPath();
        if (p.endsWith("npc")) return false;
        for (String w : CHAKRA_WORDS) if (p.contains(w)) return true;
        return false;
    }

    /** Dano em esfera com queda por distancia + empurrao. Retorna quantos foram atingidos. */
    public static int blast(ServerLevel lv, Entity src, LivingEntity owner, Vec3 pos, float radius, float damage,
                            float knock, boolean fire) {
        AABB box = new AABB(pos, pos).inflate(radius);
        List<LivingEntity> list = lv.getEntitiesOfClass(LivingEntity.class, box, le -> !isAlly(owner, le) && le.isAlive());
        int n = 0;
        for (LivingEntity le : list) {
            Vec3 c = le.position().add(0, le.getBbHeight() * 0.5, 0);
            double d = c.distanceTo(pos);
            if (d > radius + le.getBbWidth()) continue;
            float f = (float) Math.max(0.35D, 1.0D - 0.65D * (d / radius));
            if (le.hurt(source(lv, src, owner), damage * f)) n++;
            if (fire) le.setSecondsOnFire(6);
            Vec3 push = c.subtract(pos);
            push = push.lengthSqr() < 1.0E-4 ? new Vec3(0, 1, 0) : push.normalize();
            double k = knock * f * (1.0D - le.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 0.8D);
            le.setDeltaMovement(le.getDeltaMovement().add(push.x * k, 0.35D * k + 0.2D, push.z * k));
            le.hurtMarked = true;
        }
        return n;
    }

    /** Cratera esferica (so em blocos quebraveis; limitada para nao travar o servidor). */
    public static void crater(ServerLevel lv, Vec3 pos, float radius) {
        if (!DESTROY_BLOCKS || !lv.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return;
        int r = (int) Math.ceil(radius), count = 0;
        BlockPos c = BlockPos.containing(pos);
        for (int x = -r; x <= r && count < MAX_BLOCKS_PER_BLAST; x++) {
            for (int y = -r; y <= r && count < MAX_BLOCKS_PER_BLAST; y++) {
                for (int z = -r; z <= r && count < MAX_BLOCKS_PER_BLAST; z++) {
                    if (x * x + y * y + z * z > radius * radius) continue;
                    BlockPos bp = c.offset(x, y, z);
                    BlockState st = lv.getBlockState(bp);
                    if (st.isAir()) continue;
                    float h = st.getDestroySpeed(lv, bp);
                    if (h < 0 || h > 30.0F) continue; // bedrock, obsidiana forte etc. resistem
                    lv.destroyBlock(bp, false);
                    count++;
                }
            }
        }
    }

    public static void sound(ServerLevel lv, Vec3 p, SoundEvent s, float vol, float pitch) {
        lv.playSound(null, p.x, p.y, p.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    /** Som do Narutoto se existir (ex.: susanoo_attack_perfect), senao um som do Minecraft. */
    public static void modSound(ServerLevel lv, Vec3 p, String name, SoundEvent fallback, float vol, float pitch) {
        SoundEvent s = ForgeRegistries.SOUND_EVENTS.getValue(new ResourceLocation("narutoto", name));
        sound(lv, p, s != null ? s : fallback, vol, pitch);
    }

    /** Aneis de onda de choque que se expandem (varios ticks). */
    public static void shockwave(ServerLevel lv, Vec3 c, SusanooKind k, float maxR) {
        for (int i = 0; i < 4; i++) {
            final float r = maxR * (i + 1) / 4.0F;
            SusanooTasks.later(i * 2, () -> ring(lv, c, k.dust(2.2F), r, (int) (14 + r * 6)));
        }
    }

    public static void ring(ServerLevel lv, Vec3 c, ParticleOptions p, float r, int n) {
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2 / n;
            lv.sendParticles(p, c.x + Math.cos(a) * r, c.y + 0.3, c.z + Math.sin(a) * r, 1, 0, 0.1, 0, 0.02);
        }
    }

    /** Explosao completa do Yasaka Magatama (azul-escuro) - dano em area, onda de choque, cratera. */
    public static void magatamaExplosion(ServerLevel lv, Entity src, LivingEntity owner, Vec3 pos, SusanooKind k,
                                         float radius, float damage, boolean fire) {
        blast(lv, src, owner, pos, radius, damage, 2.2F, fire);
        crater(lv, pos, radius * 0.45F);
        lv.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        lv.sendParticles(k.dust(3.0F), pos.x, pos.y + 1, pos.z, 140, radius * 0.4, radius * 0.3, radius * 0.4, 0.15);
        lv.sendParticles(k.glowDust(2.0F), pos.x, pos.y + 1, pos.z, 60, radius * 0.3, radius * 0.25, radius * 0.3, 0.2);
        lv.sendParticles(ParticleTypes.END_ROD, pos.x, pos.y + 1, pos.z, 40, radius * 0.3, radius * 0.3, radius * 0.3, 0.25);
        shockwave(lv, pos, k, radius);
        sound(lv, pos, SoundEvents.GENERIC_EXPLODE, 4.0F, 0.6F);
        sound(lv, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, 3.0F, 0.5F);
    }
}
