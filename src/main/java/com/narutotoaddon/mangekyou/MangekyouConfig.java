package com.narutotoaddon.mangekyou;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.ForgeConfigSpec;

/** Config comum (config/narutoto_mangekyou-common.toml): chances de drop dos olhos. */
public final class MangekyouConfig {
    private MangekyouConfig() {}

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue SASUKE_CHANCE, SASUKE_LOOTING, ITACHI_CHANCE, ITACHI_LOOTING;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_DROPS;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Drops dos Mangekyous ao derrotar NPCs do Narutoto (so conta se o autor do golpe for um jogador).").push("drops");
        SASUKE_CHANCE = b.comment("Chance do Mangekyou do Sasuke ao derrotar narutoto:sasuke_npc (0.0 a 1.0)")
                .defineInRange("sasukeChance", 0.25D, 0.0D, 1.0D);
        SASUKE_LOOTING = b.comment("Bonus por nivel de Pilhagem (Sasuke)").defineInRange("sasukeLooting", 0.05D, 0.0D, 1.0D);
        ITACHI_CHANCE = b.comment("Chance do Mangekyou do Itachi ao derrotar narutoto:itachi_npc (1.0 = sempre, como no addon original)")
                .defineInRange("itachiChance", 1.0D, 0.0D, 1.0D);
        ITACHI_LOOTING = b.comment("Bonus por nivel de Pilhagem (Itachi)").defineInRange("itachiLooting", 0.0D, 0.0D, 1.0D);
        EXTRA_DROPS = b.comment(
                "Drops extras no formato \"namespace:entidade|olho|chance|bonusPilhagem\". Olhos: sasuke, madara, itachi, shisui.",
                "O NarutoTo 0.4.1 NAO tem entidade de Madara nem de Shisui, entao a lista vem VAZIA.",
                "Exemplo: \"narutoto:uchiha_npc_3|madara|0.03|0.01\"")
                .defineListAllowEmpty("extraDrops", new ArrayList<String>(), o -> o instanceof String);
        b.pop();
        SPEC = b.build();
    }

    /** Linha de drop extra ja interpretada. */
    public record Extra(String entityId, Implant eye, float chance, float looting) {}

    public static List<Extra> extras() {
        List<Extra> out = new ArrayList<>();
        for (String line : EXTRA_DROPS.get()) {
            Extra e = parseExtra(line);
            if (e != null) out.add(e);
        }
        return out;
    }

    /** "namespace:path|olho|chance[|pilhagem]" -> Extra, ou null se invalido (Rinne nao pode ser drop extra). */
    public static Extra parseExtra(String line) {
        if (line == null) return null;
        String[] f = line.split("\\|");
        if (f.length < 3) return null;
        Implant eye = Implant.byId(f[1].trim());
        if (eye == null || eye == Implant.RINNE) return null;
        try {
            float chance = Math.max(0F, Math.min(1F, Float.parseFloat(f[2].trim())));
            float loot = f.length > 3 ? Math.max(0F, Float.parseFloat(f[3].trim())) : 0F;
            return new Extra(f[0].trim(), eye, chance, loot);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
