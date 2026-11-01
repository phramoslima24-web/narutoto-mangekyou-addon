package com.narutotoaddon.mangekyou;

/** Olhos que podem ser implantados. */
public enum Implant {
    ITACHI("itachi", Ability.NOVO_AMATERASU, Ability.TSUKUYOMI),
    SHISUI("shisui", Ability.NOVO_AMATERASU, Ability.KOTOAMATSUKAMI),
    MADARA("madara", Ability.REWIND, Ability.TIME_STOP);

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
