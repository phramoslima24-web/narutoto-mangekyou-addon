package com.narutotoaddon.mangekyou;

/** Olhos que podem ser implantados. */
public enum Implant {
    ITACHI("itachi", Ability.NOVO_AMATERASU, Ability.TSUKUYOMI, Ability.YATA_MIRROR, Ability.YASAKA_JEWEL,
            Ability.IZANAGI_LEFT, Ability.IZANAGI_RIGHT, Ability.IZANAMI_LEFT, Ability.IZANAMI_RIGHT),
    SHISUI("shisui", Ability.NOVO_AMATERASU, Ability.KOTOAMATSUKAMI, Ability.TSUKUMO, Ability.DRILL_LANCE,
            Ability.IZANAGI_LEFT, Ability.IZANAGI_RIGHT, Ability.IZANAMI_LEFT, Ability.IZANAMI_RIGHT),
    MADARA("madara", Ability.REWIND, Ability.TIME_STOP, Ability.YASAKA_MAGATAMA, Ability.SUSANOO_CLONES,
            Ability.IZANAGI_LEFT, Ability.IZANAGI_RIGHT, Ability.IZANAMI_LEFT, Ability.IZANAMI_RIGHT),
    // Sasuke: Susanoo original do Narutoto (intacto) + Amaterasu. Sem Izanagi/Izanami, entao nao cega.
    SASUKE("sasuke", Ability.NOVO_AMATERASU),
    RINNE("rinne", Ability.MUGEN_TSUKUYOMI, Ability.SHINJU_ROOTS, Ability.AMENOMINAKA);

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

    /** Tem Izanagi/Izanami (pode perder a visao de um olho). */
    public boolean canBlind() {
        return allows(Ability.IZANAGI_LEFT);
    }

    public static Implant byId(String id) {
        if (id == null) return null;
        for (Implant i : values()) if (i.id.equals(id)) return i;
        return null;
    }
}
