package com.narutotoaddon.mangekyouizanagi;

/** Jutsus extras. O Susanoo continua sendo o do proprio mod Narutoto. */
public enum Ability {
    NOVO_AMATERASU("novo_amaterasu", 160, Eye.ANY),
    KOTOAMATSUKAMI("kotoamatsukami", 3600, Eye.ANY),
    TSUKUYOMI("tsukuyomi", 800, Eye.ANY),
    REWIND("rewind", 200, Eye.LEFT),
    TIME_STOP("time_stop", 1200, Eye.RIGHT),
    IZANAGI_LEFT("izanagi_left", 12000, Eye.LEFT),
    IZANAGI_RIGHT("izanagi_right", 12000, Eye.RIGHT),
    IZANAMI_LEFT("izanami_left", 6000, Eye.LEFT),
    IZANAMI_RIGHT("izanami_right", 6000, Eye.RIGHT);

    /** Qual olho o jutsu exige (bitmask: 1 = esquerdo, 2 = direito, 0 = qualquer um que ainda enxergue). */
    public static final class Eye {
        private Eye() {}
        public static final int ANY = 0, LEFT = 1, RIGHT = 2;
    }

    /** Prefixo das chaves de jutsu gravadas nos slots do Narutoto (tambem e a chave de traducao). */
    public static final String JUTSU_PREFIX = "jutsu.mkiz.";

    public final String id;
    /** Recarga em ticks (20 ticks = 1 segundo). */
    public final int cooldownTicks;
    public final int eye;

    Ability(String id, int cooldownTicks, int eye) {
        this.id = id;
        this.cooldownTicks = cooldownTicks;
        this.eye = eye;
    }

    /** Izanagi e Izanami selam o olho usado (cegam). */
    public boolean costsEye() {
        return this == IZANAGI_LEFT || this == IZANAGI_RIGHT || this == IZANAMI_LEFT || this == IZANAMI_RIGHT;
    }

    public String jutsuKey() {
        return JUTSU_PREFIX + id;
    }

    public static Ability byId(String id) {
        for (Ability a : values()) if (a.id.equals(id)) return a;
        return null;
    }

    public static Ability byJutsuKey(String key) {
        if (key == null || !key.startsWith(JUTSU_PREFIX)) return null;
        return byId(key.substring(JUTSU_PREFIX.length()));
    }
}
