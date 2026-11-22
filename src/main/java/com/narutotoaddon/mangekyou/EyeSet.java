package com.narutotoaddon.mangekyou;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Logica pura (sem Minecraft) dos olhos implantados: serializacao, sorteio e jutsus acumulados.
 * Fica separada para poder ser testada fora do jogo.
 */
public final class EyeSet {
    private EyeSet() {}

    /** Quem entra no sorteio do primeiro despertar. O Obito e o Rinne NAO participam. */
    public static final List<Implant> LOTTERY = List.of(Implant.SASUKE, Implant.MADARA, Implant.ITACHI, Implant.SHISUI);

    /** Le a lista salva ("itachi,sasuke"), sem repetidos e ignorando ids desconhecidos. Mantem a ordem de implante. */
    public static List<Implant> parse(String csv) {
        Set<Implant> out = new LinkedHashSet<>();
        if (csv != null) {
            for (String s : csv.split(",")) {
                Implant i = Implant.byId(s.trim());
                if (i != null) out.add(i);
            }
        }
        return new ArrayList<>(out);
    }

    public static String write(List<Implant> eyes) {
        StringBuilder sb = new StringBuilder();
        for (Implant i : new LinkedHashSet<>(eyes)) {
            if (sb.length() > 0) sb.append(',');
            sb.append(i.id);
        }
        return sb.toString();
    }

    /** Adiciona sem nunca remover nada. Retorna false se o olho ja estava na lista. */
    public static boolean add(List<Implant> eyes, Implant eye) {
        if (eyes.contains(eye)) return false;
        eyes.add(eye);
        return true;
    }

    /** Sorteio uniforme entre Sasuke, Madara, Itachi e Shisui. */
    public static Implant draw(Random rnd) {
        return LOTTERY.get(rnd.nextInt(LOTTERY.size()));
    }

    /** Uniao dos jutsus de todos os olhos, sem duplicados e na ordem em que os olhos foram obtidos. */
    public static List<Ability> abilities(List<Implant> eyes) {
        Set<Ability> out = new LinkedHashSet<>();
        for (Implant i : eyes) {
            for (Ability a : i.abilities) out.add(a);
        }
        return new ArrayList<>(out);
    }

    public static boolean allows(List<Implant> eyes, Ability a) {
        for (Implant i : eyes) if (i.allows(a)) return true;
        return false;
    }
}
