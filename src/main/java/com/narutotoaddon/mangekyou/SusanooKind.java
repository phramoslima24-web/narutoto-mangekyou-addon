package com.narutotoaddon.mangekyou;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3f;

/**
 * Qual Susanoo pertence a qual Mangekyou. A identificacao e feita SEMPRE pelo implante real do jogador
 * ({@link Implants#get}, no cliente {@link ClientState}) - nunca por nome de jogador ou texto.
 * O Sasuke nao esta aqui de proposito: o Susanoo original do mod continua sem nenhuma alteracao.
 */
public enum SusanooKind {
    MADARA("madara", Implant.MADARA, 0.12F, 0.25F, 0.70F, 0.30F, 0.50F, 1.00F),
    ITACHI("itachi", Implant.ITACHI, 0.95F, 0.74F, 0.16F, 1.00F, 0.86F, 0.32F),
    SHISUI("shisui", Implant.SHISUI, 0.16F, 0.78F, 0.38F, 0.38F, 1.00F, 0.58F);

    public static final String[] STAGE_NAMES = {"rib", "skeleton", "complete", "perfect"};

    public final String id;
    public final Implant implant;
    public final Vector3f main;
    public final Vector3f bright;

    SusanooKind(String id, Implant implant, float r, float g, float b, float br, float bg, float bb) {
        this.id = id;
        this.implant = implant;
        this.main = new Vector3f(r, g, b);
        this.bright = new Vector3f(br, bg, bb);
    }

    public static SusanooKind of(Implant i) {
        if (i == null) return null;
        for (SusanooKind k : values()) if (k.implant == i) return k;
        return null;
    }

    /** Mangekyou equipado (implante) da entidade; null para Sasuke / sem implante / Rinne. */
    public static SusanooKind of(Entity e) {
        if (e == null) return null;
        Implant i = e.level().isClientSide ? ClientState.get(e.getUUID()) : Implants.get(e);
        return of(i);
    }

    public static SusanooKind byOrdinal(int o) {
        SusanooKind[] v = values();
        return o >= 0 && o < v.length ? v[o] : MADARA;
    }

    public static String stageName(int stage) {
        return STAGE_NAMES[Math.max(1, Math.min(4, stage)) - 1];
    }

    public ResourceLocation texture(int stage) {
        return new ResourceLocation(MangekyouAddon.MODID, "textures/entity/" + id + "_susanoo_" + stageName(stage) + ".png");
    }

    public ResourceLocation geo(int stage) {
        return new ResourceLocation(MangekyouAddon.MODID, "geo/" + id + "_susanoo_" + stageName(stage) + ".geo.json");
    }

    public ResourceLocation animation(int stage) {
        return new ResourceLocation(MangekyouAddon.MODID,
                "animations/" + id + "_susanoo_" + stageName(stage) + ".animation.json");
    }

    public DustParticleOptions dust(float size) {
        return new DustParticleOptions(new Vector3f(main), size);
    }

    public DustParticleOptions glowDust(float size) {
        return new DustParticleOptions(new Vector3f(bright), size);
    }
}
