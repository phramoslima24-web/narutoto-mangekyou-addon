package com.narutotoaddon.mangekyou;

/** Medidas (em blocos) de cada estagio do Susanoo, usadas pelas habilidades e efeitos. */
public final class SusanooGeometry {
    private SusanooGeometry() {}

    /** Altura do peito (de onde saem projeteis). Os clones sempre usam o corpo "completo". */
    public static float chestHeight(int stage, boolean clone) {
        if (clone) stage = 3;
        return switch (stage) { case 1 -> 2.0F; case 2 -> 8.0F; case 3 -> 10.0F; default -> 28.0F; };
    }

    /** Distancia a frente do corpo onde ficam o escudo, a boca da arma e os projeteis. */
    public static float reach(int stage, boolean clone) {
        if (clone) stage = 3;
        return switch (stage) { case 1 -> 1.5F; case 2 -> 5.0F; case 3 -> 7.0F; default -> 10.0F; };
    }

    /** Raio de protecao do Espelho de Yata. */
    public static float mirrorRadius(int stage) {
        return switch (stage) { case 1 -> 3.5F; case 2 -> 6.0F; case 3 -> 8.5F; default -> 17.0F; };
    }

    /** Escala visual do modelo (o do Susanoo perfeito do Sasuke e desenhado a 0,4). */
    public static float modelScale(int stage) {
        return stage == 4 ? 0.4F : 1.0F;
    }

    /** Alcance da broca/lanca e tamanho do modelo dela. */
    public static float lanceScale(int stage) {
        return switch (stage) { case 1 -> 0.10F; case 2 -> 0.35F; case 3 -> 0.45F; default -> 1.0F; };
    }
}
