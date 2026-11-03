package com.narutotoaddon.mangekyou;

/** Jutsus extras. O Susanoo continua sendo o do proprio mod Narutoto. */
public enum Ability {
    NOVO_AMATERASU("novo_amaterasu", 160),
    KOTOAMATSUKAMI("kotoamatsukami", 3600),
    TSUKUYOMI("tsukuyomi", 800),
    REWIND("rewind", 200),
    TIME_STOP("time_stop", 1200),
    IZANAGI("izanagi", 400),
    IZANAMI("izanami", 600);

    /** Prefixo das chaves de jutsu gravadas nos slots do Narutoto (tambem e a chave de traducao). */
    public static final String JUTSU_PREFIX = "jutsu.mkaddon.";

    public final String id;
    /** Recarga em ticks (20 ticks = 1 segundo). */
    public final int cooldownTicks;

    Ability(String id, int cooldownTicks) {
        this.id = id;
        this.cooldownTicks = cooldownTicks;
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
