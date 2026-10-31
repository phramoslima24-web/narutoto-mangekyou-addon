package com.narutotoaddon.mangekyou;

public enum Ability {
    SUSANOO("susanoo", 1200),
    AMATERASU("amaterasu", 160),
    KOTOAMATSUKAMI("kotoamatsukami", 3600),
    TSUKUYOMI("tsukuyomi", 800),
    REWIND("rewind", 200),
    TIME_STOP("time_stop", 1200);

    public final String id;
    /** Recarga em ticks (20 ticks = 1 segundo). */
    public final int cooldownTicks;

    Ability(String id, int cooldownTicks) {
        this.id = id;
        this.cooldownTicks = cooldownTicks;
    }

    public String langKey() {
        return "ability." + MangekyouAddon.MODID + "." + id;
    }
}
