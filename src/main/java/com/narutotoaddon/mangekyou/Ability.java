package com.narutotoaddon.mangekyou;

import net.minecraft.network.chat.Component;

/** Jutsus extras. O Susanoo continua sendo o do proprio mod Narutoto. */
public enum Ability {
    NOVO_AMATERASU("novo_amaterasu", 160, 0, Eye.ANY),
    KOTOAMATSUKAMI("kotoamatsukami", 3600, 0, Eye.ANY),
    TSUKUYOMI("tsukuyomi", 800, 0, Eye.ANY),
    REWIND("rewind", 200, 0, Eye.LEFT),
    TIME_STOP("time_stop", 1200, 0, Eye.RIGHT),
    // Izanagi / Izanami: um para cada olho
    IZANAGI_LEFT("izanagi_left", 12000, 0, Eye.LEFT),
    IZANAGI_RIGHT("izanagi_right", 12000, 0, Eye.RIGHT),
    IZANAMI_LEFT("izanami_left", 6000, 0, Eye.LEFT),
    IZANAMI_RIGHT("izanami_right", 6000, 0, Eye.RIGHT),
    // Rinne Sharingan: custam chakra do Narutoto
    MUGEN_TSUKUYOMI("mugen_tsukuyomi", 6000, 2000, Eye.ANY),
    SHINJU_ROOTS("shinju_roots", 600, 500, Eye.ANY),
    AMENOMINAKA("amenominaka", 2400, 1000, Eye.ANY);

    /** Olho exigido (bitmask: 1 = esquerdo, 2 = direito, 0 = qualquer um que ainda enxergue). */
    public static final class Eye {
        private Eye() {}
        public static final int ANY = 0, LEFT = 1, RIGHT = 2;
    }

    /** Prefixo das chaves de jutsu gravadas nos slots do Narutoto (tambem e a chave de traducao). */
    public static final String JUTSU_PREFIX = "jutsu.mkaddon.";

    public final String id;
    public final int cooldownTicks;
    public final int chakraCost;
    public final int eye;

    Ability(String id, int cooldownTicks, int chakraCost, int eye) {
        this.id = id;
        this.cooldownTicks = cooldownTicks;
        this.chakraCost = chakraCost;
        this.eye = eye;
    }

    /** O Narutoto guarda o NOME TRADUZIDO do jutsu (jutsu_key, learned_jutsu, jutsu_choosed_name). */
    public String jutsuName() {
        return Component.translatable(jutsuKey()).getString();
    }

    public String jutsuKey() {
        return JUTSU_PREFIX + id;
    }

    public static Ability byId(String id) {
        for (Ability a : values()) if (a.id.equals(id)) return a;
        return null;
    }

    public static Ability byJutsuKey(String key) {
        if (key == null || key.isEmpty()) return null;
        for (Ability a : values()) {
            if (key.equals(a.jutsuKey()) || key.equals(a.jutsuName())) return a;
        }
        return null;
    }
}
