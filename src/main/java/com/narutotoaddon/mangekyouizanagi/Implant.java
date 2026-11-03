package com.narutotoaddon.mangekyouizanagi;

/** Olhos que podem ser implantados. */
public enum Implant {
    ITACHI("itachi", Ability.NOVO_AMATERASU, Ability.TSUKUYOMI,
            Ability.IZANAGI_LEFT, Ability.IZANAGI_RIGHT, Ability.IZANAMI_LEFT, Ability.IZANAMI_RIGHT),
    SHISUI("shisui", Ability.NOVO_AMATERASU, Ability.KOTOAMATSUKAMI,
            Ability.IZANAGI_LEFT, Ability.IZANAGI_RIGHT, Ability.IZANAMI_LEFT, Ability.IZANAMI_RIGHT),
    MADARA("madara", Ability.REWIND, Ability.TIME_STOP,
            Ability.IZANAGI_LEFT, Ability.IZANAGI_RIGHT, Ability.IZANAMI_LEFT, Ability.IZANAMI_RIGHT);

    public final String id;
    public final Ability[] abilities;

    Implant(String id, Ability... abilities) {
        this.id = id;
        this.abilities = abilities;
    }

    public String nameKey() {
        return "implant." + MangekyouAddon.MODID + "." + id;
    }

    public boolean allows(Ability a) {
        for (Ability x : abilities) if (x == a) return true;
        return false;
    }

    public static Implant byId(String id) {
        if (id == null) return null;
        for (Implant i : values()) if (i.id.equals(id)) return i;
        return null;
    }
}
